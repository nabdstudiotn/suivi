package com.example.ui.attendance

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
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
import com.example.ui.theme.NabdRestBackground
import com.example.ui.theme.NabdRestBorder
import com.example.ui.theme.NabdRestText
import com.example.ui.theme.NabdSurfaceSubtle
import com.example.ui.theme.NabdSurfaceWhite

/**
 * Section 1: Pointage — Duration and Attendance Entry Dialog.
 * Conforms strictly to 06_pointage_scope.md:
 * - Direct wheel / stepper controls for hours and minutes.
 * - No text keyboard.
 * - Stored internally as integer workedMinutes (e.g. 7 h 30 = 450 min). No Float/Double.
 * - 0 h 00 cannot be saved as work.
 * - French actions: Heures travaillées, Sélectionner la durée, Enregistrer, Annuler, Effacer, AB, À renseigner.
 */
@Composable
fun TimeEntryWheelDialog(
    worker: Worker,
    currentEntry: AttendanceEntry?,
    dateFrench: String,
    onDismiss: () -> Unit,
    onSaveWorkedHours: (hours: Int, minutes: Int) -> Unit,
    onSetAbsent: () -> Unit,
    onSetRestDay: () -> Unit,
    onResetToBlank: () -> Unit
) {
    var selectedHours by remember {
        mutableIntStateOf(
            if (currentEntry?.status == AttendanceStatus.WORKED) currentEntry.hours else 7
        )
    }
    var selectedMinutes by remember {
        mutableIntStateOf(
            if (currentEntry?.status == AttendanceStatus.WORKED) currentEntry.minutes else 0
        )
    }

    val isZeroDuration = (selectedHours == 0 && selectedMinutes == 0)

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .testTag("time_entry_dialog"),
            color = NabdSurfaceWhite,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header with Worker Initials and Close button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(NabdPurple.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = worker.initials,
                                fontWeight = FontWeight.Bold,
                                color = NabdPurple,
                                fontSize = 16.sp
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
                                text = "${worker.role} • $dateFrench",
                                style = MaterialTheme.typography.bodySmall,
                                color = NabdMineralGray
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("dialog_close_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Annuler",
                            tint = NabdObsidian
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Section Title: Heures travaillées
                Text(
                    text = "Heures travaillées",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = NabdObsidian
                )
                Text(
                    text = "Sélectionner la durée",
                    style = MaterialTheme.typography.bodySmall,
                    color = NabdMineralGray
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Digital display box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(NabdSurfaceSubtle)
                        .border(1.dp, NabdBorder, RoundedCornerShape(14.dp))
                        .padding(vertical = 14.dp),
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
                            text = "${selectedHours} h ${selectedMinutes.toString().padStart(2, '0')}",
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isZeroDuration) NabdCoralText else NabdObsidian
                        )
                    }
                }

                // Error hint if 0 h 00
                if (isZeroDuration) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "0 h 00 ne peut pas être enregistré comme temps de travail. Choisissez un état explicite ou renseignez une durée valide.",
                        style = MaterialTheme.typography.bodySmall,
                        color = NabdCoralText,
                        fontSize = 11.sp
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Hours Selector (Chips 1 to 12)
                Text(
                    text = "Heures ($selectedHours h)",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = NabdObsidian
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(4, 6, 7, 8, 9, 10).forEach { h ->
                        val isSelected = selectedHours == h
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) NabdPurple else NabdSurfaceSubtle)
                                .border(
                                    1.dp,
                                    if (isSelected) NabdPurple else NabdBorder,
                                    RoundedCornerShape(10.dp)
                                )
                                .clickable { selectedHours = h }
                                .testTag("hour_chip_$h"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "$h h",
                                color = if (isSelected) Color.White else NabdObsidian,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Minutes Selector (:00, :15, :30, :45)
                Text(
                    text = "Minutes ($selectedMinutes min)",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = NabdObsidian
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(0, 15, 30, 45).forEach { m ->
                        val isSelected = selectedMinutes == m
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) NabdPurple else NabdSurfaceSubtle)
                                .border(
                                    1.dp,
                                    if (isSelected) NabdPurple else NabdBorder,
                                    RoundedCornerShape(10.dp)
                                )
                                .clickable { selectedMinutes = m }
                                .testTag("minute_chip_$m"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = ":${m.toString().padStart(2, '0')}",
                                color = if (isSelected) Color.White else NabdObsidian,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Quick Presets
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PresetChip(
                        label = "7 h 00 (Normale)",
                        onClick = { selectedHours = 7; selectedMinutes = 0 },
                        modifier = Modifier.weight(1f)
                    )
                    PresetChip(
                        label = "7 h 30",
                        onClick = { selectedHours = 7; selectedMinutes = 30 },
                        modifier = Modifier.weight(1f)
                    )
                    PresetChip(
                        label = "8 h 00",
                        onClick = { selectedHours = 8; selectedMinutes = 0 },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Primary Save Button: Enregistrer
                Button(
                    onClick = {
                        if (!isZeroDuration) {
                            onSaveWorkedHours(selectedHours, selectedMinutes)
                        }
                    },
                    enabled = !isZeroDuration,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("save_worked_hours_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NabdPurple,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Enregistrer (${selectedHours} h ${selectedMinutes.toString().padStart(2, '0')})",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Specific States Section: AB | Jour de repos
                Text(
                    text = "Autres états du jour",
                    style = MaterialTheme.typography.titleSmall,
                    color = NabdObsidian,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // AB Button
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(NabdCoralBackground)
                            .border(1.dp, NabdCoralBorder, RoundedCornerShape(12.dp))
                            .clickable { onSetAbsent() }
                            .padding(vertical = 12.dp)
                            .testTag("action_mark_ab"),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "AB",
                                fontWeight = FontWeight.Bold,
                                color = NabdCoralText,
                                fontSize = 16.sp
                            )
                            Text(
                                text = "Absence constatée",
                                fontSize = 11.sp,
                                color = NabdCoralText
                            )
                        }
                    }

                    // Jour de repos Button
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(NabdRestBackground)
                            .border(1.dp, NabdRestBorder, RoundedCornerShape(12.dp))
                            .clickable { onSetRestDay() }
                            .padding(vertical = 12.dp)
                            .testTag("action_mark_repos"),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Jour de repos",
                                fontWeight = FontWeight.Bold,
                                color = NabdRestText,
                                fontSize = 15.sp
                            )
                            Text(
                                text = "Repos convenu",
                                fontSize = 11.sp,
                                color = NabdRestText
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Action: Effacer (Remettre à « À renseigner »)
                OutlinedButton(
                    onClick = onResetToBlank,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("action_reset_blank"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = NabdObsidian
                    ),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = androidx.compose.ui.graphics.SolidColor(NabdBorder)
                    )
                ) {
                    Text(
                        text = "Effacer la saisie (À renseigner)",
                        fontSize = 13.sp
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Action: Annuler
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("action_cancel"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = NabdMineralGray
                    ),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = androidx.compose.ui.graphics.SolidColor(NabdBorder.copy(alpha = 0.6f))
                    )
                ) {
                    Text(
                        text = "Annuler",
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun PresetChip(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(NabdSurfaceSubtle)
            .border(1.dp, NabdBorder, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = NabdObsidian
        )
    }
}
