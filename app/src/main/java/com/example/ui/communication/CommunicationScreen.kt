package com.example.ui.communication

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.domain.model.AnnouncementAudience
import com.example.domain.model.AnnouncementPriority
import com.example.domain.model.AnnouncementReadStats
import com.example.domain.model.AnnouncementStatus
import com.example.domain.model.AnnouncementType
import com.example.domain.model.ChatConversation
import com.example.domain.model.ChatMessage
import com.example.domain.model.CompanyMember
import com.example.domain.model.MemberRole
import com.example.domain.model.MessageSyncStatus
import com.example.domain.model.WorkshopAnnouncement
import com.example.ui.theme.NabdBone
import com.example.ui.theme.NabdBorder
import com.example.ui.theme.NabdCoralBackground
import com.example.ui.theme.NabdCoralBorder
import com.example.ui.theme.NabdCoralText
import com.example.ui.theme.NabdDeepViolet
import com.example.ui.theme.NabdMineralGray
import com.example.ui.theme.NabdMintBackground
import com.example.ui.theme.NabdMintBorder
import com.example.ui.theme.NabdMintText
import com.example.ui.theme.NabdObsidian
import com.example.ui.theme.NabdPurple
import com.example.ui.theme.NabdSurfaceSubtle
import com.example.ui.theme.NabdSurfaceWhite

/**
 * Single Communication Room for Suivi:
 * Contains:
 * - Real announcement bell (quiet animation, unread counter, manager announcement creator, and detail with read tracking).
 * - Discussions (Atelier group chat owned by Directrice).
 * - Messages privés (Real 1-to-1 text conversations between company members).
 */
@Composable
fun CommunicationScreen(
    state: CommunicationUiState,
    onSelectTab: (Int) -> Unit,
    onMessageInputChange: (String) -> Unit,
    onSendGroupMessage: () -> Unit,
    onOpenDirectChat: (CompanyMember) -> Unit,
    onCloseDirectChat: () -> Unit,
    onSendDirectMessage: () -> Unit,
    onRetryMessage: (String) -> Unit,
    // Announcements callbacks
    onOpenAnnouncementsSheet: () -> Unit = {},
    onCloseAnnouncementsSheet: () -> Unit = {},
    onOpenCreateAnnouncementDialog: () -> Unit = {},
    onCloseCreateAnnouncementDialog: () -> Unit = {},
    onNewAnnouncementTypeChange: (AnnouncementType) -> Unit = {},
    onNewAnnouncementTitleChange: (String) -> Unit = {},
    onNewAnnouncementContentChange: (String) -> Unit = {},
    onNewAnnouncementPriorityChange: (AnnouncementPriority) -> Unit = {},
    onNewAnnouncementAudienceChange: (AnnouncementAudience) -> Unit = {},
    onNewAnnouncementDurationDaysChange: (Int) -> Unit = {},
    onPublishAnnouncement: () -> Unit = {},
    onCancelAnnouncement: (String) -> Unit = {},
    onOpenAnnouncementDetails: (WorkshopAnnouncement) -> Unit = {},
    onCloseAnnouncementDetails: () -> Unit = {},
    onClearFeedbackMessage: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    if (state.activeDirectConversation != null) {
        BackHandler { onCloseDirectChat() }
    }

    val isManager = state.currentMember?.role == MemberRole.DIRECTRICE || state.currentMember?.canPostGroup == true

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NabdBone)
            .padding(horizontal = 16.dp)
            .testTag("communication_screen")
    ) {
        Spacer(modifier = Modifier.height(14.dp))

        // Screen Top Bar: Title + Announcement Bell + Member Badge
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Communication d'atelier",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = NabdObsidian
                )
                Text(
                    text = state.currentMember?.let {
                        "${it.displayName} • ${it.role.labelFrench}"
                    } ?: "Connexion à l'atelier…",
                    style = MaterialTheme.typography.bodySmall,
                    color = NabdDeepViolet,
                    fontWeight = FontWeight.Medium
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Real Announcement Bell with quiet animation when unread > 0
                AnnouncementBellButton(
                    unreadCount = state.unreadAnnouncementsCount,
                    onClick = onOpenAnnouncementsSheet
                )

                // Workshop Badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = NabdPurple.copy(alpha = 0.1f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NabdPurple.copy(alpha = 0.2f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Shield, contentDescription = null, tint = NabdPurple, modifier = Modifier.size(12.dp))
                        Text(text = "Atelier NABD", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = NabdPurple)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Error message banner
        if (state.errorMessage != null) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = NabdCoralBackground,
                border = androidx.compose.foundation.BorderStroke(1.dp, NabdCoralBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(imageVector = Icons.Default.Error, contentDescription = null, tint = NabdCoralText, modifier = Modifier.size(16.dp))
                    Text(text = state.errorMessage, color = NabdCoralText, fontSize = 12.sp)
                }
            }
        }

        // Sub-tabs: Discussions | Messages privés (only if not in active direct chat)
        if (state.activeDirectConversation == null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(NabdSurfaceSubtle, RoundedCornerShape(10.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                TabButton(
                    title = "Discussions",
                    isSelected = state.selectedTab == 0,
                    onClick = { onSelectTab(0) },
                    modifier = Modifier.weight(1f),
                    testTag = "comm_tab_discussions"
                )
                TabButton(
                    title = "Messages privés",
                    isSelected = state.selectedTab == 1,
                    onClick = { onSelectTab(1) },
                    modifier = Modifier.weight(1f),
                    testTag = "comm_tab_direct"
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Content Area
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            if (state.activeDirectConversation != null) {
                DirectChatThread(
                    conversation = state.activeDirectConversation,
                    messages = state.directMessages,
                    currentMember = state.currentMember,
                    messageInput = state.messageInput,
                    isSending = state.isSending,
                    onBack = onCloseDirectChat,
                    onInputChange = onMessageInputChange,
                    onSend = onSendDirectMessage,
                    onRetry = onRetryMessage
                )
            } else {
                when (state.selectedTab) {
                    0 -> GroupDiscussionsView(
                        messages = state.groupMessages,
                        currentMember = state.currentMember,
                        messageInput = state.messageInput,
                        isSending = state.isSending,
                        onInputChange = onMessageInputChange,
                        onSend = onSendGroupMessage,
                        onRetry = onRetryMessage
                    )
                    1 -> DirectConversationsListView(
                        members = state.companyMembers,
                        conversations = state.directConversations,
                        currentMember = state.currentMember,
                        onOpenChat = onOpenDirectChat
                    )
                }
            }
        }
    }

    // --- Announcement List Dialog ---
    if (state.isAnnouncementSheetOpen) {
        AnnouncementsListDialog(
            announcements = state.announcements,
            isManager = isManager,
            currentMember = state.currentMember,
            onClose = onCloseAnnouncementsSheet,
            onOpenCreate = onOpenCreateAnnouncementDialog,
            onSelectAnnouncement = onOpenAnnouncementDetails
        )
    }

    // --- Announcement Details Dialog (Triggers Real Read Receipt) ---
    if (state.viewingAnnouncementDetails != null) {
        AnnouncementDetailsDialog(
            announcement = state.viewingAnnouncementDetails,
            stats = state.viewingAnnouncementReadStats,
            isManager = isManager,
            currentUserId = state.currentMember?.userId,
            onClose = onCloseAnnouncementDetails,
            onCancelAnnouncement = { onCancelAnnouncement(state.viewingAnnouncementDetails.id) }
        )
    }

    // --- Create Announcement Dialog (Manager only) ---
    if (state.isCreateAnnouncementDialogOpen) {
        CreateAnnouncementDialog(
            type = state.newAnnouncementType,
            title = state.newAnnouncementTitle,
            content = state.newAnnouncementContent,
            priority = state.newAnnouncementPriority,
            audience = state.newAnnouncementAudience,
            durationDays = state.newAnnouncementDurationDays,
            isPublishing = state.isPublishingAnnouncement,
            onTypeChange = onNewAnnouncementTypeChange,
            onTitleChange = onNewAnnouncementTitleChange,
            onContentChange = onNewAnnouncementContentChange,
            onPriorityChange = onNewAnnouncementPriorityChange,
            onAudienceChange = onNewAnnouncementAudienceChange,
            onDurationDaysChange = onNewAnnouncementDurationDaysChange,
            onPublish = onPublishAnnouncement,
            onDismiss = onCloseCreateAnnouncementDialog
        )
    }
}

/**
 * Announcement Bell Icon with quiet bell animation when unread count > 0.
 */
@Composable
private fun AnnouncementBellButton(
    unreadCount: Int,
    onClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "bell_transition")
    val rotation by if (unreadCount > 0) {
        infiniteTransition.animateFloat(
            initialValue = -8f,
            targetValue = 8f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 600),
                repeatMode = RepeatMode.Reverse
            ),
            label = "bell_rotation"
        )
    } else {
        androidx.compose.runtime.remember { androidx.compose.runtime.mutableFloatStateOf(0f) }
    }

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (unreadCount > 0) NabdCoralBackground else NabdSurfaceWhite,
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = if (unreadCount > 0) NabdCoralBorder else NabdBorder
        ),
        modifier = Modifier
            .clickable { onClick() }
            .testTag("announcement_bell_button")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = if (unreadCount > 0) Icons.Default.NotificationsActive else Icons.Default.Notifications,
                contentDescription = "Annonces d'atelier",
                tint = if (unreadCount > 0) NabdCoralText else NabdObsidian,
                modifier = Modifier
                    .size(18.dp)
                    .rotate(rotation)
            )
            if (unreadCount > 0) {
                Surface(
                    shape = CircleShape,
                    color = NabdCoralText,
                    modifier = Modifier.size(18.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = unreadCount.toString(),
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            } else {
                Text(
                    text = "Annonces",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = NabdObsidian
                )
            }
        }
    }
}

/**
 * Dialog displaying active and past announcements.
 */
@Composable
private fun AnnouncementsListDialog(
    announcements: List<WorkshopAnnouncement>,
    isManager: Boolean,
    currentMember: CompanyMember?,
    onClose: () -> Unit,
    onOpenCreate: () -> Unit,
    onSelectAnnouncement: (WorkshopAnnouncement) -> Unit
) {
    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = NabdBone,
            border = androidx.compose.foundation.BorderStroke(1.dp, NabdBorder),
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .heightIn(max = 640.dp)
                .padding(vertical = 16.dp)
                .testTag("announcements_list_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Campaign, contentDescription = null, tint = NabdPurple, modifier = Modifier.size(24.dp))
                        Column {
                            Text(
                                text = "Annonces d'atelier",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = NabdObsidian
                            )
                            Text(
                                text = "Diffusions officielles de la direction",
                                style = MaterialTheme.typography.bodySmall,
                                color = NabdMineralGray
                            )
                        }
                    }

                    IconButton(onClick = onClose, modifier = Modifier.size(32.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Fermer", tint = NabdObsidian)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Manager Action: Nouvelle annonce
                if (isManager) {
                    Button(
                        onClick = onOpenCreate,
                        colors = ButtonDefaults.buttonColors(containerColor = NabdPurple),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("create_announcement_button")
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Publier une nouvelle annonce", fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                HorizontalDivider(color = NabdBorder, thickness = 0.5.dp)
                Spacer(modifier = Modifier.height(10.dp))

                // Filtered announcements visible to current member
                val visibleAnnouncements = announcements.filter { a ->
                    if (isManager) true
                    else currentMember != null && a.isAudienceEligible(currentMember) && !a.isCancelled
                }

                if (visibleAnnouncements.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Campaign, contentDescription = null, tint = NabdMineralGray, modifier = Modifier.size(36.dp))
                            Text(
                                text = "Aucune annonce active",
                                color = NabdObsidian,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Les consignes de l'atelier apparaîtront ici.",
                                color = NabdMineralGray,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(visibleAnnouncements, key = { it.id }) { announcement ->
                            AnnouncementCard(
                                announcement = announcement,
                                onClick = { onSelectAnnouncement(announcement) }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Announcement Card in list.
 */
@Composable
private fun AnnouncementCard(
    announcement: WorkshopAnnouncement,
    onClick: () -> Unit
) {
    val isActive = announcement.isCurrentlyActive
    val isUnread = !announcement.isReadLocally && isActive

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = NabdSurfaceWhite,
        border = androidx.compose.foundation.BorderStroke(
            width = if (isUnread) 1.5.dp else 1.dp,
            color = if (isUnread) NabdCoralBorder else NabdBorder
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("announcement_item_${announcement.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Badges row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TypeBadge(type = announcement.type)
                    PriorityBadge(priority = announcement.priority)
                }

                // Status or Unread dot
                if (isUnread) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = NabdCoralBackground,
                        border = androidx.compose.foundation.BorderStroke(1.dp, NabdCoralBorder)
                    ) {
                        Text(
                            text = "Non lu",
                            color = NabdCoralText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                } else if (announcement.isCancelled) {
                    Text(text = "Annulé", color = NabdMineralGray, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                } else if (announcement.isExpired) {
                    Text(text = "Expiré", color = NabdMineralGray, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Title
            Text(
                text = announcement.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = NabdObsidian
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Preview
            Text(
                text = announcement.content,
                style = MaterialTheme.typography.bodySmall,
                color = NabdDeepViolet,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Meta row: Audience + Validity in Tunis time
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Audience : ${announcement.targetAudience.labelFrench}",
                    fontSize = 11.sp,
                    color = NabdPurple,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Jusqu'au ${announcement.formattedEndTunis}",
                    fontSize = 11.sp,
                    color = NabdMineralGray
                )
            }
        }
    }
}

/**
 * Announcement Details Dialog (Reading happens strictly on open).
 */
@Composable
private fun AnnouncementDetailsDialog(
    announcement: WorkshopAnnouncement,
    stats: AnnouncementReadStats?,
    isManager: Boolean,
    currentUserId: String?,
    onClose: () -> Unit,
    onCancelAnnouncement: () -> Unit
) {
    val scrollState = rememberScrollState()

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = NabdBone,
            border = androidx.compose.foundation.BorderStroke(1.dp, NabdBorder),
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .heightIn(max = 680.dp)
                .padding(vertical = 16.dp)
                .testTag("announcement_details_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TypeBadge(type = announcement.type)
                    IconButton(onClick = onClose, modifier = Modifier.size(32.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Fermer", tint = NabdObsidian)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(scrollState)
                ) {
                    // Title
                    Text(
                        text = announcement.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = NabdObsidian
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Meta: Author & Dates in Africa/Tunis
                    Text(
                        text = "Par ${announcement.authorName} (${announcement.authorRole.labelFrench})",
                        style = MaterialTheme.typography.bodySmall,
                        color = NabdPurple,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Publié le ${announcement.formattedStartTunis} • Valable jusqu'au ${announcement.formattedEndTunis}",
                        style = MaterialTheme.typography.bodySmall,
                        color = NabdMineralGray
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Badges row
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PriorityBadge(priority = announcement.priority)
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = NabdSurfaceSubtle,
                            border = androidx.compose.foundation.BorderStroke(1.dp, NabdBorder)
                        ) {
                            Text(
                                text = "Pour : ${announcement.targetAudience.labelFrench}",
                                fontSize = 11.sp,
                                color = NabdObsidian,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Full Content
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = NabdSurfaceWhite,
                        border = androidx.compose.foundation.BorderStroke(1.dp, NabdBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = announcement.content,
                            style = MaterialTheme.typography.bodyMedium,
                            color = NabdObsidian,
                            lineHeight = 22.sp,
                            modifier = Modifier.padding(16.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Read Confirmation for the current user
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = NabdMintBackground,
                        border = androidx.compose.foundation.BorderStroke(1.dp, NabdMintBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = NabdMintText, modifier = Modifier.size(16.dp))
                            Text(
                                text = "Lecture enregistrée avec succès (heure de Tunis)",
                                fontSize = 12.sp,
                                color = NabdMintText,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // --- Manager-Only Read Tracking Panel (Lu par X / Y) ---
                    if (isManager && stats != null) {
                        Spacer(modifier = Modifier.height(20.dp))
                        ManagerReadTrackingPanel(stats = stats)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Footer Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isManager && announcement.isCurrentlyActive) {
                        OutlinedButton(
                            onClick = onCancelAnnouncement,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = NabdCoralText),
                            border = androidx.compose.foundation.BorderStroke(1.dp, NabdCoralBorder),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Cancel, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "Annuler l'annonce", fontSize = 12.sp)
                        }
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }

                    Button(
                        onClick = onClose,
                        colors = ButtonDefaults.buttonColors(containerColor = NabdPurple),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(text = "Fermer")
                    }
                }
            }
        }
    }
}

/**
 * Manager-Only Read Tracking Breakdown:
 * Lu par X/Y, Non lu par Z, Readers list with Africa/Tunis timestamps, Unread list.
 */
@Composable
private fun ManagerReadTrackingPanel(
    stats: AnnouncementReadStats
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = NabdSurfaceWhite,
        border = androidx.compose.foundation.BorderStroke(1.dp, NabdPurple.copy(alpha = 0.3f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("manager_read_tracking_panel")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(imageVector = Icons.Default.Visibility, contentDescription = null, tint = NabdPurple, modifier = Modifier.size(18.dp))
                    Text(
                        text = "Suivi de lecture (Gérance)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = NabdObsidian
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = NabdPurple.copy(alpha = 0.1f)
                ) {
                    Text(
                        text = stats.summaryTextFrench,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = NabdPurple,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Audience ciblée : ${stats.targetAudienceCount} membres • ${stats.unreadSummaryTextFrench}",
                fontSize = 12.sp,
                color = NabdDeepViolet
            )

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = NabdBorder, thickness = 0.5.dp)
            Spacer(modifier = Modifier.height(10.dp))

            // Section 1: Who read (Ont lu)
            Text(
                text = "Ont lu l'annonce (${stats.readCount})",
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = NabdMintText
            )
            Spacer(modifier = Modifier.height(6.dp))

            if (stats.readers.isEmpty()) {
                Text(
                    text = "Aucun membre n'a encore ouvert cette annonce.",
                    fontSize = 11.sp,
                    color = NabdMineralGray
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    stats.readers.forEach { receipt ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(NabdMintBackground, RoundedCornerShape(6.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = receipt.recipientName,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = NabdObsidian
                                )
                                Text(
                                    text = receipt.recipientRole.labelFrench,
                                    fontSize = 10.sp,
                                    color = NabdMineralGray
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = receipt.firstReadTimeFormattedTunis,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NabdMintText
                                )
                                Text(
                                    text = receipt.firstReadDateFormattedTunis,
                                    fontSize = 10.sp,
                                    color = NabdMineralGray
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Section 2: Unread (En attente de lecture)
            if (stats.unreadMembers.isNotEmpty()) {
                Text(
                    text = "En attente de lecture (${stats.unreadCount})",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = NabdCoralText
                )
                Spacer(modifier = Modifier.height(6.dp))

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    stats.unreadMembers.forEach { member ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(NabdSurfaceSubtle, RoundedCornerShape(6.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = member.displayName,
                                fontSize = 12.sp,
                                color = NabdObsidian
                            )
                            Text(
                                text = member.role.labelFrench,
                                fontSize = 10.sp,
                                color = NabdMineralGray
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Ce relevé est confidentiel et accessible uniquement par la direction.",
                fontSize = 10.sp,
                color = NabdMineralGray
            )
        }
    }
}

/**
 * Dialog for Directrice to compose and publish an announcement.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CreateAnnouncementDialog(
    type: AnnouncementType,
    title: String,
    content: String,
    priority: AnnouncementPriority,
    audience: AnnouncementAudience,
    durationDays: Int,
    isPublishing: Boolean,
    onTypeChange: (AnnouncementType) -> Unit,
    onTitleChange: (String) -> Unit,
    onContentChange: (String) -> Unit,
    onPriorityChange: (AnnouncementPriority) -> Unit,
    onAudienceChange: (AnnouncementAudience) -> Unit,
    onDurationDaysChange: (Int) -> Unit,
    onPublish: () -> Unit,
    onDismiss: () -> Unit
) {
    val scrollState = rememberScrollState()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = NabdBone,
            border = androidx.compose.foundation.BorderStroke(1.dp, NabdBorder),
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .heightIn(max = 680.dp)
                .padding(vertical = 16.dp)
                .testTag("create_announcement_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Publier une annonce d'atelier",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = NabdObsidian
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Fermer", tint = NabdObsidian)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(scrollState),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Type selector
                    Text(text = "Type d'annonce", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = NabdObsidian)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        AnnouncementType.values().forEach { t ->
                            FilterChip(
                                label = t.labelFrench,
                                selected = type == t,
                                onClick = { onTypeChange(t) }
                            )
                        }
                    }

                    // Priority selector
                    Text(text = "Priorité", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = NabdObsidian)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        AnnouncementPriority.values().forEach { p ->
                            FilterChip(
                                label = p.labelFrench,
                                selected = priority == p,
                                onClick = { onPriorityChange(p) }
                            )
                        }
                    }

                    // Target Audience
                    Text(text = "Audience ciblée", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = NabdObsidian)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        AnnouncementAudience.values().forEach { a ->
                            FilterChip(
                                label = a.labelFrench,
                                selected = audience == a,
                                onClick = { onAudienceChange(a) }
                            )
                        }
                    }

                    // Title
                    Text(text = "Titre de l'annonce", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = NabdObsidian)
                    OutlinedTextField(
                        value = title,
                        onValueChange = onTitleChange,
                        placeholder = { Text("Ex: Changement d'horaires pour la semaine prochaine…") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("announcement_title_input"),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NabdPurple,
                            unfocusedBorderColor = NabdBorder,
                            focusedContainerColor = NabdSurfaceWhite,
                            unfocusedContainerColor = NabdSurfaceWhite
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )

                    // Content
                    Text(text = "Contenu de l'annonce", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = NabdObsidian)
                    OutlinedTextField(
                        value = content,
                        onValueChange = onContentChange,
                        placeholder = { Text("Détails complets de la consigne…") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 110.dp)
                            .testTag("announcement_content_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NabdPurple,
                            unfocusedBorderColor = NabdBorder,
                            focusedContainerColor = NabdSurfaceWhite,
                            unfocusedContainerColor = NabdSurfaceWhite
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )

                    // Duration selector
                    Text(text = "Durée de validité", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = NabdObsidian)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(1 to "1 jour", 3 to "3 jours", 7 to "7 jours", 14 to "14 jours", 30 to "30 jours").forEach { (days, label) ->
                            FilterChip(
                                label = label,
                                selected = durationDays == days,
                                onClick = { onDurationDaysChange(days) }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(text = "Annuler")
                    }

                    Button(
                        onClick = onPublish,
                        enabled = !isPublishing && title.isNotBlank() && content.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = NabdPurple),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1.4f)
                            .testTag("submit_announcement_button")
                    ) {
                        if (isPublishing) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "Publication…")
                        } else {
                            Text(text = "Publier l'annonce", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (selected) NabdPurple else NabdSurfaceWhite,
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = if (selected) NabdPurple else NabdBorder
        ),
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) Color.White else NabdObsidian,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun TypeBadge(type: AnnouncementType) {
    val (bgColor, borderColor, txtColor) = when (type) {
        AnnouncementType.INFORMATION -> Triple(NabdSurfaceSubtle, NabdBorder, NabdPurple)
        AnnouncementType.NOTIFICATION -> Triple(NabdCoralBackground, NabdCoralBorder, NabdCoralText)
        AnnouncementType.SECURITE -> Triple(Color(0xFFFEF3C7), Color(0xFFFDE68A), Color(0xFFB45309))
        AnnouncementType.ORGANISATION -> Triple(NabdMintBackground, NabdMintBorder, NabdMintText)
    }
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = bgColor,
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
    ) {
        Text(
            text = type.labelFrench,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = txtColor,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
        )
    }
}

@Composable
private fun PriorityBadge(priority: AnnouncementPriority) {
    val (bgColor, borderColor, txtColor) = when (priority) {
        AnnouncementPriority.NORMALE -> Triple(NabdSurfaceSubtle, NabdBorder, NabdMineralGray)
        AnnouncementPriority.IMPORTANTE -> Triple(Color(0xFFEDE9FE), Color(0xFFDDD6FE), NabdPurple)
        AnnouncementPriority.URGENTE -> Triple(NabdCoralBackground, NabdCoralBorder, NabdCoralText)
    }
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = bgColor,
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
    ) {
        Text(
            text = priority.labelFrench,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            color = txtColor,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

// --- Retained Discussions and Private Messages sub-composables ---

@Composable
private fun TabButton(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) NabdSurfaceWhite else Color.Transparent,
        modifier = modifier
            .clickable { onClick() }
            .testTag(testTag)
    ) {
        Box(
            modifier = Modifier.padding(vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) NabdPurple else NabdMineralGray
            )
        }
    }
}

@Composable
private fun GroupDiscussionsView(
    messages: List<ChatMessage>,
    currentMember: CompanyMember?,
    messageInput: String,
    isSending: Boolean,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    onRetry: (String) -> Unit
) {
    val listState = rememberLazyListState()
    val canPost = currentMember?.role == MemberRole.DIRECTRICE || currentMember?.canPostGroup == true

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = NabdSurfaceWhite,
            border = androidx.compose.foundation.BorderStroke(1.dp, NabdBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(imageVector = Icons.Default.Chat, contentDescription = null, tint = NabdPurple, modifier = Modifier.size(18.dp))
                Column {
                    Text(text = "Discussion générale d'atelier", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = NabdObsidian)
                    Text(text = "Espace collectif de l'équipe pour consignes et échanges", style = MaterialTheme.typography.bodySmall, color = NabdMineralGray)
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (messages.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(imageVector = Icons.Default.Chat, contentDescription = null, tint = NabdMineralGray, modifier = Modifier.size(32.dp))
                    Text(text = "Aucun message dans l'atelier pour l'instant.", color = NabdMineralGray, fontSize = 13.sp)
                }
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(messages, key = { it.id }) { message ->
                    val isMe = message.senderId == currentMember?.userId
                    MessageBubble(message = message, isMe = isMe, onRetry = onRetry)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (canPost) {
            MessageInputBar(
                value = messageInput,
                onValueChange = onInputChange,
                onSend = onSend,
                isSending = isSending,
                placeholder = "Écrire un message à l'atelier…"
            )
        } else {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = NabdSurfaceSubtle,
                border = androidx.compose.foundation.BorderStroke(1.dp, NabdBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = NabdMineralGray, modifier = Modifier.size(16.dp))
                    Text(text = "Publication réservée aux gérantes et superviseures.", style = MaterialTheme.typography.bodySmall, color = NabdMineralGray)
                }
            }
        }
    }
}

@Composable
private fun DirectConversationsListView(
    members: List<CompanyMember>,
    conversations: List<ChatConversation>,
    currentMember: CompanyMember?,
    onOpenChat: (CompanyMember) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "Membres de l'atelier",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = NabdObsidian
        )
        Spacer(modifier = Modifier.height(8.dp))

        if (members.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "Aucun autre membre enregistré dans cet atelier.", color = NabdMineralGray, fontSize = 13.sp)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(members, key = { it.id }) { member ->
                    MemberRowItem(
                        member = member,
                        onClick = { onOpenChat(member) }
                    )
                }
            }
        }
    }
}

@Composable
private fun MemberRowItem(
    member: CompanyMember,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = NabdSurfaceWhite,
        border = androidx.compose.foundation.BorderStroke(1.dp, NabdBorder),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("member_row_${member.userId}")
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = NabdPurple.copy(alpha = 0.15f),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = member.displayName.take(2).uppercase(),
                        color = NabdPurple,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = member.displayName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = NabdObsidian
                )
                Text(
                    text = member.role.labelFrench,
                    style = MaterialTheme.typography.bodySmall,
                    color = NabdDeepViolet
                )
            }

            Icon(
                imageVector = Icons.Default.Chat,
                contentDescription = "Ouvrir la discussion",
                tint = NabdPurple,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun DirectChatThread(
    conversation: ChatConversation,
    messages: List<ChatMessage>,
    currentMember: CompanyMember?,
    messageInput: String,
    isSending: Boolean,
    onBack: () -> Unit,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    onRetry: (String) -> Unit
) {
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = NabdSurfaceWhite,
            border = androidx.compose.foundation.BorderStroke(1.dp, NabdBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour", tint = NabdObsidian)
                }
                Spacer(modifier = Modifier.width(6.dp))
                Column {
                    Text(
                        text = conversation.otherParticipantName ?: conversation.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = NabdObsidian
                    )
                    Text(
                        text = conversation.otherParticipantRole?.labelFrench ?: "Message privé",
                        style = MaterialTheme.typography.bodySmall,
                        color = NabdDeepViolet
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (messages.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "Début de la conversation privée.", color = NabdMineralGray, fontSize = 13.sp)
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(messages, key = { it.id }) { message ->
                    val isMe = message.senderId == currentMember?.userId
                    MessageBubble(message = message, isMe = isMe, onRetry = onRetry)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        MessageInputBar(
            value = messageInput,
            onValueChange = onInputChange,
            onSend = onSend,
            isSending = isSending,
            placeholder = "Écrire un message privé…"
        )
    }
}

@Composable
private fun MessageBubble(
    message: ChatMessage,
    isMe: Boolean,
    onRetry: (String) -> Unit
) {
    val bubbleColor = if (isMe) NabdPurple else NabdSurfaceWhite
    val textColor = if (isMe) Color.White else NabdObsidian
    val metaColor = if (isMe) Color.White.copy(alpha = 0.8f) else NabdMineralGray

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
    ) {
        if (!isMe) {
            Text(
                text = "${message.senderName} • ${message.senderRole.labelFrench}",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = NabdDeepViolet,
                modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
            )
        }

        Surface(
            shape = RoundedCornerShape(
                topStart = 14.dp,
                topEnd = 14.dp,
                bottomStart = if (isMe) 14.dp else 2.dp,
                bottomEnd = if (isMe) 2.dp else 14.dp
            ),
            color = bubbleColor,
            border = if (!isMe) androidx.compose.foundation.BorderStroke(1.dp, NabdBorder) else null,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = textColor,
                    lineHeight = 20.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.align(Alignment.End),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = message.timeFormatted,
                        fontSize = 10.sp,
                        color = metaColor
                    )
                    if (isMe) {
                        when (message.syncStatus) {
                            MessageSyncStatus.SYNCED -> {
                                Icon(imageVector = Icons.Default.Check, contentDescription = "Synchronisé", tint = metaColor, modifier = Modifier.size(12.dp))
                            }
                            MessageSyncStatus.PENDING -> {
                                Icon(imageVector = Icons.Default.Schedule, contentDescription = "En attente de synchronisation", tint = metaColor, modifier = Modifier.size(12.dp))
                            }
                            MessageSyncStatus.FAILED -> {
                                Icon(imageVector = Icons.Default.Error, contentDescription = "Échec d'envoi", tint = NabdCoralText, modifier = Modifier.size(12.dp))
                            }
                        }
                    }
                }
            }
        }

        if (isMe && message.syncStatus == MessageSyncStatus.FAILED) {
            Row(
                modifier = Modifier
                    .clickable { onRetry(message.id) }
                    .padding(top = 2.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(imageVector = Icons.Default.Refresh, contentDescription = "Réessayer", tint = NabdCoralText, modifier = Modifier.size(12.dp))
                Text(text = "Échec d'envoi • Réessayer", color = NabdCoralText, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun MessageInputBar(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    isSending: Boolean,
    placeholder: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder, fontSize = 13.sp, color = NabdMineralGray) },
            modifier = Modifier
                .weight(1f)
                .testTag("communication_message_input"),
            maxLines = 3,
            shape = RoundedCornerShape(20.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = NabdPurple,
                unfocusedBorderColor = NabdBorder,
                focusedContainerColor = NabdSurfaceWhite,
                unfocusedContainerColor = NabdSurfaceWhite
            )
        )

        IconButton(
            onClick = onSend,
            enabled = value.isNotBlank() && !isSending,
            modifier = Modifier
                .size(46.dp)
                .background(if (value.isNotBlank() && !isSending) NabdPurple else NabdSurfaceSubtle, CircleShape)
                .testTag("communication_send_button")
        ) {
            if (isSending) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
            } else {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Envoyer",
                    tint = if (value.isNotBlank()) Color.White else NabdMineralGray,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
