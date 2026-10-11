package com.example.ui.attendance

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.domain.model.AttendanceEntry
import com.example.domain.model.AttendanceStatus
import com.example.domain.model.Worker
import com.example.ui.theme.NabdBorder
import com.example.ui.theme.NabdCoralBackground
import com.example.ui.theme.NabdCoralBorder
import com.example.ui.theme.NabdCoralText
import com.example.ui.theme.NabdMineralGray
import com.example.ui.theme.NabdObsidian
import com.example.ui.theme.NabdPurple
import com.example.ui.theme.NabdSurfaceSubtle
import com.example.ui.theme.NabdSurfaceWhite
import kotlinx.coroutines.launch

/**
 * Section 1: Pointage — Duration and Attendance Entry Dialog (Pointage v2).
 * Adheres strictly to Pointage v2 requirements:
 * 1. Vertical hour wheel: Snapping, values 1 through 12 only (no hour 0).
 *    Centered value is selected. Neighboring values have lower emphasis.
 * 2. Minutes: exactly two choices: "00 min" and "30 min".
 *    Stored deterministically as integer minutes (hours * 60 + minutes).
 *    Never save 0 h 00 as WORKED.
 * 3. Compact task dialog: Worker identity, date/role, duration display,
 *    vertical wheel, 00/30 controls, AB action, and primary save action.
 *    Manual "Jour de repos" and "Effacer la saisie" removed.
 * 4. Save and move to next worker:
 *    Real repository save finishes before navigation.
 *    Primary action is "Enregistrer et suivante".
 *    Physical swipe left/right between workers supported.
 *    Unsaved changes confirmation before navigating.
 *    Clear French completion state on the last worker.
 */
@Composable
fun TimeEntryWheelDialog(
    worker: Worker,
    currentEntry: AttendanceEntry?,
    dateFrench: String,
    onDismiss: () -> Unit,
    onSaveWorkedHours: (hours: Int, minutes: Int) -> Unit,
    onSetAbsent: () -> Unit,
    onSetRestDay: () -> Unit = {},
    onResetToBlank: () -> Unit = {}
) {
    val viewModel = AttendanceViewModel.currentInstance
    val uiState = viewModel?.uiState?.collectAsState()?.value

    // Initial values derived from stored WORKED record or defaults (7h 00)
    val initialHours = if (currentEntry?.status == AttendanceStatus.WORKED) {
        currentEntry.hours.coerceIn(1, 12)
    } else {
        7
    }
    val initialMinutes = if (currentEntry?.status == AttendanceStatus.WORKED) {
        if (currentEntry.minutes >= 30) 30 else 0
    } else {
        0
    }

    var selectedHours by remember(worker.id) { mutableIntStateOf(initialHours) }
    var selectedMinutes by remember(worker.id) { mutableIntStateOf(initialMinutes) }

    // Check unsaved changes relative to stored entry
    val hasUnsavedChanges = remember(worker.id, selectedHours, selectedMinutes, currentEntry) {
        if (currentEntry?.status == AttendanceStatus.WORKED) {
            selectedHours != currentEntry.hours || selectedMinutes != currentEntry.minutes
        } else {
            // Worker is currently BLANK or ABSENT, but user configured hours/minutes
            true
        }
    }

    var showUnsavedPrompt by remember { mutableStateOf(false) }
    var pendingNavigationDirection by remember { mutableStateOf<NavigationDirection?>(null) }

    // Determine position in Pointage list
    val orderedWorkers = uiState?.pointageOrderedWorkers ?: emptyList()
    val currentIndex = orderedWorkers.indexOfFirst { it.id == worker.id }
    val hasPrevious = currentIndex > 0
    val hasNext = currentIndex != -1 && currentIndex < orderedWorkers.size - 1
    val isLastWorker = (currentIndex != -1 && currentIndex == orderedWorkers.size - 1)

    val isLoading = uiState?.isLoading ?: false
    val errorMessage = uiState?.saveErrorMessage

    fun executeNavigation(direction: NavigationDirection) {
        when (direction) {
            NavigationDirection.NEXT -> {
                viewModel?.navigateNextWorker()
            }
            NavigationDirection.PREVIOUS -> {
                viewModel?.navigatePreviousWorker()
            }
        }
    }

    fun requestNavigation(direction: NavigationDirection) {
        if (hasUnsavedChanges) {
            pendingNavigationDirection = direction
            showUnsavedPrompt = true
        } else {
            executeNavigation(direction)
        }
    }

    // Horizontal swipe gesture tracking
    var totalDragX by remember { mutableFloatStateOf(0f) }

    Dialog(onDismissRequest = {
        if (hasUnsavedChanges) {
            showUnsavedPrompt = true
            pendingNavigationDirection = null
        } else {
            onDismiss()
        }
    }) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .pointerInput(worker.id) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            if (totalDragX < -80f && hasNext) {
                                requestNavigation(NavigationDirection.NEXT)
                            } else if (totalDragX > 80f && hasPrevious) {
                                requestNavigation(NavigationDirection.PREVIOUS)
                            }
                            totalDragX = 0f
                        },
                        onDragCancel = {
                            totalDragX = 0f
                        },
                        onHorizontalDrag = { _, dragAmount ->
                            totalDragX += dragAmount
                        }
                    )
                }
                .testTag("time_entry_dialog"),
            color = NabdSurfaceWhite,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header with Worker identity and Navigation / Close icons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(NabdPurple.copy(alpha = 0.12f))
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
                                fontWeight = FontWeight.Bold,
                                color = NabdObsidian,
                                maxLines = 1
                            )
                            Text(
                                text = "${worker.role} • $dateFrench",
                                style = MaterialTheme.typography.bodySmall,
                                color = NabdMineralGray,
                                maxLines = 1
                            )
                        }
                    }

                    // Navigation & Dismiss controls
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (hasPrevious) {
                            IconButton(
                                onClick = { requestNavigation(NavigationDirection.PREVIOUS) },
                                modifier = Modifier
                                    .size(36.dp)
                                    .testTag("dialog_btn_previous_worker")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Opératrice précédente",
                                    tint = NabdObsidian,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        if (hasNext) {
                            IconButton(
                                onClick = { requestNavigation(NavigationDirection.NEXT) },
                                modifier = Modifier
                                    .size(36.dp)
                                    .testTag("dialog_btn_next_worker")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = "Opératrice suivante",
                                    tint = NabdObsidian,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("dialog_close_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Fermer le dialogue",
                                tint = NabdObsidian,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Digital Duration Display: e.g. "7 h 30" (450 min)
                val totalMinutes = (selectedHours * 60) + selectedMinutes
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(NabdSurfaceSubtle)
                        .border(1.dp, NabdBorder, RoundedCornerShape(12.dp))
                        .padding(vertical = 10.dp, horizontal = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = NabdPurple,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${selectedHours} h ${if (selectedMinutes == 0) "00" else "30"}",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = NabdObsidian
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "($totalMinutes min)",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = NabdMineralGray
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Requirement 1: Vertical Hour Wheel (1 through 12, snapping, centered value)
                Text(
                    text = "Heure",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = NabdObsidian
                )
                Spacer(modifier = Modifier.height(4.dp))

                VerticalHourWheel(
                    selectedHour = selectedHours,
                    onHourSelected = { hour -> selectedHours = hour }
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Requirement 2: Exactly two minute choices (00 min and 30 min)
                Text(
                    text = "Minutes",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = NabdObsidian
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    listOf(0 to "00 min", 30 to "30 min").forEach { (m, label) ->
                        val isSelected = (selectedMinutes == m)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) NabdPurple else NabdSurfaceSubtle)
                                .border(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) NabdPurple else NabdBorder,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .clickable { selectedMinutes = m }
                                .semantics {
                                    contentDescription = "$label ${if (isSelected) "sélectionné" else ""}"
                                }
                                .testTag("minute_choice_$m"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                color = if (isSelected) Color.White else NabdObsidian,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }

                // Error message banner if previous save failed
                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(NabdCoralBackground)
                            .border(1.dp, NabdCoralBorder, RoundedCornerShape(10.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                            .testTag("dialog_error_banner")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ErrorOutline,
                                contentDescription = null,
                                tint = NabdCoralText,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = errorMessage,
                                color = NabdCoralText,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Requirement 4: Primary save action "Enregistrer et suivante"
                Button(
                    onClick = {
                        onSaveWorkedHours(selectedHours, selectedMinutes)
                    },
                    enabled = !isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .testTag("save_worked_hours_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NabdPurple,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        text = if (isLastWorker) {
                            "Enregistrer (Dernière)"
                        } else {
                            "Enregistrer et suivante"
                        },
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Requirement 3: AB action (Absence constatée)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(NabdCoralBackground)
                        .border(1.dp, NabdCoralBorder, RoundedCornerShape(12.dp))
                        .clickable(enabled = !isLoading) { onSetAbsent() }
                        .semantics {
                            contentDescription = "Marquer absence constatée AB pour cette opératrice"
                        }
                        .testTag("action_mark_ab"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "AB — Absence constatée",
                        fontWeight = FontWeight.Bold,
                        color = NabdCoralText,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }

    // Confirmation modal when trying to navigate with unsaved changes
    if (showUnsavedPrompt) {
        AlertDialog(
            onDismissRequest = {
                showUnsavedPrompt = false
                pendingNavigationDirection = null
            },
            title = {
                Text(
                    text = "Modifications non enregistrées",
                    fontWeight = FontWeight.Bold,
                    color = NabdObsidian,
                    fontSize = 16.sp
                )
            },
            text = {
                Text(
                    text = "Voulez-vous enregistrer le pointage (${selectedHours} h ${if (selectedMinutes == 0) "00" else "30"}) pour ${worker.fullName} avant de continuer ?",
                    color = NabdObsidian,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showUnsavedPrompt = false
                        onSaveWorkedHours(selectedHours, selectedMinutes)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NabdPurple,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Enregistrer et continuer")
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            showUnsavedPrompt = false
                            val dir = pendingNavigationDirection
                            pendingNavigationDirection = null
                            if (dir != null) {
                                executeNavigation(dir)
                            } else {
                                onDismiss()
                            }
                        },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Ignorer")
                    }
                }
            },
            containerColor = NabdSurfaceWhite,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

private enum class NavigationDirection {
    PREVIOUS,
    NEXT
}

/**
 * Requirement 1: Real vertically scrollable snapping wheel.
 * Valid work hours are exactly 1 through 12. No hour 0.
 * Centered value is the selected value. Snaps to integer.
 * Neighboring values have lower visual emphasis.
 */
@Composable
fun VerticalHourWheel(
    selectedHour: Int,
    onHourSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val hours = remember { (1..12).toList() }
    val itemHeight = 44.dp
    val density = LocalDensity.current

    // Initial list state positioned so selectedHour is at the top of the visible window
    val initialIndex = (selectedHour - 1).coerceIn(0, 11)
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialIndex)
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)
    val coroutineScope = rememberCoroutineScope()

    // Determine the hour closest to the center line of the wheel
    val currentCenteredHour by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val visibleItems = layoutInfo.visibleItemsInfo
            if (visibleItems.isEmpty()) {
                selectedHour
            } else {
                val viewportCenter = layoutInfo.viewportStartOffset +
                    (layoutInfo.viewportEndOffset - layoutInfo.viewportStartOffset) / 2
                val closest = visibleItems.minByOrNull { item ->
                    val itemCenter = item.offset + item.size / 2
                    kotlin.math.abs(itemCenter - viewportCenter)
                }
                (closest?.index ?: (selectedHour - 1)) + 1
            }
        }
    }

    LaunchedEffect(currentCenteredHour) {
        if (currentCenteredHour in 1..12 && currentCenteredHour != selectedHour) {
            onHourSelected(currentCenteredHour)
        }
    }

    LaunchedEffect(selectedHour) {
        if (currentCenteredHour != selectedHour) {
            listState.animateScrollToItem((selectedHour - 1).coerceIn(0, 11))
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(132.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(NabdSurfaceSubtle)
            .border(1.dp, NabdBorder, RoundedCornerShape(12.dp))
            .semantics {
                contentDescription = "$selectedHour heures sélectionnées"
                stateDescription = "$selectedHour heures sélectionnées"
            }
            .testTag("vertical_hour_wheel"),
        contentAlignment = Alignment.Center
    ) {
        // Horizontal selection band centered in viewport
        Box(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .height(itemHeight)
                .clip(RoundedCornerShape(8.dp))
                .background(NabdPurple.copy(alpha = 0.10f))
                .border(1.dp, NabdPurple.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
        )

        LazyColumn(
            state = listState,
            flingBehavior = flingBehavior,
            contentPadding = PaddingValues(vertical = itemHeight),
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            items(hours.size, key = { hours[it] }) { index ->
                val hour = hours[index]
                val isSelected = (hour == currentCenteredHour)
                val distance = kotlin.math.abs(hour - currentCenteredHour)

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(itemHeight)
                        .clickable {
                            coroutineScope.launch {
                                listState.animateScrollToItem(index)
                            }
                        }
                        .testTag("hour_chip_$hour"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$hour h",
                        fontSize = if (isSelected) 22.sp else if (distance == 1) 16.sp else 13.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = when {
                            isSelected -> NabdPurple
                            distance == 1 -> NabdObsidian.copy(alpha = 0.55f)
                            else -> NabdMineralGray.copy(alpha = 0.35f)
                        }
                    )
                }
            }
        }
    }
}
