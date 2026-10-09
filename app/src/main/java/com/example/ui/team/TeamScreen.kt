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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import com.example.domain.model.Worker
import com.example.domain.model.WorkerProfileStatus
import com.example.domain.model.normalizeForFrenchSearch
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

enum class TeamFilterTab(val label: String) {
    ALL("Toutes"),
    ACTIVE("Actives"),
    ARCHIVED("Archivées")
}

/**
 * Team list screen (Équipe) according to Section 2 specifications:
 * - Persistent worker profiles with independent internal UUID.
 * - French accent-insensitive & case-insensitive search.
 * - Filter tabs (Toutes, Actives, Archivées).
 * - Honest empty states.
 * - Worker cards with status badges and hourly rate preview.
 */
@Composable
fun TeamScreen(
    workers: List<Worker>,
    onWorkerClicked: (Worker) -> Unit,
    onAddWorkerClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedTab by remember { mutableStateOf(TeamFilterTab.ALL) }

    val activeCount = remember(workers) { workers.count { it.isActive } }
    val archivedCount = remember(workers) { workers.count { !it.isActive } }

    // Filter by tab
    val tabFiltered = remember(workers, selectedTab) {
        when (selectedTab) {
            TeamFilterTab.ALL -> workers
            TeamFilterTab.ACTIVE -> workers.filter { it.isActive }
            TeamFilterTab.ARCHIVED -> workers.filter { !it.isActive }
        }
    }

    // Filter by search query (accent and case insensitive)
    val filteredWorkers = remember(tabFiltered, searchQuery) {
        if (searchQuery.isBlank()) {
            tabFiltered
        } else {
            val queryNormalized = normalizeForFrenchSearch(searchQuery)
            tabFiltered.filter { w ->
                normalizeForFrenchSearch(w.firstName).contains(queryNormalized) ||
                        normalizeForFrenchSearch(w.lastName).contains(queryNormalized) ||
                        normalizeForFrenchSearch(w.fullName).contains(queryNormalized) ||
                        normalizeForFrenchSearch(w.role).contains(queryNormalized)
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(NabdBone)
            .padding(horizontal = 16.dp)
            .testTag("team_screen")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Spacer(modifier = Modifier.height(14.dp))

            // Screen Header: Title + Add Worker Button
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
                            "Base d'atelier vide — Enregistrement requis"
                        else
                            "$activeCount actives • $archivedCount archivées",
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
                // Honest Initial Empty State
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
                            text = "L'application fonctionne exclusivement à partir de données réelles d'atelier. Cliquez ci-dessous pour créer la première fiche.",
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
                // Search Bar (Case & Accent Insensitive)
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            text = "Rechercher par prénom, nom ou rôle…",
                            fontSize = 13.sp,
                            color = NabdMineralGray
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Rechercher",
                            tint = NabdMineralGray,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Effacer",
                                    tint = NabdMineralGray,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = NabdSurfaceWhite,
                        unfocusedContainerColor = NabdSurfaceWhite,
                        focusedBorderColor = NabdPurple,
                        unfocusedBorderColor = NabdBorder
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_team_search")
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Filter Tabs (Toutes, Actives, Archivées)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TeamFilterTab.values().forEach { tab ->
                        val isSelected = selectedTab == tab
                        val count = when (tab) {
                            TeamFilterTab.ALL -> workers.size
                            TeamFilterTab.ACTIVE -> activeCount
                            TeamFilterTab.ARCHIVED -> archivedCount
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) NabdPurple else NabdSurfaceWhite)
                                .border(
                                    1.dp,
                                    if (isSelected) NabdPurple else NabdBorder,
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable { selectedTab = tab }
                                .padding(horizontal = 12.dp, vertical = 7.dp)
                                .testTag("tab_${tab.name.lowercase()}"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${tab.label} ($count)",
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else NabdObsidian
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Workers List or Search Empty State
                if (filteredWorkers.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 30.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(NabdSurfaceWhite)
                            .border(1.dp, NabdBorder, RoundedCornerShape(14.dp))
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = if (searchQuery.isNotBlank())
                                    "Aucun résultat pour « $searchQuery »"
                                else
                                    "Aucune ouvrière dans cette catégorie",
                                fontWeight = FontWeight.SemiBold,
                                color = NabdObsidian,
                                fontSize = 14.sp
                            )
                            if (searchQuery.isNotBlank()) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Button(
                                    onClick = { searchQuery = "" },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = NabdPurple.copy(alpha = 0.12f),
                                        contentColor = NabdPurple
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Effacer la recherche", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(filteredWorkers, key = { it.id }) { worker ->
                            TeamMemberCard(
                                worker = worker,
                                onClick = { onWorkerClicked(worker) }
                            )
                        }

                        item {
                            Spacer(modifier = Modifier.height(24.dp))
                        }
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
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                // Circle Initials Avatar
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(
                            if (worker.isActive) NabdPurple.copy(alpha = 0.12f)
                            else NabdMineralGray.copy(alpha = 0.12f)
                        )
                        .border(
                            1.dp,
                            if (worker.isActive) NabdPurple.copy(alpha = 0.25f)
                            else NabdBorder,
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = worker.initials,
                        fontWeight = FontWeight.Bold,
                        color = if (worker.isActive) NabdPurple else NabdMineralGray,
                        fontSize = 15.sp
                    )
                }

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = worker.fullName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = NabdObsidian
                        )
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = worker.role,
                            style = MaterialTheme.typography.bodySmall,
                            color = NabdMineralGray
                        )
                        Text(
                            text = " • ",
                            style = MaterialTheme.typography.bodySmall,
                            color = NabdMineralGray
                        )
                        Text(
                            text = "${worker.rateFormattedTnd}/h",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = NabdDeepViolet
                        )
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = "Embauchée le : ${worker.hireDateIso}",
                        fontSize = 11.sp,
                        color = NabdMineralGray
                    )
                }
            }

            // Status Badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatusBadge(status = worker.profileStatus)

                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Ouvrir",
                    tint = NabdMineralGray.copy(alpha = 0.7f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun StatusBadge(status: WorkerProfileStatus) {
    val (bgColor, borderColor, textColor) = when (status) {
        WorkerProfileStatus.ACTIVE -> Triple(NabdMintBackground, NabdMintBorder, NabdMintText)
        WorkerProfileStatus.ARCHIVED -> Triple(NabdCoralBackground, NabdCoralBorder, NabdCoralText)
        WorkerProfileStatus.CONFLICT -> Triple(Color(0xFFFEF3C7), Color(0xFFFDE68A), Color(0xFF92400E))
        WorkerProfileStatus.READ_ONLY -> Triple(NabdSurfaceSubtle, NabdBorder, NabdMineralGray)
        WorkerProfileStatus.PENDING_SAVE -> Triple(NabdSurfaceSubtle, NabdBorder, NabdMineralGray)
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = status.labelFrench,
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
