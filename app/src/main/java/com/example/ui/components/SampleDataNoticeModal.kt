package com.example.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.NabdCoralText
import com.example.ui.theme.NabdDeepViolet
import com.example.ui.theme.NabdMintText
import com.example.ui.theme.NabdObsidian
import com.example.ui.theme.NabdPurple
import com.example.ui.theme.NabdSurfaceWhite

/**
 * Transparent status report modal detailing real local persistence and strictly
 * unlinked cloud services per 04_real_only_policy.md.
 */
@Composable
fun SampleDataNoticeModal(
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .testTag("sample_data_modal"),
            color = NabdSurfaceWhite,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(NabdDeepViolet.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = NabdDeepViolet,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Text(
                            text = "Politique d'intégrité réelle",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = NabdObsidian
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_status_modal")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Fermer",
                            tint = NabdObsidian
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Section 1: Ce qui fonctionne réellement
                Text(
                    text = "Persistance locale & opérations réelles :",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = NabdMintText
                )
                Spacer(modifier = Modifier.height(6.dp))

                StatusItem(
                    title = "Stockage local SQLite (Room)",
                    desc = "Aucune fausse donnée inventée. Les ouvrières, taux et pointages proviennent exclusivement de la saisie utilisateur et de Room.",
                    isActive = true
                )
                StatusItem(
                    title = "Arithmétique monétaire exacte",
                    desc = "Calculs déterministes en millimes entiers (1 TND = 1 000 millimes) sans virgule flottante binaire.",
                    isActive = true
                )
                StatusItem(
                    title = "Aucune évaluation par IA",
                    desc = "Le produit ne comporte aucun score IA, avis automatique ou évaluateur. Le calcul est strictement applicatif et déterministe.",
                    isActive = true
                )
                StatusItem(
                    title = "Pointage et règles de dates",
                    desc = "Heures réelles, absence AB, jour de repos et À renseigner. Demain est strictement verrouillé et le dimanche est le repos habituel.",
                    isActive = true
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Section 2: Services non connectés
                Text(
                    text = "Services cloud non connectés (en attente) :",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = NabdCoralText
                )
                Spacer(modifier = Modifier.height(6.dp))

                UnconnectedItem(
                    title = "Firebase & Firestore",
                    desc = "Non connecté. Le stockage du téléphone de la gérante est l'unique copie maîtresse actuelle."
                )
                UnconnectedItem(
                    title = "Google Drive & Sauvegarde externe",
                    desc = "Non connecté. Aucune synchronisation externe n'est simulée."
                )
                UnconnectedItem(
                    title = "Comptes & sessions multi-utilisateurs",
                    desc = "Non connectés. Aucun faux écran de connexion n'est présenté."
                )

                Spacer(modifier = Modifier.height(18.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NabdPurple,
                        contentColor = Color.White
                    )
                ) {
                    Text("J'ai compris")
                }
            }
        }
    }
}

@Composable
private fun StatusItem(title: String, desc: String, isActive: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = if (isActive) NabdMintText else Color.Gray,
            modifier = Modifier
                .size(16.dp)
                .padding(top = 2.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = NabdObsidian
            )
            Text(
                text = desc,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.tertiary
            )
        }
    }
}

@Composable
private fun UnconnectedItem(title: String, desc: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = Icons.Default.CloudOff,
            contentDescription = null,
            tint = NabdCoralText,
            modifier = Modifier
                .size(16.dp)
                .padding(top = 2.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = NabdObsidian
            )
            Text(
                text = desc,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.tertiary
            )
        }
    }
}
