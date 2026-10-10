package com.example.ui.attendance

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
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.AtelierDate
import com.example.domain.model.AttendanceEntry
import com.example.domain.model.AttendanceStatus
import com.example.domain.model.AttendanceViewMode
import com.example.domain.model.Worker
import com.example.ui.theme.NabdBorder
import com.example.ui.theme.NabdBone
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
import com.example.ui.theme.NabdRestBackground
import com.example.ui.theme.NabdRestBorder
import com.example.ui.theme.NabdRestText
import com.example.ui.theme.NabdSurfaceSubtle
import com.example.ui.theme.NabdSurfaceWhite

/**
 * Section 1: Pointage / Pointage du jour.
 * Specifications per 06_pointage_scope.md:
 * - View controls: Jour, Semaine, Mois.
 * - Active operators only (filtered by effective employment period on displayed date).
 * - Distinct states: UNRECORDED (À renseigner), WORKED (e.g. 7 h 30), ABSENT (AB), REST_DAY (Jour de repos).
 * - Sunday rule: every Sunday in Africa/Tunis is automatically REST_DAY.
 * - Tomorrow: locked empty preview, dates after tomorrow unreachable.
 * - Strictly NO total-hours footer on daily screen.
 * - Honest local storage indicator: Enregistré sur cet appareil • Non synchronisé.
 */
@Composable
fun DailyAttendanceScreen(
    state: AttendanceUiState,
    onPreviousDay: () -> Unit,
    onNextDay: () -> Unit,
    onToday: () -> Unit,
    onViewModeSelected: (AttendanceViewMode) -> Unit,
    onWorkerClicked: (Worker) -> Unit,
    onWorkerClickedForDate: (Worker, AtelierDate) -> Unit,
    onConfirmEffacer: () -> Unit,
    onDismissEffacer: () -> Unit,
    onClearError: () -> Unit,
    onOpenSampleNotice: () -> Unit,
    onNavigateToTeam: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeWorkers = state.activeWorkersForDate

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NabdBone)
            .padding(horizontal = 16.dp)
            .testTag("daily_attendance_screen")
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // Local Storage & Honest Sync Status Banner
        LocalStorageSyncBanner(
            isLoading = state.isLoading,
            onOpenNotice = onOpenSampleNotice
        )

        // Error Banner if save failed
        if (state.saveErrorMessage != null) {
            Spacer(modifier = Modifier.height(8.dp))
            SaveErrorBanner(
                message = state.saveErrorMessage,
                onDismiss = onClearError
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Date Context & Navigation Bar
        DateNavigationBar(
            dateFormatted = state.currentDate.formattedFrench,
            isToday = state.isViewingToday,
            isTomorrow = state.isViewingTomorrow,
            canGoForward = state.canGoForward,
            onPrevious = onPreviousDay,
            onNext = onNextDay,
            onToday = onToday
        )

        Spacer(modifier = Modifier.height(10.dp))

        // View Mode Selector: Jour | Semaine | Mois
        ViewModeSelector(
            currentMode = state.viewMode,
            onSelectMode = onViewModeSelected
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Notice: Tomorrow Locked (Demain — aperçu verrouillé)
        if (state.isViewingTomorrow && state.viewMode == AttendanceViewMode.JOUR) {
            LockedTomorrowBanner()
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Notice: Past Date (Lecture seule / Enregistrement archivé)
        if (!state.isViewingToday && !state.isViewingTomorrow && state.viewMode == AttendanceViewMode.JOUR) {
            PastDateBanner()
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Notice: Sunday Rest Day (Automatique obligatoire)
        if (state.currentDate.isSunday && state.viewMode == AttendanceViewMode.JOUR) {
            SundayNoticeBanner()
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Section Title
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Opératrices actives (${activeWorkers.size})",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = NabdObsidian
            )
            Text(
                text = "Africa/Tunis",
                style = MaterialTheme.typography.labelSmall,
                color = NabdMineralGray
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (activeWorkers.isEmpty()) {
            // Honest Empty State: Aucune opératrice active
            EmptyActiveWorkersState(onNavigateToTeam = onNavigateToTeam)
        } else {
            when (state.viewMode) {
                AttendanceViewMode.JOUR -> {
                    // 1. Day View (Jour)
                    // Strictly NO total-hours footer!
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("workers_attendance_list"),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(activeWorkers, key = { it.id }) { worker ->
                            val entry = state.attendanceEntries[worker.id]
                            WorkerAttendanceCard(
                                worker = worker,
                                entry = entry,
                                isLocked = state.isTomorrowLocked,
                                isSunday = state.currentDate.isSunday,
                                onClick = { onWorkerClicked(worker) }
                            )
                        }

                        item {
                            Spacer(modifier = Modifier.height(28.dp))
                        }
                    }
                }

                AttendanceViewMode.SEMAINE -> {
                    // 2. Week View (Semaine)
                    WeekAttendanceView(
                        state = state,
                        workers = activeWorkers,
                        onCellClicked = onWorkerClickedForDate
                    )
                }

                AttendanceViewMode.MOIS -> {
                    // 3. Month View (Mois)
                    MonthAttendanceView(
                        state = state,
                        workers = activeWorkers,
                        onCellClicked = onWorkerClickedForDate
                    )
                }
            }
        }
    }

    // Confirmation dialog for Effacer
    if (state.showEffacerConfirmDialog) {
        AlertDialog(
            onDismissRequest = onDismissEffacer,
            title = {
                Text(
                    text = "Confirmer l'effacement",
                    fontWeight = FontWeight.Bold,
                    color = NabdObsidian
                )
            },
            text = {
                Text(
                    text = "Remettre cette case en état « À renseigner » ? Cette action efface la saisie enregistrée sur cet appareil.",
                    color = NabdObsidian
                )
            },
            confirmButton = {
                Button(
                    onClick = onConfirmEffacer,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NabdCoralText,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(text = "Effacer")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = onDismissEffacer,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(text = "Annuler")
                }
            },
            containerColor = NabdSurfaceWhite,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

/**
 * Visual card for each active worker in the daily attendance list.
 */
@Composable
private fun WorkerAttendanceCard(
    worker: Worker,
    entry: AttendanceEntry?,
    isLocked: Boolean,
    isSunday: Boolean,
    onClick: () -> Unit
) {
    val status = if (isSunday) {
        AttendanceStatus.REST_DAY
    } else {
        entry?.status ?: AttendanceStatus.BLANK
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, NabdBorder, RoundedCornerShape(14.dp))
            .clickable(enabled = !isLocked && !isSunday, onClick = onClick)
            .testTag("worker_card_${worker.id}"),
        color = NabdSurfaceWhite,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Worker Initials & Identification (Leading column)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(NabdPurple.copy(alpha = 0.1f))
                        .border(1.dp, NabdPurple.copy(alpha = 0.2f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = worker.initials,
                        fontWeight = FontWeight.Bold,
                        color = NabdPurple,
                        fontSize = 15.sp
                    )
                }

                Column {
                    Text(
                        text = worker.fullName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = NabdObsidian
                    )
                    Text(
                        text = worker.role,
                        style = MaterialTheme.typography.bodySmall,
                        color = NabdMineralGray
                    )
                }
            }

            // Attendance Status Badge
            AttendanceStatusBadge(
                status = status,
                entry = entry,
                isLocked = isLocked
            )
        }
    }
}

/**
 * Explicit visual badges for the 4 distinct attendance states:
 * 1. WORKED: Mint surface, dark green text + Schedule icon (e.g. "7 h 30")
 * 2. ABSENT: Coral surface, dark rust text + EventBusy icon ("AB")
 * 3. REST_DAY: Lavender surface, violet text + Bedtime icon ("Jour de repos")
 * 4. BLANK: Soft subtle surface, dark text + Edit icon ("À renseigner")
 * Also handles Locked tomorrow: ("Demain — verrouillé")
 */
@Composable
fun AttendanceStatusBadge(
    status: AttendanceStatus,
    entry: AttendanceEntry?,
    isLocked: Boolean,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor, borderColor, icon, label) = when {
        isLocked -> StatusVisuals(
            NabdSurfaceSubtle,
            NabdMineralGray,
            NabdBorder,
            Icons.Default.Lock,
            "Verrouillé"
        )
        status == AttendanceStatus.WORKED -> StatusVisuals(
            NabdMintBackground,
            NabdMintText,
            NabdMintBorder,
            Icons.Default.Schedule,
            entry?.durationFormatted ?: "0 h"
        )
        status == AttendanceStatus.ABSENT -> StatusVisuals(
            NabdCoralBackground,
            NabdCoralText,
            NabdCoralBorder,
            Icons.Default.EventBusy,
            "AB"
        )
        status == AttendanceStatus.REST_DAY -> StatusVisuals(
            NabdRestBackground,
            NabdRestText,
            NabdRestBorder,
            Icons.Default.Bedtime,
            "Jour de repos"
        )
        else -> StatusVisuals(
            NabdSurfaceSubtle,
            NabdObsidian,
            NabdBorder,
            Icons.Default.Edit,
            "À renseigner"
        )
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 7.dp)
            .testTag("status_badge_${status.name.lowercase()}"),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = label,
                color = textColor,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
        }
    }
}

private data class StatusVisuals(
    val bgColor: Color,
    val textColor: Color,
    val borderColor: Color,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val label: String
)

/**
 * View Mode Selector: Jour | Semaine | Mois
 */
@Composable
private fun ViewModeSelector(
    currentMode: AttendanceViewMode,
    onSelectMode: (AttendanceViewMode) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, NabdBorder, RoundedCornerShape(12.dp)),
        color = NabdSurfaceWhite
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            AttendanceViewMode.values().forEach { mode ->
                val isSelected = (mode == currentMode)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) NabdPurple else Color.Transparent)
                        .clickable { onSelectMode(mode) }
                        .padding(vertical = 8.dp)
                        .testTag("tab_view_mode_${mode.name.lowercase()}"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = mode.titleFrench,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) Color.White else NabdObsidian,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

/**
 * 2. Week Attendance View (Semaine).
 * Reads the same attendance repository entries for the 7 days of the week.
 * Keeps worker names pinned/leading on the left while scrolling days horizontally.
 */
@Composable
private fun WeekAttendanceView(
    state: AttendanceUiState,
    workers: List<Worker>,
    onCellClicked: (Worker, AtelierDate) -> Unit
) {
    val weekDays = state.currentDate.getWeekDays()
    val scrollState = rememberScrollState()

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, NabdBorder, RoundedCornerShape(14.dp))
            .testTag("week_attendance_view"),
        color = NabdSurfaceWhite
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            // Days Header Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(scrollState),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Leading fixed worker header
                Box(
                    modifier = Modifier
                        .width(130.dp)
                        .padding(vertical = 8.dp)
                ) {
                    Text(
                        text = "Opératrice",
                        fontWeight = FontWeight.Bold,
                        color = NabdObsidian,
                        fontSize = 13.sp
                    )
                }

                // Columns for each day of week
                weekDays.forEach { day ->
                    val isToday = (day == AtelierDate.todayTunis())
                    val isSunday = day.isSunday
                    Box(
                        modifier = Modifier
                            .width(88.dp)
                            .padding(horizontal = 4.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${day.shortDayOfWeekNameFrench} ${day.day}",
                                fontWeight = if (isToday) FontWeight.Bold else FontWeight.SemiBold,
                                color = if (isToday) NabdPurple else NabdObsidian,
                                fontSize = 12.sp
                            )
                            if (isSunday) {
                                Text(
                                    text = "Repos",
                                    fontSize = 10.sp,
                                    color = NabdRestText
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Rows for each active worker
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(workers, key = { it.id }) { worker ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(scrollState),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Sticky leading Worker Name
                        Box(
                            modifier = Modifier
                                .width(130.dp)
                                .padding(vertical = 6.dp)
                        ) {
                            Column {
                                Text(
                                    text = worker.fullName,
                                    fontWeight = FontWeight.SemiBold,
                                    color = NabdObsidian,
                                    fontSize = 13.sp,
                                    maxLines = 1
                                )
                                Text(
                                    text = worker.role,
                                    color = NabdMineralGray,
                                    fontSize = 11.sp,
                                    maxLines = 1
                                )
                            }
                        }

                        // Day cells
                        weekDays.forEach { day ->
                            val key = "${worker.id}_${day.isoString}"
                            val entry = state.monthAttendanceEntries[key]
                            val isSunday = day.isSunday
                            val isTomorrow = (day == AtelierDate.todayTunis().nextDay())
                            val isFuture = (day > AtelierDate.todayTunis().nextDay())

                            val status = when {
                                isSunday -> AttendanceStatus.REST_DAY
                                isFuture || isTomorrow -> AttendanceStatus.BLANK
                                else -> entry?.status ?: AttendanceStatus.BLANK
                            }

                            val isCellLocked = isFuture || isTomorrow || isSunday

                            Box(
                                modifier = Modifier
                                    .width(88.dp)
                                    .padding(horizontal = 4.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        when {
                                            isCellLocked -> NabdSurfaceSubtle
                                            status == AttendanceStatus.WORKED -> NabdMintBackground
                                            status == AttendanceStatus.ABSENT -> NabdCoralBackground
                                            status == AttendanceStatus.REST_DAY -> NabdRestBackground
                                            else -> NabdSurfaceSubtle
                                        }
                                    )
                                    .border(
                                        1.dp,
                                        when {
                                            isCellLocked -> NabdBorder
                                            status == AttendanceStatus.WORKED -> NabdMintBorder
                                            status == AttendanceStatus.ABSENT -> NabdCoralBorder
                                            status == AttendanceStatus.REST_DAY -> NabdRestBorder
                                            else -> NabdBorder
                                        },
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable(enabled = !isCellLocked) {
                                        onCellClicked(worker, day)
                                    }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = when {
                                        isSunday -> "Repos"
                                        isFuture || isTomorrow -> "—"
                                        status == AttendanceStatus.WORKED -> entry?.durationFormatted ?: "0 h"
                                        status == AttendanceStatus.ABSENT -> "AB"
                                        status == AttendanceStatus.REST_DAY -> "Repos"
                                        else -> "À renseigner"
                                    },
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = when {
                                        isSunday -> NabdRestText
                                        isCellLocked -> NabdMineralGray
                                        status == AttendanceStatus.WORKED -> NabdMintText
                                        status == AttendanceStatus.ABSENT -> NabdCoralText
                                        status == AttendanceStatus.REST_DAY -> NabdRestText
                                        else -> NabdObsidian
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 3. Month Attendance View (Mois).
 * Reads the same attendance repository entries for all days in currentDate's month.
 * Worker name remains leading/fixed while scrolling days horizontally.
 */
@Composable
private fun MonthAttendanceView(
    state: AttendanceUiState,
    workers: List<Worker>,
    onCellClicked: (Worker, AtelierDate) -> Unit
) {
    val monthDays = state.currentDate.getMonthDays()
    val scrollState = rememberScrollState()

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, NabdBorder, RoundedCornerShape(14.dp))
            .testTag("month_attendance_view"),
        color = NabdSurfaceWhite
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            // Header Row: Days 1..N
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(scrollState),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Leading fixed worker header
                Box(
                    modifier = Modifier
                        .width(130.dp)
                        .padding(vertical = 8.dp)
                ) {
                    Text(
                        text = "Opératrice",
                        fontWeight = FontWeight.Bold,
                        color = NabdObsidian,
                        fontSize = 13.sp
                    )
                }

                monthDays.forEach { day ->
                    val isToday = (day == AtelierDate.todayTunis())
                    val isSunday = day.isSunday
                    Box(
                        modifier = Modifier
                            .width(62.dp)
                            .padding(horizontal = 2.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${day.day}",
                                fontWeight = if (isToday) FontWeight.Bold else FontWeight.SemiBold,
                                color = if (isToday) NabdPurple else if (isSunday) NabdRestText else NabdObsidian,
                                fontSize = 12.sp
                            )
                            Text(
                                text = day.shortDayOfWeekNameFrench.take(1),
                                fontSize = 10.sp,
                                color = if (isSunday) NabdRestText else NabdMineralGray
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Rows for each active worker
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(workers, key = { it.id }) { worker ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(scrollState),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Leading Worker Name
                        Box(
                            modifier = Modifier
                                .width(130.dp)
                                .padding(vertical = 6.dp)
                        ) {
                            Text(
                                text = worker.fullName,
                                fontWeight = FontWeight.SemiBold,
                                color = NabdObsidian,
                                fontSize = 12.sp,
                                maxLines = 1
                            )
                        }

                        // Day cells
                        monthDays.forEach { day ->
                            val key = "${worker.id}_${day.isoString}"
                            val entry = state.monthAttendanceEntries[key]
                            val isSunday = day.isSunday
                            val isTomorrow = (day == AtelierDate.todayTunis().nextDay())
                            val isFuture = (day > AtelierDate.todayTunis().nextDay())

                            val status = when {
                                isSunday -> AttendanceStatus.REST_DAY
                                isFuture || isTomorrow -> AttendanceStatus.BLANK
                                else -> entry?.status ?: AttendanceStatus.BLANK
                            }

                            val isCellLocked = isFuture || isTomorrow || isSunday

                            Box(
                                modifier = Modifier
                                    .width(62.dp)
                                    .padding(horizontal = 2.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(
                                        when {
                                            isCellLocked -> NabdSurfaceSubtle
                                            status == AttendanceStatus.WORKED -> NabdMintBackground
                                            status == AttendanceStatus.ABSENT -> NabdCoralBackground
                                            status == AttendanceStatus.REST_DAY -> NabdRestBackground
                                            else -> NabdSurfaceSubtle
                                        }
                                    )
                                    .border(
                                        1.dp,
                                        when {
                                            isCellLocked -> NabdBorder
                                            status == AttendanceStatus.WORKED -> NabdMintBorder
                                            status == AttendanceStatus.ABSENT -> NabdCoralBorder
                                            status == AttendanceStatus.REST_DAY -> NabdRestBorder
                                            else -> NabdBorder
                                        },
                                        RoundedCornerShape(6.dp)
                                    )
                                    .clickable(enabled = !isCellLocked) {
                                        onCellClicked(worker, day)
                                    }
                                    .padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = when {
                                        isSunday -> "R"
                                        isFuture || isTomorrow -> "—"
                                        status == AttendanceStatus.WORKED -> entry?.durationFormatted ?: "0 h"
                                        status == AttendanceStatus.ABSENT -> "AB"
                                        status == AttendanceStatus.REST_DAY -> "R"
                                        else -> "—"
                                    },
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = when {
                                        isSunday -> NabdRestText
                                        isCellLocked -> NabdMineralGray
                                        status == AttendanceStatus.WORKED -> NabdMintText
                                        status == AttendanceStatus.ABSENT -> NabdCoralText
                                        status == AttendanceStatus.REST_DAY -> NabdRestText
                                        else -> NabdMineralGray
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Honest Local Storage and Sync Status Banner.
 */
@Composable
private fun LocalStorageSyncBanner(
    isLoading: Boolean,
    onOpenNotice: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(NabdDeepViolet.copy(alpha = 0.07f))
            .border(1.dp, NabdDeepViolet.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
            .clickable(onClick = onOpenNotice)
            .padding(horizontal = 12.dp, vertical = 7.dp)
            .testTag("local_storage_banner")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(12.dp),
                        strokeWidth = 2.dp,
                        color = NabdPurple
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = NabdDeepViolet,
                        modifier = Modifier.size(14.dp)
                    )
                }
                Text(
                    text = "Enregistré sur cet appareil • Non synchronisé",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = NabdDeepViolet
                )
            }
            Text(
                text = "État ›",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = NabdPurple
            )
        }
    }
}

/**
 * Save Error Banner
 */
@Composable
private fun SaveErrorBanner(
    message: String,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(NabdCoralBackground)
            .border(1.dp, NabdCoralBorder, RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .testTag("save_error_banner")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.ErrorOutline,
                    contentDescription = null,
                    tint = NabdCoralText,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = message,
                    color = NabdCoralText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Fermer",
                    tint = NabdCoralText,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

/**
 * Date Navigation Bar
 */
@Composable
private fun DateNavigationBar(
    dateFormatted: String,
    isToday: Boolean,
    isTomorrow: Boolean,
    canGoForward: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onToday: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, NabdBorder, RoundedCornerShape(14.dp)),
        color = NabdSurfaceWhite,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = onPrevious,
                modifier = Modifier.testTag("btn_prev_day")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Jour précédent",
                    tint = NabdObsidian
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = dateFormatted,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = NabdObsidian
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    when {
                        isToday -> {
                            Text(
                                text = "Aujourd'hui",
                                style = MaterialTheme.typography.labelSmall,
                                color = NabdPurple,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        isTomorrow -> {
                            Text(
                                text = "Demain — aperçu verrouillé",
                                style = MaterialTheme.typography.labelSmall,
                                color = NabdCoralText,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        else -> {
                            Text(
                                text = "Enregistré",
                                style = MaterialTheme.typography.labelSmall,
                                color = NabdMineralGray
                            )
                        }
                    }

                    if (!isToday) {
                        Text(
                            text = "• Revenir à aujourd'hui",
                            style = MaterialTheme.typography.labelSmall,
                            color = NabdPurple,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clickable(onClick = onToday)
                                .testTag("btn_jump_today")
                        )
                    }
                }
            }

            IconButton(
                onClick = onNext,
                enabled = canGoForward,
                modifier = Modifier.testTag("btn_next_day")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Jour suivant",
                    tint = if (canGoForward) NabdObsidian else NabdMineralGray.copy(alpha = 0.3f)
                )
            }
        }
    }
}

@Composable
private fun LockedTomorrowBanner() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(NabdDeepViolet.copy(alpha = 0.08f))
            .border(1.dp, NabdDeepViolet.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
            .padding(12.dp)
            .testTag("tomorrow_locked_banner")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = NabdDeepViolet,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = "Demain — aperçu verrouillé. La saisie anticipée n'est pas autorisée.",
                style = MaterialTheme.typography.bodySmall,
                color = NabdDeepViolet,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun PastDateBanner() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(NabdSurfaceSubtle)
            .border(1.dp, NabdBorder, RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .testTag("past_date_banner")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Default.History,
                contentDescription = null,
                tint = NabdMineralGray,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = "Historique enregistré sur cet appareil",
                fontSize = 11.sp,
                color = NabdMineralGray,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun SundayNoticeBanner() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(NabdRestBackground)
            .border(1.dp, NabdRestBorder, RoundedCornerShape(12.dp))
            .padding(12.dp)
            .testTag("sunday_notice_banner")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Bedtime,
                contentDescription = null,
                tint = NabdRestText,
                modifier = Modifier.size(20.dp)
            )
            Column {
                Text(
                    text = "Dimanche — Jour de repos hebdomadaire automatique",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = NabdRestText
                )
                Text(
                    text = "Toutes les opératrices sont automatiquement en Jour de repos (Africa/Tunis). Ni AB ni manque de saisie.",
                    style = MaterialTheme.typography.bodySmall,
                    color = NabdRestText.copy(alpha = 0.85f)
                )
            }
        }
    }
}

@Composable
private fun EmptyActiveWorkersState(onNavigateToTeam: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 24.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(NabdSurfaceWhite)
            .border(1.dp, NabdBorder, RoundedCornerShape(16.dp))
            .padding(28.dp)
            .testTag("attendance_empty_state"),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = NabdMineralGray,
                modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Aucune opératrice active.",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = NabdObsidian
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Pour démarrer le pointage quotidien, enregistrez d'abord les membres de l'équipe dans l'onglet Équipe.",
                style = MaterialTheme.typography.bodyMedium,
                color = NabdMineralGray
            )
            Spacer(modifier = Modifier.height(18.dp))
            Button(
                onClick = onNavigateToTeam,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = NabdPurple,
                    contentColor = Color.White
                ),
                modifier = Modifier.testTag("btn_attendance_go_team")
            ) {
                Text(text = "Ouvrir l'onglet Équipe")
            }
        }
    }
}
