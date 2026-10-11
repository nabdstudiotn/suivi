package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.AtelierSection
import com.example.ui.theme.NabdBorder
import com.example.ui.theme.NabdObsidian
import com.example.ui.theme.NabdPurple
import com.example.ui.theme.NabdSurfaceSubtle
import com.example.ui.theme.NabdSurfaceWhite

/**
 * Horizontal section navigation bar per project screen map:
 * Pointage -> Équipe -> Paie -> Communication -> Archives -> Paramètres.
 * Adheres to Requirement 7:
 * - Deterministic Material vector icons.
 * - French content descriptions.
 * - Distinct selected state via icon, text semantics, restrained purple, background, and border.
 */
@Composable
fun SectionNavigationBar(
    currentSection: AtelierSection,
    onSectionSelected: (AtelierSection) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(NabdSurfaceWhite)
            .border(
                width = 1.dp,
                color = NabdBorder.copy(alpha = 0.6f),
                shape = RoundedCornerShape(0.dp)
            )
            .padding(vertical = 8.dp)
            .testTag("section_nav_bar")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState)
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AtelierSection.values().forEach { section ->
                val isSelected = section == currentSection
                val tag = "nav_tab_${section.name.lowercase()}"

                val (icon, frenchDesc) = when (section) {
                    AtelierSection.POINTAGE -> Icons.Default.EventAvailable to "Section Pointage"
                    AtelierSection.EQUIPE -> Icons.Default.Group to "Section Équipe"
                    AtelierSection.PAIE -> Icons.Default.Payments to "Section Paie et règlements"
                    AtelierSection.COMMUNICATION -> Icons.Default.Chat to "Section Communication"
                    AtelierSection.ARCHIVES -> Icons.Default.Inventory2 to "Section Archives"
                    AtelierSection.PARAMETRES -> Icons.Default.Settings to "Section Paramètres"
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) NabdPurple else NabdSurfaceSubtle)
                        .border(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = if (isSelected) NabdPurple else NabdBorder,
                            shape = RoundedCornerShape(10.dp)
                        )
                        .clickable { onSectionSelected(section) }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .semantics {
                            selected = isSelected
                            contentDescription = "${section.titleFrench}, ${if (isSelected) "sélectionné" else "non sélectionné"}"
                        }
                        .testTag(tag),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = frenchDesc,
                            tint = if (isSelected) Color.White else NabdObsidian,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = section.titleFrench,
                            style = MaterialTheme.typography.labelLarge,
                            color = if (isSelected) Color.White else NabdObsidian,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}
