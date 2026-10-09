package com.example.ui.team

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import com.example.domain.model.Worker
import com.example.ui.theme.NabdBone
import com.example.ui.theme.NabdBorder
import com.example.ui.theme.NabdCoralBackground
import com.example.ui.theme.NabdCoralBorder
import com.example.ui.theme.NabdCoralText
import com.example.ui.theme.NabdMineralGray
import com.example.ui.theme.NabdMintBackground
import com.example.ui.theme.NabdMintBorder
import com.example.ui.theme.NabdMintText
import com.example.ui.theme.NabdObsidian
import com.example.ui.theme.NabdPurple
import com.example.ui.theme.NabdSurfaceWhite

/**
 * Team list screen (Équipe):
 * Displays real active and archived workers from Room SQLite storage.
 * Provides clean empty state when no worker has been entered yet.
 */
@Composable
fun TeamScreen(
    workers: List<Worker>,
    onWorkerClicked: (Worker) -> Unit,
    onAddWorkerClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeWorkers = workers.filter { it.isActive }
    val archivedWorkers = workers.filter { !it.isActive }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(NabdBone)
            .padding(horizontal = 16.dp)
            .testTag("team_screen")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Équipe d'atelier",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = NabdObsidian
                    )
                    Text(
                        text = if (workers.isEmpty())
                            "Base locale vide — Enregistrement requis"
                        else
                            "${activeWorkers.size} actives • ${archivedWorkers.size} archivées",
                        style = MaterialTheme.typography.bodySmall,
                        color = NabdMineralGray
                    )
                }

                Button(
                    onClick = onAddWorkerClicked,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NabdPurple,
                        contentColor = Color.White
                    ),
                    modifier = Modifier.testTag("btn_add_worker_top")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.size(6.dp))
                    Text(text = "Ajouter", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (workers.isEmpty()) {
                // Honest Empty State
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 40.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(NabdSurfaceWhite)
                        .border(1.dp, NabdBorder, RoundedCornerShape(16.dp))
                        .padding(32.dp)
                        .testTag("team_empty_state"),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(NabdPurple.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.GroupAdd,
                                contentDescription = null,
                                tint = NabdPurple,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Aucune ouvrière enregistrée",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = NabdObsidian
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "L'application fonctionne exclusivement à partir de données réelles persistées. Cliquez ci-dessous pour créer la première fiche.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = NabdMineralGray,
                            lineHeight = 20.sp
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        Button(
                            onClick = onAddWorkerClicked,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NabdPurple,
                                contentColor = Color.White
                            ),
                            modifier = Modifier.testTag("btn_empty_state_add_worker")
                        ) {
                            Text(text = "Créer la première fiche")
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (activeWorkers.isNotEmpty()) {
                        item {
                            Text(
                                text = "Ouvrières en activité (${activeWorkers.size})",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = NabdObsidian
                            )
                        }

                        items(activeWorkers, key = { it.id }) { worker ->
                            TeamMemberCard(worker = worker, onClick = { onWorkerClicked(worker) })
                        }
                    }

                    if (archivedWorkers.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Profils archivés (${archivedWorkers.size})",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = NabdMineralGray
                            )
                        }

                        items(archivedWorkers, key = { it.id }) { worker ->
                            TeamMemberCard(worker = worker, onClick = { onWorkerClicked(worker) })
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun TeamMemberCard(
    worker: Worker,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, NabdBorder, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .testTag("team_card_${worker.id}"),
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
                        text = "${worker.role} • ${worker.rateFormattedTnd}/h",
                        style = MaterialTheme.typography.bodySmall,
                        color = NabdMineralGray
                    )
                }
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (worker.isActive) NabdMintBackground else NabdCoralBackground)
                    .border(
                        1.dp,
                        if (worker.isActive) NabdMintBorder else NabdCoralBorder,
                        RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = if (worker.isActive) "Active" else "Archivée",
                    color = if (worker.isActive) NabdMintText else NabdCoralText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
