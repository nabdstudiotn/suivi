package com.example.ui.team

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Note
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
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
import androidx.compose.runtime.mutableStateOf
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
import com.example.domain.model.HourlyRateRecord
import com.example.domain.model.Worker
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
 * Worker profile detail screen (Section 2 - Équipe):
 * Displays single continuous profile, effective hourly rates, return dates,
 * internal immutable ID, and direct entry point to the worker's personal monthly archive.
 */
@Composable
fun WorkerProfileScreen(
    worker: Worker,
    rateHistory: List<HourlyRateRecord>,
    onBack: () -> Unit,
    onViewWorkerArchive: (Worker) -> Unit,
    onToggleActiveStatus: (Worker) -> Unit,
    onAddRateClicked: (Worker) -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)

    var showArchiveConfirmDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NabdBone)
            .padding(horizontal = 16.dp)
            .testTag("worker_profile_screen")
    ) {
        Spacer(modifier = Modifier.height(10.dp))

        // Top Navigation Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.testTag("btn_profile_back")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Retour",
                    tint = NabdObsidian
                )
            }
            Text(
                text = "Fiche ouvrière",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = NabdObsidian
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Main Identity Card
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.dp, NabdBorder, RoundedCornerShape(16.dp)),
                    color = NabdSurfaceWhite,
                    tonalElevation = 2.dp
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .background(NabdPurple.copy(alpha = 0.12f))
                                    .border(1.5.dp, NabdPurple.copy(alpha = 0.25f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = worker.initials,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NabdPurple
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = worker.fullName,
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = NabdObsidian
                                )
                                Text(
                                    text = worker.role,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = NabdMineralGray
                                )
                            }

                            StatusBadge(status = worker.profileStatus)
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Internal UUID identifier (Not dependent on name)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(NabdSurfaceSubtle)
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Identifiant système interne",
                                    fontSize = 11.sp,
                                    color = NabdMineralGray
                                )
                                Text(
                                    text = "UUID: ${worker.id.take(8)}...",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = NabdMineralGray
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Dates metadata
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "Date d'embauche",
                                    fontSize = 11.sp,
                                    color = NabdMineralGray
                                )
                                Text(
                                    text = worker.hireDateIso,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = NabdObsidian
                                )
                            }

                            if (worker.archivedDateIso != null) {
                                Column {
                                    Text(
                                        text = "Date d'archivage",
                                        fontSize = 11.sp,
                                        color = NabdMineralGray
                                    )
                                    Text(
                                        text = worker.archivedDateIso,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = NabdCoralText
                                    )
                                }
                            }

                            if (worker.returnDateIso != null) {
                                Column {
                                    Text(
                                        text = "Date de reprise",
                                        fontSize = 11.sp,
                                        color = NabdMineralGray
                                    )
                                    Text(
                                        text = worker.returnDateIso,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = NabdPurple
                                    )
                                }
                            }
                        }

                        // Workshop notes if present
                        if (!worker.notes.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(NabdBone)
                                    .padding(10.dp)
                            ) {
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Note,
                                            contentDescription = null,
                                            tint = NabdMineralGray,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Notes d'atelier :",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = NabdMineralGray
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = worker.notes,
                                        fontSize = 12.sp,
                                        color = NabdObsidian
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Current Hourly Rate Card
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.dp, NabdBorder, RoundedCornerShape(16.dp)),
                    color = NabdSurfaceWhite,
                    tonalElevation = 2.dp
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Rémunération horaire courante",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = NabdObsidian
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "${worker.rateFormattedTnd}/h",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NabdDeepViolet
                                )
                            }
                            Button(
                                onClick = { onAddRateClicked(worker) },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = NabdPurple,
                                    contentColor = Color.White
                                ),
                                modifier = Modifier.testTag("btn_open_add_rate")
                            ) {
                                Text(text = "Nouveau taux", fontSize = 12.sp)
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Calcul exact en millimes entiers (1 TND = 1 000 millimes)",
                            fontSize = 11.sp,
                            color = NabdMineralGray
                        )
                    }
                }
            }

            // Rate History Section
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Historique des taux horaires (${rateHistory.size})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = NabdObsidian
                    )
                }
            }

            items(rateHistory, key = { it.id }) { rate ->
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
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = rate.rateFormattedTnd,
                                fontWeight = FontWeight.Bold,
                                color = NabdObsidian,
                                fontSize = 14.sp
                            )
                            Text(
                                text = rate.motif,
                                fontSize = 11.sp,
                                color = NabdMineralGray
                            )
                        }
                        Text(
                            text = "Effet : ${rate.effectiveDateIso}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = NabdDeepViolet
                        )
                    }
                }
            }

            // Actions Card
            item {
                Spacer(modifier = Modifier.height(6.dp))

                Button(
                    onClick = { onViewWorkerArchive(worker) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .testTag("btn_view_worker_archive"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NabdPurple,
                        contentColor = Color.White
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Archive,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Consulter les archives de pointage",
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = { showArchiveConfirmDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .testTag("btn_toggle_active_status"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = if (worker.isActive) NabdCoralText else NabdMintText
                    )
                ) {
                    Text(
                        text = if (worker.isActive) "Archiver l'ouvrière (interruption)" else "Réactiver le profil (reprise d'activité)",
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Confirmation dialog before changing active / archive status
    if (showArchiveConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showArchiveConfirmDialog = false },
            icon = {
                Icon(
                    imageVector = if (worker.isActive) Icons.Default.Warning else Icons.Default.Info,
                    contentDescription = null,
                    tint = if (worker.isActive) NabdCoralText else NabdPurple
                )
            },
            title = {
                Text(
                    text = if (worker.isActive) "Archiver l'ouvrière" else "Réactiver l'ouvrière",
                    fontWeight = FontWeight.Bold,
                    color = NabdObsidian
                )
            },
            text = {
                Text(
                    text = if (worker.isActive)
                        "L'archivage marque la fin de la période d'activité courante. Tous les pointages, paiements et taux historiques restent strictement conservés et consultables dans les archives."
                    else
                        "La réactivation enregistre une reprise d'activité effective à partir d'aujourd'hui. L'ouvrière réapparaîtra dans la liste des pointages journaliers.",
                    fontSize = 13.sp,
                    color = NabdMineralGray
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showArchiveConfirmDialog = false
                        onToggleActiveStatus(worker)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (worker.isActive) Color(0xFFDC2626) else NabdPurple,
                        contentColor = Color.White
                    )
                ) {
                    Text(if (worker.isActive) "Confirmer l'archivage" else "Confirmer la réactivation")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showArchiveConfirmDialog = false }) {
                    Text("Annuler")
                }
            }
        )
    }
}
