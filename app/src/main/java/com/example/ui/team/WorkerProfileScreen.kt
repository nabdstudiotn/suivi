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
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
 * Worker profile detail screen:
 * Displays single continuous profile, effective hourly rates, return dates,
 * and direct entry point to the worker's personal monthly archive.
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

                            // Active / Archived badge
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (worker.isActive) NabdMintBackground else NabdCoralBackground)
                                    .border(
                                        1.dp,
                                        if (worker.isActive) NabdMintBorder else NabdCoralBorder,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    text = if (worker.isActive) "Active" else "Archivée",
                                    color = if (worker.isActive) NabdMintText else NabdCoralText,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

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
                                    text = worker.rateFormattedTnd,
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
                            text = "Consulter les archives de travail",
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = { onToggleActiveStatus(worker) },
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
}
