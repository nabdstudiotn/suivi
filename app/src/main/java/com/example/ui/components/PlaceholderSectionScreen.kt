package com.example.ui.components

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.AtelierSection
import com.example.ui.theme.NabdBone
import com.example.ui.theme.NabdBorder
import com.example.ui.theme.NabdDeepViolet
import com.example.ui.theme.NabdMineralGray
import com.example.ui.theme.NabdObsidian
import com.example.ui.theme.NabdPurple
import com.example.ui.theme.NabdSurfaceWhite

/**
 * Screen presentation for secondary sections (Archives, Équipe, Paie, Annonces, Discussions, Paramètres).
 * Handles back navigation to Pointage per guidelines.
 */
@Composable
fun PlaceholderSectionScreen(
    section: AtelierSection,
    onBackToPointage: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBackToPointage)

    val (icon, subtitle, details) = when (section) {
        AtelierSection.ARCHIVES -> Triple(
            Icons.Default.Archive,
            "Historique & clôture mensuelle",
            "Consultation des mois passés clôturés, feuilles d'heures figées et vérification des présences antérieures."
        )
        AtelierSection.EQUIPE -> Triple(
            Icons.Default.Groups,
            "Gestion des ouvrières & profils",
            "Fiches individuelles, taux horaires par ouvrière, rôles (Piqueuse, Coupeuse, Finition) et historique d'embauche."
        )
        AtelierSection.PAIE -> Triple(
            Icons.Default.Payments,
            "Décompte & bordereau de paie",
            "Calcul mensuel indépendant (TND), gestion des acomptes, primes, reliquats antérieurs et échelle d'audit à 4 méthodes."
        )
        AtelierSection.COMMUNICATION -> Triple(
            Icons.Default.RecordVoiceOver,
            "Communication & affichage d'atelier",
            "Consignes de travail de la gérante, annonces officielles et échanges d'équipe internes."
        )
        AtelierSection.PARAMETRES -> Triple(
            Icons.Default.Settings,
            "Configuration & audit de conformité",
            "Sauvegarde locale de l'appareil, vérification des règles de calcul et journaux d'audit de la gérante."
        )
        AtelierSection.POINTAGE -> Triple(
            Icons.Default.Archive,
            "",
            ""
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NabdBone)
            .padding(16.dp)
            .testTag("placeholder_section_${section.name.lowercase()}"),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .border(1.dp, NabdBorder, RoundedCornerShape(20.dp)),
            color = NabdSurfaceWhite,
            tonalElevation = 2.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(NabdDeepViolet.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = NabdDeepViolet,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = section.titleFrench,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = NabdObsidian
                )

                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.titleSmall,
                    color = NabdPurple,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = details,
                    style = MaterialTheme.typography.bodyMedium,
                    color = NabdMineralGray,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(NabdBone)
                        .border(1.dp, NabdBorder, RoundedCornerShape(10.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        text = "Fonctionnalité en attente — Conformément à la politique d'intégrité (04_real_only_policy.md), aucune donnée fictive n'est affichée. Cette section sera activée lors de son étape dédiée avec persistance Room complète.",
                        fontSize = 12.sp,
                        color = NabdDeepViolet
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = onBackToPointage,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NabdPurple,
                        contentColor = Color.White
                    ),
                    modifier = Modifier.testTag("btn_back_to_pointage")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(text = "Retour au Pointage")
                    }
                }
            }
        }
    }
}
