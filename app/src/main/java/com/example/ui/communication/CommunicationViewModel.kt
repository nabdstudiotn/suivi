package com.example.ui.communication

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.CommunicationRepository
import com.example.domain.model.AnnouncementAudience
import com.example.domain.model.AnnouncementPriority
import com.example.domain.model.AnnouncementReadStats
import com.example.domain.model.AnnouncementStatus
import com.example.domain.model.AnnouncementType
import com.example.domain.model.ChatConversation
import com.example.domain.model.ChatMessage
import com.example.domain.model.CompanyMember
import com.example.domain.model.ConversationType
import com.example.domain.model.MemberRole
import com.example.domain.model.WorkshopAnnouncement
import com.example.domain.model.buildGroupConversationId
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

data class CommunicationUiState(
    val selectedTab: Int = 0, // 0: Discussions (Group), 1: Messages privés (Direct)
    val companyId: String = "atelier_nabd_01",
    val currentMember: CompanyMember? = null,
    val isLoading: Boolean = false,
    val groupConversationId: String = buildGroupConversationId("atelier_nabd_01"),
    val groupMessages: List<ChatMessage> = emptyList(),
    val companyMembers: List<CompanyMember> = emptyList(),
    val directConversations: List<ChatConversation> = emptyList(),
    val activeDirectConversation: ChatConversation? = null,
    val directMessages: List<ChatMessage> = emptyList(),
    val messageInput: String = "",
    val isSending: Boolean = false,
    val errorMessage: String? = null,
    // Announcements & Read Tracking state
    val announcements: List<WorkshopAnnouncement> = emptyList(),
    val unreadAnnouncementsCount: Int = 0,
    val isAnnouncementSheetOpen: Boolean = false,
    val isCreateAnnouncementDialogOpen: Boolean = false,
    val viewingAnnouncementDetails: WorkshopAnnouncement? = null,
    val viewingAnnouncementReadStats: AnnouncementReadStats? = null,
    val newAnnouncementType: AnnouncementType = AnnouncementType.INFORMATION,
    val newAnnouncementTitle: String = "",
    val newAnnouncementContent: String = "",
    val newAnnouncementPriority: AnnouncementPriority = AnnouncementPriority.NORMALE,
    val newAnnouncementAudience: AnnouncementAudience = AnnouncementAudience.TOUS,
    val newAnnouncementDurationDays: Int = 7,
    val isPublishingAnnouncement: Boolean = false,
    val announcementFeedbackMessage: String? = null
)

class CommunicationViewModel(
    private val repository: CommunicationRepository,
    private val companyId: String = "atelier_nabd_01"
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        CommunicationUiState(
            companyId = companyId,
            groupConversationId = buildGroupConversationId(companyId)
        )
    )
    val uiState: StateFlow<CommunicationUiState> = _uiState.asStateFlow()

    private var readStatsJob: Job? = null

    init {
        // Ensure and observe current member
        viewModelScope.launch {
            try {
                repository.ensureCurrentMember(companyId)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = e.localizedMessage)
            }
        }

        viewModelScope.launch {
            repository.observeCurrentMember(companyId).collectLatest { member ->
                _uiState.value = _uiState.value.copy(currentMember = member)
                updateUnreadCount()
            }
        }

        // Observe group messages
        val groupId = buildGroupConversationId(companyId)
        repository.startRealtimeMessagesListener(companyId, groupId)
        viewModelScope.launch {
            repository.observeMessages(groupId).collectLatest { msgs ->
                _uiState.value = _uiState.value.copy(groupMessages = msgs)
            }
        }

        // Observe other company members
        viewModelScope.launch {
            repository.observeCompanyMembers(companyId).collectLatest { membersList ->
                val currentUid = _uiState.value.currentMember?.userId
                val others = membersList.filter { it.userId != currentUid }
                _uiState.value = _uiState.value.copy(companyMembers = others)
            }
        }

        // Observe conversations
        viewModelScope.launch {
            repository.observeConversations(companyId).collectLatest { convs ->
                val directOnly = convs.filter { it.type == ConversationType.DIRECT }
                _uiState.value = _uiState.value.copy(directConversations = directOnly)
            }
        }

        // Observe announcements
        repository.startAnnouncementsRealtimeListener(companyId)
        viewModelScope.launch {
            repository.observeAnnouncements(companyId).collectLatest { list ->
                _uiState.value = _uiState.value.copy(announcements = list)
                updateUnreadCount()
            }
        }

        // Flush outbox on start
        viewModelScope.launch {
            repository.flushOutbox()
        }
    }

    private fun updateUnreadCount() {
        val member = _uiState.value.currentMember
        val unread = _uiState.value.announcements.count { a ->
            a.isCurrentlyActive && (member == null || a.isAudienceEligible(member)) && !a.isReadLocally
        }
        _uiState.value = _uiState.value.copy(unreadAnnouncementsCount = unread)
    }

    fun selectTab(tab: Int) {
        _uiState.value = _uiState.value.copy(selectedTab = tab, messageInput = "", errorMessage = null)
    }

    fun setMessageInput(text: String) {
        _uiState.value = _uiState.value.copy(messageInput = text, errorMessage = null)
    }

    fun sendGroupMessage() {
        val text = _uiState.value.messageInput.trim()
        if (text.isBlank()) return

        val state = _uiState.value
        val member = state.currentMember
        if (member == null || (!member.canPostGroup && member.role != MemberRole.DIRECTRICE)) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "Publication réservée à la direction de l'atelier."
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSending = true, errorMessage = null)
            val result = repository.sendMessage(
                companyId = companyId,
                conversationId = state.groupConversationId,
                text = text,
                type = ConversationType.GROUP
            )
            _uiState.value = _uiState.value.copy(
                isSending = false,
                messageInput = if (result.isSuccess) "" else text,
                errorMessage = result.exceptionOrNull()?.localizedMessage
            )
        }
    }

    fun openDirectChat(otherMember: CompanyMember) {
        viewModelScope.launch {
            try {
                val conv = repository.createOrGetDirectConversation(companyId, otherMember)
                _uiState.value = _uiState.value.copy(
                    activeDirectConversation = conv,
                    messageInput = "",
                    errorMessage = null
                )
                // Start listening to messages for this direct conversation
                repository.startRealtimeMessagesListener(companyId, conv.id)
                repository.observeMessages(conv.id).collectLatest { msgs ->
                    _uiState.value = _uiState.value.copy(directMessages = msgs)
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = e.localizedMessage)
            }
        }
    }

    fun closeDirectChat() {
        _uiState.value = _uiState.value.copy(activeDirectConversation = null, directMessages = emptyList(), messageInput = "")
    }

    fun sendDirectMessage() {
        val text = _uiState.value.messageInput.trim()
        val conv = _uiState.value.activeDirectConversation ?: return
        if (text.isBlank()) return

        val recipientId = conv.otherParticipantId ?: conv.participantUids.firstOrNull { it != _uiState.value.currentMember?.userId }
        if (recipientId == null) {
            _uiState.value = _uiState.value.copy(errorMessage = "Destinataire non identifié.")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSending = true, errorMessage = null)
            val result = repository.sendMessage(
                companyId = companyId,
                conversationId = conv.id,
                text = text,
                type = ConversationType.DIRECT,
                recipientId = recipientId
            )
            _uiState.value = _uiState.value.copy(
                isSending = false,
                messageInput = if (result.isSuccess) "" else text,
                errorMessage = result.exceptionOrNull()?.localizedMessage
            )
        }
    }

    fun retryMessage(messageId: String) {
        viewModelScope.launch {
            val res = repository.retryMessage(messageId)
            if (res.isFailure) {
                _uiState.value = _uiState.value.copy(errorMessage = res.exceptionOrNull()?.localizedMessage)
            }
        }
    }

    // --- Announcements UI actions ---

    fun openAnnouncementsSheet() {
        _uiState.value = _uiState.value.copy(isAnnouncementSheetOpen = true, announcementFeedbackMessage = null)
    }

    fun closeAnnouncementsSheet() {
        _uiState.value = _uiState.value.copy(
            isAnnouncementSheetOpen = false,
            viewingAnnouncementDetails = null,
            viewingAnnouncementReadStats = null
        )
        readStatsJob?.cancel()
        readStatsJob = null
    }

    fun openCreateAnnouncementDialog() {
        val member = _uiState.value.currentMember
        if (member?.role != MemberRole.DIRECTRICE && member?.canPostGroup != true) {
            _uiState.value = _uiState.value.copy(errorMessage = "Création d'annonce réservée à la direction de l'atelier.")
            return
        }
        _uiState.value = _uiState.value.copy(
            isCreateAnnouncementDialogOpen = true,
            newAnnouncementTitle = "",
            newAnnouncementContent = "",
            newAnnouncementType = AnnouncementType.INFORMATION,
            newAnnouncementPriority = AnnouncementPriority.NORMALE,
            newAnnouncementAudience = AnnouncementAudience.TOUS,
            newAnnouncementDurationDays = 7,
            announcementFeedbackMessage = null
        )
    }

    fun closeCreateAnnouncementDialog() {
        _uiState.value = _uiState.value.copy(isCreateAnnouncementDialogOpen = false)
    }

    fun setNewAnnouncementType(type: AnnouncementType) {
        _uiState.value = _uiState.value.copy(newAnnouncementType = type)
    }

    fun setNewAnnouncementTitle(title: String) {
        _uiState.value = _uiState.value.copy(newAnnouncementTitle = title)
    }

    fun setNewAnnouncementContent(content: String) {
        _uiState.value = _uiState.value.copy(newAnnouncementContent = content)
    }

    fun setNewAnnouncementPriority(priority: AnnouncementPriority) {
        _uiState.value = _uiState.value.copy(newAnnouncementPriority = priority)
    }

    fun setNewAnnouncementAudience(audience: AnnouncementAudience) {
        _uiState.value = _uiState.value.copy(newAnnouncementAudience = audience)
    }

    fun setNewAnnouncementDurationDays(days: Int) {
        _uiState.value = _uiState.value.copy(newAnnouncementDurationDays = days.coerceAtLeast(1))
    }

    fun publishAnnouncement() {
        val state = _uiState.value
        val title = state.newAnnouncementTitle.trim()
        val content = state.newAnnouncementContent.trim()
        if (title.isBlank()) {
            _uiState.value = state.copy(errorMessage = "Veuillez saisir un titre pour l'annonce.")
            return
        }
        if (content.isBlank()) {
            _uiState.value = state.copy(errorMessage = "Veuillez saisir le contenu de l'annonce.")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isPublishingAnnouncement = true, errorMessage = null)
            val result = repository.createAnnouncement(
                companyId = companyId,
                type = state.newAnnouncementType,
                title = title,
                content = content,
                priority = state.newAnnouncementPriority,
                targetAudience = state.newAnnouncementAudience,
                durationDays = state.newAnnouncementDurationDays
            )
            if (result.isSuccess) {
                _uiState.value = _uiState.value.copy(
                    isPublishingAnnouncement = false,
                    isCreateAnnouncementDialogOpen = false,
                    announcementFeedbackMessage = "Annonce publiée avec succès."
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isPublishingAnnouncement = false,
                    errorMessage = result.exceptionOrNull()?.localizedMessage
                )
            }
        }
    }

    fun cancelAnnouncement(announcementId: String) {
        viewModelScope.launch {
            val res = repository.cancelAnnouncement(companyId, announcementId)
            if (res.isSuccess) {
                _uiState.value = _uiState.value.copy(
                    announcementFeedbackMessage = "Annonce annulée avec succès.",
                    viewingAnnouncementDetails = null,
                    viewingAnnouncementReadStats = null
                )
            } else {
                _uiState.value = _uiState.value.copy(errorMessage = res.exceptionOrNull()?.localizedMessage)
            }
        }
    }

    /**
     * Opens announcement details and records read receipt ONLY at this moment.
     */
    fun openAnnouncementDetails(announcement: WorkshopAnnouncement) {
        _uiState.value = _uiState.value.copy(
            viewingAnnouncementDetails = announcement,
            announcementFeedbackMessage = null
        )

        // Record read receipt
        viewModelScope.launch {
            repository.recordAnnouncementRead(companyId, announcement)
            updateUnreadCount()
        }

        // If manager/author, observe read statistics
        readStatsJob?.cancel()
        val member = _uiState.value.currentMember
        val isManagerOrAuthor = member?.role == MemberRole.DIRECTRICE || member?.userId == announcement.authorId
        if (isManagerOrAuthor) {
            readStatsJob = viewModelScope.launch {
                repository.getReadStatsForAnnouncement(companyId, announcement).collectLatest { stats ->
                    _uiState.value = _uiState.value.copy(viewingAnnouncementReadStats = stats)
                }
            }
        }
    }

    fun closeAnnouncementDetails() {
        readStatsJob?.cancel()
        readStatsJob = null
        _uiState.value = _uiState.value.copy(
            viewingAnnouncementDetails = null,
            viewingAnnouncementReadStats = null
        )
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    fun clearAnnouncementFeedback() {
        _uiState.value = _uiState.value.copy(announcementFeedbackMessage = null)
    }

    class Factory(
        private val repository: CommunicationRepository,
        private val companyId: String = "atelier_nabd_01"
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return CommunicationViewModel(repository, companyId) as T
        }
    }
}
