package com.example.ui.archives

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.ArchiveMonthStatus
import com.example.domain.model.AttendanceStatus
import com.example.domain.model.AuditEvent
import com.example.domain.model.DailyAttendanceRecordDetail
import com.example.domain.model.MonthlyWorkerArchiveItem
import com.example.domain.model.WorkedMonthSummary
import com.example.domain.model.Worker
import com.example.domain.model.WorkshopMonthlyArchiveSummary
import com.example.ui.theme.NabdBone
import com.example.ui.theme.NabdBorder
import com.example.ui.theme.NabdDeepViolet
import com.example.ui.theme.NabdMineralGray
import com.example.ui.theme.NabdMuted
import com.example.ui.theme.NabdObsidian
import com.example.ui.theme.NabdPurple
import com.example.ui.theme.NabdSurfaceSubtle
import com.example.ui.theme.NabdSurfaceWhite

private val ColorStatusClosed = Color(0xFF2E7D32)
private val ColorStatusRevision = Color(0xFFE65100)
private val ColorStatusOpen = Color(0xFF6750A4)

/**
 * Stage 7: Centralized Workshop and Worker Chronological Archives.
 * Derived strictly from confirmed Pointage data with month locking, revision, auditing,
 * and central permissions enforcement.
 */
@Composable
fun ArchivesScreen(
    workerFilter: Worker? = null,
    workedMonths: List<WorkedMonthSummary> = emptyList(),
    onBack: () -> Unit = {},
    viewModel: ArchivesViewModel? = null,
    modifier: Modifier = Modifier
) {
    if (viewModel == null) {
        // Fallback backward-compatible view
        LegacyArchivesFallback(
            workerFilter = workerFilter,
            workedMonths = workedMonths,
            onBack = onBack,
            modifier = modifier
        )
        return
    }

    val state by viewModel.uiState.collectAsState()

    // Handle back button when inspecting a specific worker
    if (state.selectedTab == ArchiveTab.CHRONOLOGIE_OUVRIERE || state.selectedWorker != null) {
        BackHandler {
            viewModel.backToWorkshopOverview()
            if (workerFilter != null) {
                onBack()
            }
        }
    }

    // Auto-select worker if passed via navigation from WorkerProfile
    remember(workerFilter) {
        if (workerFilter != null && state.selectedWorker?.id != workerFilter.id) {
            viewModel.selectWorkerForChronology(workerFilter)
        }
        true
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NabdBone)
            .padding(horizontal = 16.dp)
            .testTag("archives_screen")
    ) {
        Spacer(modifier = Modifier.height(10.dp))

        // Feedback Banner
        if (state.feedbackMessage != null) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .testTag("archive_feedback_banner"),
                color = if (state.isErrorFeedback) Color(0xFFFFEBEE) else Color(0xFFE8F5E9),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (state.isErrorFeedback) Color(0xFFEF9A9A) else Color(0xFFA5D6A7)
                )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (state.isErrorFeedback) Icons.Default.Warning else Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = if (state.isErrorFeedback) Color(0xFFC62828) else Color(0xFF2E7D32),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = state.feedbackMessage ?: "",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                            color = if (state.isErrorFeedback) Color(0xFFC62828) else Color(0xFF2E7D32)
                        )
                    }
                    IconButton(
                        onClick = { viewModel.clearFeedbackMessage() },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Fermer",
                            tint = NabdMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        // Main Navigation / Header Bar
        if (state.selectedTab == ArchiveTab.CHRONOLOGIE_OUVRIERE && state.selectedWorker != null) {
            WorkerChronologyHeader(
                worker = state.selectedWorker!!,
                onBack = {
                    viewModel.backToWorkshopOverview()
                    if (workerFilter != null) onBack()
                }
            )
        } else {
            WorkshopArchiveHeader(
                selectedYearMonth = state.selectedYearMonth,
                availableMonths = state.availableMonths,
                currentStatus = state.workshopMonthlyArchive?.status ?: ArchiveMonthStatus.OUVERT,
                onMonthSelected = { viewModel.selectMonth(it) },
                onPreviousMonth = { viewModel.previousMonth() },
                onNextMonth = { viewModel.nextMonth() }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Search Bar
        OutlinedTextField(
            value = state.searchQuery,
            onValueChange = { viewModel.setSearchQuery(it) },
            placeholder = {
                Text(
                    if (state.selectedTab == ArchiveTab.CHRONOLOGIE_OUVRIERE)
                        "Rechercher un mois ou une date travaillée..."
                    else
                        "Rechercher une ouvrière (nom, prénom, rôle)..."
                )
            },
            leadingIcon = {
                Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = NabdMuted)
            },
            trailingIcon = {
                if (state.searchQuery.isNotEmpty()) {
                    IconButton(onClick = { viewModel.setSearchQuery("") }) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Effacer", tint = NabdMuted)
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("archive_search_input"),
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = NabdSurfaceWhite,
                unfocusedContainerColor = NabdSurfaceWhite,
                focusedBorderColor = NabdPurple,
                unfocusedBorderColor = NabdBorder
            )
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Content Area
        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = NabdPurple)
            }
        } else if (state.selectedTab == ArchiveTab.CHRONOLOGIE_OUVRIERE && state.selectedWorker != null) {
            WorkerChronologicalContent(
                worker = state.selectedWorker!!,
                months = state.filteredWorkerChronologicalMonths,
                expandedMonth = state.expandedMonthInChronology,
                onToggleExpandMonth = { viewModel.toggleChronologyMonthExpansion(it) }
            )
        } else {
            WorkshopMonthlyContent(
                summary = state.workshopMonthlyArchive,
                filteredWorkers = state.filteredWorkshopWorkers,
                canUserCloseOrRevise = state.canUserCloseOrRevise,
                expandedWorkerId = state.expandedDailyWorkerId,
                onToggleWorkerDaily = { viewModel.toggleWorkerDailyExpansion(it) },
                onWorkerClicked = { viewModel.selectWorkerFromArchiveItem(it) },
                onOpenCloseDialog = { viewModel.openCloseMonthDialog() },
                onOpenRevisionDialog = { viewModel.openRevisionDialog() },
                onFinalizeRevision = { viewModel.confirmFinalizeRevision("Révision validée") },
                onOpenAuditHistory = { viewModel.openAuditHistoryDialog() }
            )
        }
    }

    // Dialogs
    if (state.showCloseMonthDialog) {
        CloseMonthDialog(
            yearMonth = state.selectedYearMonth,
            displayName = state.workshopMonthlyArchive?.displayNameFrench ?: state.selectedYearMonth,
            onDismiss = { viewModel.dismissCloseMonthDialog() },
            onConfirm = { notes -> viewModel.confirmCloseMonth(notes) }
        )
    }

    if (state.showRevisionDialog) {
        ReopenRevisionDialog(
            yearMonth = state.selectedYearMonth,
            displayName = state.workshopMonthlyArchive?.displayNameFrench ?: state.selectedYearMonth,
            onDismiss = { viewModel.dismissRevisionDialog() },
            onConfirm = { reason -> viewModel.confirmReopenForRevision(reason) }
        )
    }

    if (state.showAuditHistoryDialog) {
        AuditHistoryDialog(
            yearMonth = state.selectedYearMonth,
            auditTrail = state.auditTrail,
            onDismiss = { viewModel.dismissAuditHistoryDialog() }
        )
    }

    if (state.showPermissionWarningDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissPermissionWarning() },
            icon = {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = null,
                    tint = NabdDeepViolet,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text("Autorisation Centrale Requise", fontWeight = FontWeight.Bold, color = NabdObsidian)
            },
            text = {
                Text(
                    text = state.permissionWarningText ?: "Droits insuffisants.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = NabdObsidian
                )
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.dismissPermissionWarning() },
                    colors = ButtonDefaults.buttonColors(containerColor = NabdPurple)
                ) {
                    Text("Compris")
                }
            }
        )
    }
}

/**
 * Header for general workshop archive with month navigation.
 */
@Composable
private fun WorkshopArchiveHeader(
    selectedYearMonth: String,
    availableMonths: List<String>,
    currentStatus: ArchiveMonthStatus,
    onMonthSelected: (String) -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Archives d'Atelier",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp
                        ),
                        color = NabdObsidian
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    StatusBadge(status = currentStatus)
                }
                Text(
                    text = "Registre certifié des pointages mensuels",
                    style = MaterialTheme.typography.bodySmall,
                    color = NabdMineralGray
                )
            }

            // Month Previous / Next Arrows
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(
                    onClick = onPreviousMonth,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(NabdSurfaceWhite)
                        .border(1.dp, NabdBorder, CircleShape)
                        .testTag("btn_prev_month")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Mois précédent",
                        tint = NabdObsidian,
                        modifier = Modifier.size(16.dp)
                    )
                }
                IconButton(
                    onClick = onNextMonth,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(NabdSurfaceWhite)
                        .border(1.dp, NabdBorder, CircleShape)
                        .testTag("btn_next_month")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Mois suivant",
                        tint = NabdObsidian,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Month selector chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            availableMonths.forEach { monthIso ->
                val isSelected = monthIso == selectedYearMonth
                val parts = monthIso.split("-")
                val mNum = parts.getOrNull(1)?.toIntOrNull() ?: 1
                val yNum = parts.getOrNull(0) ?: "2026"
                val shortMonthName = when (mNum) {
                    1 -> "Jan"; 2 -> "Fév"; 3 -> "Mar"; 4 -> "Avr"
                    5 -> "Mai"; 6 -> "Juin"; 7 -> "Juil"; 8 -> "Août"
                    9 -> "Sept"; 10 -> "Oct"; 11 -> "Nov"; 12 -> "Déc"
                    else -> monthIso
                }
                val label = "$shortMonthName $yNum"

                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .clickable { onMonthSelected(monthIso) }
                        .testTag("month_chip_$monthIso"),
                    color = if (isSelected) NabdPurple else NabdSurfaceWhite,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isSelected) NabdPurple else NabdBorder
                    ),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            ),
                            color = if (isSelected) Color.White else NabdObsidian
                        )
                    }
                }
            }
        }
    }
}

/**
 * Header for a specific worker's chronological archive.
 */
@Composable
private fun WorkerChronologyHeader(
    worker: Worker,
    onBack: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(NabdSurfaceWhite)
                .border(1.dp, NabdBorder, CircleShape)
                .testTag("btn_chronology_back")
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Retour",
                tint = NabdObsidian
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = worker.fullName,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    ),
                    color = NabdObsidian
                )
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    color = if (worker.isActive) Color(0xFFE8F5E9) else Color(0xFFECEFF1),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = if (worker.isActive) "Active" else "Archivée",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = if (worker.isActive) Color(0xFF2E7D32) else Color(0xFF546E7A),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            Text(
                text = "${worker.role} • Archive chronologique de présence",
                style = MaterialTheme.typography.bodySmall,
                color = NabdMineralGray
            )
        }
    }
}

/**
 * General Monthly Workshop Content view.
 */
@Composable
private fun WorkshopMonthlyContent(
    summary: WorkshopMonthlyArchiveSummary?,
    filteredWorkers: List<MonthlyWorkerArchiveItem>,
    canUserCloseOrRevise: Boolean,
    expandedWorkerId: String?,
    onToggleWorkerDaily: (String) -> Unit,
    onWorkerClicked: (MonthlyWorkerArchiveItem) -> Unit,
    onOpenCloseDialog: () -> Unit,
    onOpenRevisionDialog: () -> Unit,
    onFinalizeRevision: () -> Unit,
    onOpenAuditHistory: () -> Unit
) {
    if (summary == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Chargement des archives...",
                color = NabdMuted,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Month KPI summary cards
        item {
            WorkshopKpiRow(summary = summary)
        }

        // Status & Lifecycle Action Card
        item {
            MonthLifecycleCard(
                summary = summary,
                canUserCloseOrRevise = canUserCloseOrRevise,
                onOpenCloseDialog = onOpenCloseDialog,
                onOpenRevisionDialog = onOpenRevisionDialog,
                onFinalizeRevision = onFinalizeRevision,
                onOpenAuditHistory = onOpenAuditHistory
            )
        }

        // Section Title
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Ouvrières enregistrées (${filteredWorkers.size})",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = NabdObsidian
                )
                Text(
                    text = "Données confirmées",
                    style = MaterialTheme.typography.labelSmall,
                    color = NabdDeepViolet
                )
            }
        }

        // Workers List
        if (filteredWorkers.isEmpty()) {
            item {
                EmptyStateCard(
                    title = "Aucun pointage pour ce mois",
                    description = "Aucune ouvrière n'a enregistré d'heures ou d'activité de pointage sur ce mois."
                )
            }
        } else {
            items(filteredWorkers, key = { it.workerId }) { workerItem ->
                MonthlyWorkerCard(
                    item = workerItem,
                    isDailyExpanded = expandedWorkerId == workerItem.workerId,
                    onToggleDaily = { onToggleWorkerDaily(workerItem.workerId) },
                    onWorkerClicked = { onWorkerClicked(workerItem) }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

/**
 * Row displaying key metrics for the selected month.
 */
@Composable
private fun WorkshopKpiRow(summary: WorkshopMonthlyArchiveSummary) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        KpiCard(
            title = "Heures Travaillées",
            value = summary.totalHoursWorkedFormatted,
            icon = Icons.Default.Schedule,
            iconTint = NabdDeepViolet,
            modifier = Modifier.weight(1f)
        )
        KpiCard(
            title = "Ouvrières",
            value = "${summary.totalWorkersActive}",
            icon = Icons.Default.People,
            iconTint = NabdPurple,
            modifier = Modifier.weight(1f)
        )
        KpiCard(
            title = "Jours Effectifs",
            value = "${summary.totalDaysWorked} j",
            icon = Icons.Default.CalendarToday,
            iconTint = Color(0xFF00796B),
            modifier = Modifier.weight(1f)
        )
        KpiCard(
            title = "Masse Salariale",
            value = summary.totalGrossPayrollFormattedTnd,
            icon = Icons.Default.Payments,
            iconTint = Color(0xFFE65100),
            modifier = Modifier.weight(1.3f)
        )
    }
}

@Composable
private fun KpiCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, NabdBorder, RoundedCornerShape(12.dp)),
        color = NabdSurfaceWhite,
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = NabdObsidian,
                maxLines = 1
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = NabdMineralGray,
                maxLines = 1
            )
        }
    }
}

/**
 * Month Lifecycle Banner & Certification Controls.
 */
@Composable
private fun MonthLifecycleCard(
    summary: WorkshopMonthlyArchiveSummary,
    canUserCloseOrRevise: Boolean,
    onOpenCloseDialog: () -> Unit,
    onOpenRevisionDialog: () -> Unit,
    onFinalizeRevision: () -> Unit,
    onOpenAuditHistory: () -> Unit
) {
    val status = summary.status
    val closure = summary.closure

    val containerColor = when (status) {
        ArchiveMonthStatus.CLOTURE -> Color(0xFFE8F5E9)
        ArchiveMonthStatus.EN_REVISION -> Color(0xFFFFF3E0)
        ArchiveMonthStatus.OUVERT -> NabdDeepViolet.copy(alpha = 0.05f)
    }
    val borderColor = when (status) {
        ArchiveMonthStatus.CLOTURE -> Color(0xFFA5D6A7)
        ArchiveMonthStatus.EN_REVISION -> Color(0xFFFFCC80)
        ArchiveMonthStatus.OUVERT -> NabdDeepViolet.copy(alpha = 0.2f)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("month_lifecycle_card"),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = when (status) {
                            ArchiveMonthStatus.CLOTURE -> Icons.Default.Lock
                            ArchiveMonthStatus.EN_REVISION -> Icons.Default.LockOpen
                            ArchiveMonthStatus.OUVERT -> Icons.Default.CalendarToday
                        },
                        contentDescription = null,
                        tint = when (status) {
                            ArchiveMonthStatus.CLOTURE -> ColorStatusClosed
                            ArchiveMonthStatus.EN_REVISION -> ColorStatusRevision
                            ArchiveMonthStatus.OUVERT -> NabdDeepViolet
                        },
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = when (status) {
                            ArchiveMonthStatus.CLOTURE -> "Mois Clôturé & Certifié"
                            ArchiveMonthStatus.EN_REVISION -> "Mois Déverrouillé pour Révision"
                            ArchiveMonthStatus.OUVERT -> "Mois Ouvert (Pointages en cours)"
                        },
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = when (status) {
                            ArchiveMonthStatus.CLOTURE -> ColorStatusClosed
                            ArchiveMonthStatus.EN_REVISION -> ColorStatusRevision
                            ArchiveMonthStatus.OUVERT -> NabdDeepViolet
                        }
                    )
                }

                // Audit History Button
                TextButton(
                    onClick = onOpenAuditHistory,
                    modifier = Modifier.testTag("btn_audit_history")
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = NabdDeepViolet,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Traçabilité", fontSize = 12.sp, color = NabdDeepViolet, fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Subtitle / Certification metadata
            Text(
                text = when (status) {
                    ArchiveMonthStatus.CLOTURE -> {
                        val closedBy = closure?.closedBy ?: "Directrice"
                        val date = closure?.formattedClosedDateTunis ?: "-"
                        val note = closure?.notes ?: "Certification de clôture mensuelle."
                        "Certifié le $date par $closedBy.\n$note"
                    }
                    ArchiveMonthStatus.EN_REVISION -> {
                        val reason = closure?.revisionReason ?: "Régularisation requise"
                        "Révision demandée. Motif : $reason"
                    }
                    ArchiveMonthStatus.OUVERT -> {
                        "Les pointages peuvent être complétés et vérifiés. La clôture certifie et verrouille les totaux de paie."
                    }
                },
                style = MaterialTheme.typography.bodySmall,
                color = NabdObsidian,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                when (status) {
                    ArchiveMonthStatus.OUVERT -> {
                        Button(
                            onClick = onOpenCloseDialog,
                            colors = ButtonDefaults.buttonColors(containerColor = NabdPurple),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("btn_close_month")
                        ) {
                            Icon(imageVector = Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Clôturer le mois", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    ArchiveMonthStatus.CLOTURE -> {
                        OutlinedButton(
                            onClick = onOpenRevisionDialog,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("btn_request_revision")
                        ) {
                            Icon(imageVector = Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Demander une révision", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                    ArchiveMonthStatus.EN_REVISION -> {
                        Button(
                            onClick = onFinalizeRevision,
                            colors = ButtonDefaults.buttonColors(containerColor = ColorStatusClosed),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("btn_finalize_revision")
                        ) {
                            Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Valider la révision & Re-clôturer", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Worker card in general monthly archive view.
 */
@Composable
private fun MonthlyWorkerCard(
    item: MonthlyWorkerArchiveItem,
    isDailyExpanded: Boolean,
    onToggleDaily: () -> Unit,
    onWorkerClicked: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, NabdBorder, RoundedCornerShape(14.dp))
            .testTag("worker_archive_card_${item.workerId}"),
        color = NabdSurfaceWhite,
        tonalElevation = 1.dp
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onWorkerClicked() },
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar Initials
                val initials = item.workerFullName.split(" ")
                    .filter { it.isNotBlank() }
                    .take(2)
                    .map { it.first().uppercase() }
                    .joinToString("")
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(NabdDeepViolet.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = initials.ifBlank { "OP" },
                        fontWeight = FontWeight.Bold,
                        color = NabdDeepViolet,
                        fontSize = 15.sp
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = item.workerFullName,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = NabdObsidian
                        )
                        if (!item.isActive) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                color = Color(0xFFECEFF1),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "Archivée",
                                    fontSize = 10.sp,
                                    color = Color(0xFF546E7A),
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                    Text(
                        text = "${item.workerRole} • Taux : ${item.appliedRateFormattedTnd}",
                        style = MaterialTheme.typography.bodySmall,
                        color = NabdMineralGray
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = item.grossPayFormattedTnd,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = NabdDeepViolet
                    )
                    Text(
                        text = item.totalHoursFormatted,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = NabdObsidian
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Voir chronologie",
                    tint = NabdMuted,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Sub-metrics row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(NabdSurfaceSubtle)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${item.workedDaysCount} jours travaillés",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = NabdObsidian
                )
                if (item.absentDaysCount > 0) {
                    Text(
                        text = "${item.absentDaysCount} absences",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                // Daily breakdown toggle
                Row(
                    modifier = Modifier
                        .clickable { onToggleDaily() }
                        .padding(2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isDailyExpanded) "Masquer détail" else "Détail journalier",
                        fontSize = 11.sp,
                        color = NabdPurple,
                        fontWeight = FontWeight.SemiBold
                    )
                    Icon(
                        imageVector = if (isDailyExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = NabdPurple,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            // Expanded Daily Records
            if (isDailyExpanded) {
                Spacer(modifier = Modifier.height(8.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White)
                        .border(1.dp, NabdBorder, RoundedCornerShape(8.dp))
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (item.dailyRecords.isEmpty()) {
                        Text("Aucun pointage journalier.", fontSize = 11.sp, color = NabdMuted)
                    } else {
                        item.dailyRecords.forEach { daily ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = daily.dayNameFrench,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = NabdObsidian
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = when (daily.status) {
                                            AttendanceStatus.WORKED -> daily.formattedHours
                                            AttendanceStatus.ABSENT -> "Absence"
                                            AttendanceStatus.REST_DAY -> "Repos"
                                            AttendanceStatus.BLANK -> "-"
                                        },
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = when (daily.status) {
                                            AttendanceStatus.WORKED -> NabdDeepViolet
                                            AttendanceStatus.ABSENT -> MaterialTheme.colorScheme.error
                                            AttendanceStatus.REST_DAY -> NabdMineralGray
                                            AttendanceStatus.BLANK -> NabdMuted
                                        }
                                    )
                                }
                            }
                            HorizontalDivider(color = NabdBorder.copy(alpha = 0.5f), thickness = 0.5.dp)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Worker Chronological Content view.
 */
@Composable
private fun WorkerChronologicalContent(
    worker: Worker,
    months: List<MonthlyWorkerArchiveItem>,
    expandedMonth: String?,
    onToggleExpandMonth: (String) -> Unit
) {
    val totalLifetimeMinutes = months.sumOf { it.totalMinutes }
    val totalLifetimeHours = "${totalLifetimeMinutes / 60}h ${(totalLifetimeMinutes % 60).toString().padStart(2, '0')}"
    val totalLifetimeDays = months.sumOf { it.workedDaysCount }
    val totalLifetimeGross = months.sumOf { it.basePayMillimes }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Historical Overview Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = NabdSurfaceWhite),
                border = androidx.compose.foundation.BorderStroke(1.dp, NabdBorder),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Récapitulatif de Carrière & Présence",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = NabdObsidian
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Mois Travaillés", fontSize = 11.sp, color = NabdMineralGray)
                            Text("${months.size} mois", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = NabdObsidian)
                        }
                        Column {
                            Text("Cumul Heures", fontSize = 11.sp, color = NabdMineralGray)
                            Text(totalLifetimeHours, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = NabdDeepViolet)
                        }
                        Column {
                            Text("Total Jours", fontSize = 11.sp, color = NabdMineralGray)
                            Text("$totalLifetimeDays j", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00796B))
                        }
                        Column {
                            Text("Gains Cumulés", fontSize = 11.sp, color = NabdMineralGray)
                            Text(com.example.domain.model.formatMillimesAsTnd(totalLifetimeGross), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE65100))
                        }
                    }
                }
            }
        }

        item {
            Text(
                text = "Historique Chronologique des Mois Travaillés (${months.size})",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = NabdObsidian
            )
        }

        if (months.isEmpty()) {
            item {
                EmptyStateCard(
                    title = "Aucun mois enregistré",
                    description = "Cette ouvrière n'a aucun historique d'heures ou de pointage effectif."
                )
            }
        } else {
            items(months, key = { it.yearMonth }) { monthItem ->
                ChronologicalMonthCard(
                    item = monthItem,
                    isExpanded = expandedMonth == monthItem.yearMonth,
                    onToggleExpand = { onToggleExpandMonth(monthItem.yearMonth) }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ChronologicalMonthCard(
    item: MonthlyWorkerArchiveItem,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit
) {
    val parts = item.yearMonth.split("-")
    val mNum = parts.getOrNull(1)?.toIntOrNull() ?: 1
    val yNum = parts.getOrNull(0) ?: "2026"
    val monthName = when (mNum) {
        1 -> "Janvier"; 2 -> "Février"; 3 -> "Mars"; 4 -> "Avril"
        5 -> "Mai"; 6 -> "Juin"; 7 -> "Juillet"; 8 -> "Août"
        9 -> "Septembre"; 10 -> "Octobre"; 11 -> "Novembre"; 12 -> "Décembre"
        else -> item.yearMonth
    }
    val titleFrench = "$monthName $yNum"

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, NabdBorder, RoundedCornerShape(14.dp))
            .testTag("chronology_card_${item.yearMonth}"),
        color = NabdSurfaceWhite,
        tonalElevation = 1.dp
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = null,
                        tint = NabdPurple,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = titleFrench,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = NabdObsidian
                    )
                }

                Text(
                    text = item.grossPayFormattedTnd,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = NabdDeepViolet
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(NabdSurfaceSubtle)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${item.totalHoursFormatted} • ${item.workedDaysCount} jours",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = NabdObsidian
                )
                Text(
                    text = "Taux : ${item.appliedRateFormattedTnd}",
                    fontSize = 11.sp,
                    color = NabdMineralGray
                )
                Row(
                    modifier = Modifier.clickable { onToggleExpand() },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isExpanded) "Masquer" else "Voir détail",
                        fontSize = 11.sp,
                        color = NabdPurple,
                        fontWeight = FontWeight.SemiBold
                    )
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = NabdPurple,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            if (isExpanded) {
                Spacer(modifier = Modifier.height(8.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White)
                        .border(1.dp, NabdBorder, RoundedCornerShape(8.dp))
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    item.dailyRecords.forEach { daily ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(daily.dayNameFrench, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = NabdObsidian)
                            Text(
                                text = when (daily.status) {
                                    AttendanceStatus.WORKED -> daily.formattedHours
                                    AttendanceStatus.ABSENT -> "Absence"
                                    AttendanceStatus.REST_DAY -> "Repos"
                                    AttendanceStatus.BLANK -> "-"
                                },
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = when (daily.status) {
                                    AttendanceStatus.WORKED -> NabdDeepViolet
                                    AttendanceStatus.ABSENT -> MaterialTheme.colorScheme.error
                                    AttendanceStatus.REST_DAY -> NabdMineralGray
                                    AttendanceStatus.BLANK -> NabdMuted
                                }
                            )
                        }
                        HorizontalDivider(color = NabdBorder.copy(alpha = 0.5f), thickness = 0.5.dp)
                    }
                }
            }
        }
    }
}

/**
 * Status Badge for archive month.
 */
@Composable
private fun StatusBadge(status: ArchiveMonthStatus) {
    val (bgColor, textColor, text) = when (status) {
        ArchiveMonthStatus.CLOTURE -> Triple(Color(0xFFE8F5E9), ColorStatusClosed, "CLÔTURÉ")
        ArchiveMonthStatus.EN_REVISION -> Triple(Color(0xFFFFF3E0), ColorStatusRevision, "EN RÉVISION")
        ArchiveMonthStatus.OUVERT -> Triple(NabdDeepViolet.copy(alpha = 0.1f), NabdDeepViolet, "OUVERT")
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(6.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = textColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        )
    }
}

/**
 * Empty state card component.
 */
@Composable
private fun EmptyStateCard(title: String, description: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(NabdSurfaceWhite)
            .border(1.dp, NabdBorder, RoundedCornerShape(16.dp))
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.Archive,
                contentDescription = null,
                tint = NabdMineralGray,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = NabdObsidian,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = NabdMineralGray,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Dialog to close/certify the month archive.
 */
@Composable
private fun CloseMonthDialog(
    yearMonth: String,
    displayName: String,
    onDismiss: () -> Unit,
    onConfirm: (notes: String) -> Unit
) {
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = NabdPurple,
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Text(
                text = "Clôturer le mois : $displayName",
                fontWeight = FontWeight.Bold,
                color = NabdObsidian
            )
        },
        text = {
            Column {
                Text(
                    text = "La clôture certifie l'ensemble des heures pointées et verrouille les calculs de paie pour ce mois.\n\nCette action est enregistrée au journal d'audit et consultable en traçabilité.",
                    style = MaterialTheme.typography.bodySmall,
                    color = NabdObsidian
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes ou visa de certification (optionnel)") },
                    placeholder = { Text("Ex: Clôture mensuelle validée après contrôle") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_closure_notes"),
                    shape = RoundedCornerShape(10.dp),
                    maxLines = 3
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(notes) },
                colors = ButtonDefaults.buttonColors(containerColor = NabdPurple),
                modifier = Modifier.testTag("btn_confirm_close_month")
            ) {
                Text("Confirmer la clôture")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler", color = NabdMuted)
            }
        }
    )
}

/**
 * Dialog to reopen closed month for revision.
 */
@Composable
private fun ReopenRevisionDialog(
    yearMonth: String,
    displayName: String,
    onDismiss: () -> Unit,
    onConfirm: (reason: String) -> Unit
) {
    var reason by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.LockOpen,
                contentDescription = null,
                tint = ColorStatusRevision,
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Text(
                text = "Déverrouiller pour Révision : $displayName",
                fontWeight = FontWeight.Bold,
                color = NabdObsidian
            )
        },
        text = {
            Column {
                Text(
                    text = "La réouverture d'un mois clôturé modifie le statut en « En Révision » et consigne un événement d'audit obligatoire avec le motif saisi.",
                    style = MaterialTheme.typography.bodySmall,
                    color = NabdObsidian
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = reason,
                    onValueChange = {
                        reason = it
                        if (it.isNotBlank()) isError = false
                    },
                    label = { Text("Motif de révision (obligatoire)") },
                    placeholder = { Text("Ex: Correction heures supplémentaires du 15 mai") },
                    isError = isError,
                    supportingText = if (isError) { { Text("Le motif est obligatoire pour la traçabilité.") } } else null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_revision_reason"),
                    shape = RoundedCornerShape(10.dp),
                    maxLines = 3
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (reason.isBlank()) {
                        isError = true
                    } else {
                        onConfirm(reason)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = ColorStatusRevision),
                modifier = Modifier.testTag("btn_confirm_reopen_revision")
            ) {
                Text("Autoriser la révision")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler", color = NabdMuted)
            }
        }
    )
}

/**
 * Dialog displaying audit trail for the selected archive month.
 */
@Composable
private fun AuditHistoryDialog(
    yearMonth: String,
    auditTrail: List<AuditEvent>,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.History,
                contentDescription = null,
                tint = NabdDeepViolet,
                modifier = Modifier.size(30.dp)
            )
        },
        title = {
            Text(
                text = "Traçabilité & Audit : $yearMonth",
                fontWeight = FontWeight.Bold,
                color = NabdObsidian
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (auditTrail.isEmpty()) {
                    Text(
                        text = "Aucun événement d'audit spécifique pour ce mois.",
                        style = MaterialTheme.typography.bodySmall,
                        color = NabdMuted
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(auditTrail, key = { it.id }) { event ->
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                color = NabdSurfaceSubtle,
                                border = androidx.compose.foundation.BorderStroke(0.5.dp, NabdBorder)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = event.actionType.labelFr,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = NabdDeepViolet
                                        )
                                        Text(
                                            text = event.actorRole.labelFrench,
                                            fontSize = 10.sp,
                                            color = NabdPurple,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = event.detailsFr,
                                        fontSize = 11.sp,
                                        color = NabdObsidian,
                                        lineHeight = 15.sp
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "${event.formattedDateTimeTunis} • Par ${event.actorName}",
                                        fontSize = 10.sp,
                                        color = NabdMineralGray
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = NabdDeepViolet)
            ) {
                Text("Fermer")
            }
        }
    )
}

/**
 * Legacy fallback when ViewModel is not passed.
 */
@Composable
private fun LegacyArchivesFallback(
    workerFilter: Worker?,
    workedMonths: List<WorkedMonthSummary>,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (workerFilter != null) {
        BackHandler(onBack = onBack)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NabdBone)
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (workerFilter != null) {
                IconButton(onClick = onBack) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                }
            }
            Text(
                text = if (workerFilter != null) "Archives : ${workerFilter.fullName}" else "Archives d'atelier",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = NabdObsidian
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        if (workedMonths.isEmpty()) {
            EmptyStateCard("Aucun mois de travail effectif", "Les mois sans pointage ne figurent pas dans cette archive.")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(workedMonths, key = { it.yearMonth }) { month ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = NabdSurfaceWhite,
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, NabdBorder)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(month.displayNameFrench, fontWeight = FontWeight.Bold)
                            Text(month.grossPayFormattedTnd, color = NabdDeepViolet, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
