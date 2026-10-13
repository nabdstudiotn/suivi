package com.example.ui.archives

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.ArchiveRepository
import com.example.data.AttendanceRepository
import com.example.data.SettingsRepository
import com.example.domain.model.ArchiveMonthStatus
import com.example.domain.model.AuditEvent
import com.example.domain.model.ChafaPermissions
import com.example.domain.model.MemberRole
import com.example.domain.model.MonthlyWorkerArchiveItem
import com.example.domain.model.UserAccountProfile
import com.example.domain.model.Worker
import com.example.domain.model.WorkshopMonthlyArchiveSummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

enum class ArchiveTab(val labelFr: String) {
    ATELIER_GENERAL("Archive d'Atelier (Mensuelle)"),
    CHRONOLOGIE_OUVRIERE("Archive par Ouvrière (Chronologique)")
}

data class ArchivesUiState(
    val selectedTab: ArchiveTab = ArchiveTab.ATELIER_GENERAL,
    val selectedYearMonth: String = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM")),
    val availableMonths: List<String> = emptyList(),
    val selectedWorker: Worker? = null,
    val workshopMonthlyArchive: WorkshopMonthlyArchiveSummary? = null,
    val workerChronologicalMonths: List<MonthlyWorkerArchiveItem> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val feedbackMessage: String? = null,
    val isErrorFeedback: Boolean = false,
    val showCloseMonthDialog: Boolean = false,
    val showRevisionDialog: Boolean = false,
    val showAuditHistoryDialog: Boolean = false,
    val showPermissionWarningDialog: Boolean = false,
    val permissionWarningText: String? = null,
    val auditTrail: List<AuditEvent> = emptyList(),
    val currentUserProfile: UserAccountProfile? = null,
    val chafaPermissions: ChafaPermissions? = null,
    val expandedDailyWorkerId: String? = null,
    val expandedMonthInChronology: String? = null
) {
    val canUserCloseOrRevise: Boolean
        get() {
            val role = currentUserProfile?.role ?: MemberRole.DIRECTRICE
            if (role == MemberRole.DIRECTRICE) return true
            return chafaPermissions?.canCloseArchive == true
        }

    val filteredWorkshopWorkers: List<MonthlyWorkerArchiveItem>
        get() {
            val list = workshopMonthlyArchive?.workers ?: emptyList()
            if (searchQuery.isBlank()) return list
            val q = searchQuery.trim().lowercase()
            return list.filter {
                it.workerFullName.lowercase().contains(q) ||
                it.workerRole.lowercase().contains(q) ||
                it.workerCategory.lowercase().contains(q)
            }
        }

    val filteredWorkerChronologicalMonths: List<MonthlyWorkerArchiveItem>
        get() {
            if (searchQuery.isBlank()) return workerChronologicalMonths
            val q = searchQuery.trim().lowercase()
            return workerChronologicalMonths.filter {
                it.yearMonth.contains(q) ||
                it.dailyRecords.any { d -> d.dateIso.contains(q) || d.dayNameFrench.lowercase().contains(q) }
            }
        }
}

class ArchivesViewModel(
    private val archiveRepository: ArchiveRepository,
    private val settingsRepository: SettingsRepository,
    private val attendanceRepository: AttendanceRepository,
    private val companyId: String = "atelier_nabd_01"
) : ViewModel() {

    private val _uiState = MutableStateFlow(ArchivesUiState())
    val uiState: StateFlow<ArchivesUiState> = _uiState.asStateFlow()

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        // Observe current user profile
        viewModelScope.launch {
            settingsRepository.getUserProfile(companyId).collectLatest { profile ->
                _uiState.update { it.copy(currentUserProfile = profile) }
            }
        }

        // Observe central permissions from Paramètres
        viewModelScope.launch {
            settingsRepository.getChafaPermissions(companyId).collectLatest { perms ->
                _uiState.update { it.copy(chafaPermissions = perms) }
            }
        }

        // Observe available months
        viewModelScope.launch {
            archiveRepository.getAllAvailableMonths().collectLatest { months ->
                _uiState.update { current ->
                    val defaultMonth = if (months.contains(current.selectedYearMonth)) {
                        current.selectedYearMonth
                    } else {
                        months.firstOrNull() ?: current.selectedYearMonth
                    }
                    current.copy(
                        availableMonths = months,
                        selectedYearMonth = defaultMonth
                    )
                }
                observeMonthlyArchive(_uiState.value.selectedYearMonth)
            }
        }
    }

    fun selectMonth(yearMonth: String) {
        _uiState.update { it.copy(selectedYearMonth = yearMonth, expandedDailyWorkerId = null) }
        observeMonthlyArchive(yearMonth)
    }

    fun previousMonth() {
        val months = _uiState.value.availableMonths
        val currentIdx = months.indexOf(_uiState.value.selectedYearMonth)
        if (currentIdx != -1 && currentIdx < months.size - 1) {
            selectMonth(months[currentIdx + 1])
        }
    }

    fun nextMonth() {
        val months = _uiState.value.availableMonths
        val currentIdx = months.indexOf(_uiState.value.selectedYearMonth)
        if (currentIdx > 0) {
            selectMonth(months[currentIdx - 1])
        }
    }

    private fun observeMonthlyArchive(yearMonth: String) {
        viewModelScope.launch {
            archiveRepository.getMonthlyWorkshopArchive(companyId, yearMonth).collectLatest { summary ->
                _uiState.update { it.copy(workshopMonthlyArchive = summary) }
            }
        }
    }

    fun selectWorkerForChronology(worker: Worker) {
        _uiState.update {
            it.copy(
                selectedTab = ArchiveTab.CHRONOLOGIE_OUVRIERE,
                selectedWorker = worker,
                searchQuery = "",
                expandedMonthInChronology = null
            )
        }
        observeWorkerChronology(worker.id)
    }

    fun selectWorkerFromArchiveItem(item: MonthlyWorkerArchiveItem) {
        viewModelScope.launch {
            val allWorkers = attendanceRepository.getAllWorkers().first()
            val worker = allWorkers.firstOrNull { it.id == item.workerId } ?: Worker(
                id = item.workerId,
                firstName = item.workerFullName.split(" ").firstOrNull() ?: item.workerFullName,
                lastName = item.workerFullName.split(" ").drop(1).joinToString(" "),
                role = item.workerRole,
                currentRateMillimes = item.appliedRateMillimes,
                isActive = item.isActive
            )
            selectWorkerForChronology(worker)
        }
    }

    private fun observeWorkerChronology(workerId: String) {
        viewModelScope.launch {
            archiveRepository.getWorkerChronologicalArchive(workerId).collectLatest { list ->
                _uiState.update { it.copy(workerChronologicalMonths = list) }
            }
        }
    }

    fun backToWorkshopOverview() {
        _uiState.update {
            it.copy(
                selectedTab = ArchiveTab.ATELIER_GENERAL,
                selectedWorker = null,
                searchQuery = ""
            )
        }
    }

    fun selectTab(tab: ArchiveTab) {
        _uiState.update { it.copy(selectedTab = tab, searchQuery = "") }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun toggleWorkerDailyExpansion(workerId: String) {
        _uiState.update { current ->
            current.copy(
                expandedDailyWorkerId = if (current.expandedDailyWorkerId == workerId) null else workerId
            )
        }
    }

    fun toggleChronologyMonthExpansion(yearMonth: String) {
        _uiState.update { current ->
            current.copy(
                expandedMonthInChronology = if (current.expandedMonthInChronology == yearMonth) null else yearMonth
            )
        }
    }

    fun openCloseMonthDialog() {
        if (!_uiState.value.canUserCloseOrRevise) {
            _uiState.update {
                it.copy(
                    showPermissionWarningDialog = true,
                    permissionWarningText = "Action réservée à la Directrice. La clôture certifiée des archives nécessite l'autorisation centrale définie dans les Paramètres."
                )
            }
            return
        }
        _uiState.update { it.copy(showCloseMonthDialog = true) }
    }

    fun dismissCloseMonthDialog() {
        _uiState.update { it.copy(showCloseMonthDialog = false) }
    }

    fun confirmCloseMonth(notes: String) {
        val user = _uiState.value.currentUserProfile
        val actorName = user?.displayName ?: "Directrice"
        val actorRole = user?.role ?: MemberRole.DIRECTRICE
        val yearMonth = _uiState.value.selectedYearMonth

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, showCloseMonthDialog = false) }
            val res = archiveRepository.closeMonth(
                companyId = companyId,
                yearMonth = yearMonth,
                actorName = actorName,
                actorRole = actorRole,
                notes = notes
            )
            _uiState.update {
                it.copy(
                    isLoading = false,
                    feedbackMessage = if (res.isSuccess) "Mois $yearMonth clôturé et certifié avec succès." else "Erreur lors de la clôture.",
                    isErrorFeedback = res.isFailure
                )
            }
        }
    }

    fun openRevisionDialog() {
        if (!_uiState.value.canUserCloseOrRevise) {
            _uiState.update {
                it.copy(
                    showPermissionWarningDialog = true,
                    permissionWarningText = "La réouverture d'une archive mensuelle clôturée est soumise aux droits centraux de la Directrice (Paramètres)."
                )
            }
            return
        }
        _uiState.update { it.copy(showRevisionDialog = true) }
    }

    fun dismissRevisionDialog() {
        _uiState.update { it.copy(showRevisionDialog = false) }
    }

    fun confirmReopenForRevision(reason: String) {
        val user = _uiState.value.currentUserProfile
        val actorName = user?.displayName ?: "Directrice"
        val actorRole = user?.role ?: MemberRole.DIRECTRICE
        val yearMonth = _uiState.value.selectedYearMonth

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, showRevisionDialog = false) }
            val res = archiveRepository.reopenForRevision(
                companyId = companyId,
                yearMonth = yearMonth,
                actorName = actorName,
                actorRole = actorRole,
                reason = reason
            )
            _uiState.update {
                it.copy(
                    isLoading = false,
                    feedbackMessage = if (res.isSuccess) "Mois $yearMonth déverrouillé en statut Révision." else "Erreur lors de la réouverture.",
                    isErrorFeedback = res.isFailure
                )
            }
        }
    }

    fun confirmFinalizeRevision(notes: String) {
        val user = _uiState.value.currentUserProfile
        val actorName = user?.displayName ?: "Directrice"
        val actorRole = user?.role ?: MemberRole.DIRECTRICE
        val yearMonth = _uiState.value.selectedYearMonth

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val res = archiveRepository.finalizeRevision(
                companyId = companyId,
                yearMonth = yearMonth,
                actorName = actorName,
                actorRole = actorRole,
                notes = notes
            )
            _uiState.update {
                it.copy(
                    isLoading = false,
                    feedbackMessage = if (res.isSuccess) "Révision validée et mois $yearMonth re-clôturé." else "Erreur lors de la finalisation.",
                    isErrorFeedback = res.isFailure
                )
            }
        }
    }

    fun openAuditHistoryDialog() {
        val yearMonth = _uiState.value.selectedYearMonth
        viewModelScope.launch {
            val trail = archiveRepository.getAuditTrailForMonth(companyId, yearMonth).first()
            _uiState.update {
                it.copy(
                    showAuditHistoryDialog = true,
                    auditTrail = trail
                )
            }
        }
    }

    fun dismissAuditHistoryDialog() {
        _uiState.update { it.copy(showAuditHistoryDialog = false) }
    }

    fun dismissPermissionWarning() {
        _uiState.update { it.copy(showPermissionWarningDialog = false, permissionWarningText = null) }
    }

    fun clearFeedbackMessage() {
        _uiState.update { it.copy(feedbackMessage = null) }
    }

    class Factory(
        private val archiveRepository: ArchiveRepository,
        private val settingsRepository: SettingsRepository,
        private val attendanceRepository: AttendanceRepository,
        private val companyId: String = "atelier_nabd_01"
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ArchivesViewModel::class.java)) {
                return ArchivesViewModel(archiveRepository, settingsRepository, attendanceRepository, companyId) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
