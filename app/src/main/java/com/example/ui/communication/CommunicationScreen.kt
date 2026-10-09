package com.example.ui.communication

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.Group
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.window.Dialog
import com.example.domain.model.Announcement
import com.example.domain.model.AtelierDate
import com.example.ui.theme.NabdBone
import com.example.ui.theme.NabdBorder
import com.example.ui.theme.NabdCoralText
import com.example.ui.theme.NabdDeepViolet
import com.example.ui.theme.NabdMineralGray
import com.example.ui.theme.NabdObsidian
import com.example.ui.theme.NabdPurple
import com.example.ui.theme.NabdSurfaceSubtle
import com.example.ui.theme.NabdSurfaceWhite

/**
 * Section 4: Communication (Annonces & Discussions).
 * Conforme à la section 6 des corrections approuvées.
 */
@Composable
fun CommunicationScreen(
    announcements: List<Announcement>,
    onCreateAnnouncement: (title: String, content: String, audience: String, expirationDateIso: String) -> Unit,
    onDeleteAnnouncement: (id: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(0) } // 0: Annonces, 1: Discussions
    var showCreateDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NabdBone)
            .padding(horizontal = 16.dp)
            .testTag("communication_screen")
    ) {
        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "Communication d'atelier",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = NabdObsidian
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Sub-tabs: Annonces | Discussions
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(NabdSurfaceWhite)
                .border(1.dp, NabdBorder, RoundedCornerShape(12.dp))
                .padding(4.dp)
        ) {
            TabButton(
                title = "Annonces (${announcements.size})",
                isSelected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                modifier = Modifier.weight(1f)
            )
            TabButton(
                title = "Discussions",
                isSelected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (selectedTab == 0) {
            // Tab 1: Annonces
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Tableau des consignes réelles",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = NabdObsidian
                )
                Button(
                    onClick = { showCreateDialog = true },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NabdPurple),
                    modifier = Modifier.testTag("btn_create_announcement")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.size(4.dp))
                    Text(text = "Publier", fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (announcements.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 30.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(NabdSurfaceWhite)
                        .border(1.dp, NabdBorder, RoundedCornerShape(16.dp))
                        .padding(28.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Aucune annonce active",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = NabdObsidian
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Les consignes de la gérante avec date d'expiration s'afficheront ici après publication.",
                            style = MaterialTheme.typography.bodySmall,
                            color = NabdMineralGray
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(announcements, key = { it.id }) { ann ->
                        AnnouncementCard(announcement = ann, onDelete = { onDeleteAnnouncement(ann.id) })
                    }
                    item { Spacer(modifier = Modifier.height(24.dp)) }
                }
            }
        } else {
            // Tab 2: Discussions
            DiscussionsPolicyView()
        }
    }

    if (showCreateDialog) {
        CreateAnnouncementDialog(
            onDismiss = { showCreateDialog = false },
            onConfirm = { t, c, a, e ->
                onCreateAnnouncement(t, c, a, e)
                showCreateDialog = false
            }
        )
    }
}

@Composable
private fun TabButton(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) NabdPurple else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) Color.White else NabdObsidian
        )
    }
}

@Composable
private fun AnnouncementCard(
    announcement: Announcement,
    onDelete: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, NabdBorder, RoundedCornerShape(14.dp)),
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
                        imageVector = Icons.Default.Campaign,
                        contentDescription = null,
                        tint = NabdPurple,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = announcement.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = NabdObsidian
                    )
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Supprimer",
                        tint = NabdCoralText,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = announcement.content,
                style = MaterialTheme.typography.bodyMedium,
                color = NabdObsidian
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Public : ${announcement.targetAudience}",
                    fontSize = 11.sp,
                    color = NabdDeepViolet,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Expire le : ${announcement.expirationDateIso}",
                    fontSize = 11.sp,
                    color = NabdMineralGray
                )
            }
        }
    }
}

@Composable
private fun DiscussionsPolicyView() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, NabdBorder, RoundedCornerShape(16.dp)),
        color = NabdSurfaceWhite,
        tonalElevation = 1.dp
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    modifier = Modifier.size(44.dp).clip(CircleShape).background(NabdDeepViolet.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = Icons.Default.Chat, contentDescription = null, tint = NabdDeepViolet)
                }
                Column {
                    Text(
                        text = "Messagerie d'équipe (Règles & Rétention)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = NabdObsidian
                    )
                    Text(text = "Intégrité des données protégée", style = MaterialTheme.typography.bodySmall, color = NabdMineralGray)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Conformément à la section 6 des corrections approuvées :",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = NabdObsidian
            )

            Spacer(modifier = Modifier.height(8.dp))

            BulletPoint(title = "Discussion de groupe", desc = "Contenu purgé mensuellement ; les groupes restent ouverts.")
            BulletPoint(title = "Discussion privée", desc = "Échanges ouvrière/ouvrière purgés après 4 jours.")
            BulletPoint(title = "Messagerie avec la gérante", desc = "Échanges privés avec la direction purgés après 14 jours.")
            BulletPoint(title = "Intégrité absolue", desc = "La purge des messages ne touche JAMAIS les fiches d'ouvrières, les feuilles de paie, les archives ni les journaux d'audit.")

            Spacer(modifier = Modifier.height(14.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(NabdSurfaceSubtle)
                    .padding(12.dp)
            ) {
                Text(
                    text = "Statut : En attente du module réseau authentifié et des permissions. Aucune conversation simulée n'est affichée.",
                    fontSize = 12.sp,
                    color = NabdDeepViolet
                )
            }
        }
    }
}

@Composable
private fun BulletPoint(title: String, desc: String) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(text = "• $title", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = NabdObsidian)
        Text(text = "   $desc", fontSize = 12.sp, color = NabdMineralGray)
    }
}

@Composable
private fun CreateAnnouncementDialog(
    onDismiss: () -> Unit,
    onConfirm: (title: String, content: String, audience: String, expirationDateIso: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var audience by remember { mutableStateOf("Tout l'atelier") }
    var expirationDate by remember {
        mutableStateOf(AtelierDate.todayTunis().nextDay().nextDay().isoString)
    }
    var error by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)),
            color = NabdSurfaceWhite,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.padding(18.dp).verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Publier une consigne", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    IconButton(onClick = onDismiss) { Icon(imageVector = Icons.Default.Close, contentDescription = "Fermer") }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it; error = null },
                    label = { Text("Titre de l'annonce") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it; error = null },
                    label = { Text("Contenu de la consigne") },
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = audience,
                    onValueChange = { audience = it },
                    label = { Text("Audience cible (ex: Tout l'atelier)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = expirationDate,
                    onValueChange = { expirationDate = it; error = null },
                    label = { Text("Date d'expiration (AAAA-MM-JJ)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (error != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = error!!, color = MaterialTheme.colorScheme.error, fontSize = 11.sp)
                }

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = {
                        if (title.isBlank() || content.isBlank()) {
                            error = "Le titre et le contenu sont obligatoires."
                            return@Button
                        }
                        onConfirm(title.trim(), content.trim(), audience.trim(), expirationDate.trim())
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NabdPurple)
                ) {
                    Text("Publier l'annonce")
                }
            }
        }
    }
}
