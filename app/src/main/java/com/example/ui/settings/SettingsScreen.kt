package com.example.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.sync.SyncState
import com.example.ui.theme.MatteSurfaceCard
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

/**
 * Settings screen detailing explicit service roles, storage architecture,
 * and security boundaries as specified in Stage 0.
 */
@Composable
fun SettingsScreen(
  syncState: SyncState,
  modifier: Modifier = Modifier
) {
  Column(
    modifier = modifier
      .fillMaxSize()
      .padding(16.dp)
  ) {
    Text(
      text = "Paramètres & Architecture",
      style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
      color = TextPrimary
    )
    Spacer(modifier = Modifier.height(16.dp))

    // Section 1: Stockage & Synchronisation
    Card(
      shape = RoundedCornerShape(12.dp),
      colors = CardDefaults.cardColors(containerColor = MatteSurfaceCard),
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
        Text(
          text = "Stockage et synchronisation",
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
          color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
          text = "• Room : Base locale sur l'appareil (source principale hors-ligne).\n" +
                 "• Cloud Firestore : Synchronisation cloud et permissions.\n" +
                 "• Google Drive : Canal de sauvegarde indépendant (ne remplace jamais sans confirmation).",
          style = MaterialTheme.typography.bodyMedium,
          color = TextSecondary
        )
        Spacer(modifier = Modifier.height(12.dp))
        val statusLabel = when (syncState) {
          is SyncState.LocalOnly -> "État : Données locales uniquement (hors-ligne)"
          is SyncState.Synchronizing -> "État : Synchronisation en cours..."
          is SyncState.Synchronized -> "État : Synchronisé"
          is SyncState.SyncUnavailable -> "État : Synchronisation indisponible (${syncState.reason})"
          is SyncState.Conflict -> "État : Conflit à vérifier"
        }
        Text(
          text = statusLabel,
          style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
          color = TextPrimary
        )
      }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Section 2: Cadre et limites de l'IA (Gemini)
    Card(
      shape = RoundedCornerShape(12.dp),
      colors = CardDefaults.cardColors(containerColor = MatteSurfaceCard),
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
        Text(
          text = "Rôle de l'Intelligence Artificielle (Gemini)",
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
          color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
          text = "• Évaluation mensuelle sur 100 uniquement :\n" +
                 "  - 40 pts : Régularité horaire (réf. 7h)\n" +
                 "  - 30 pts : Assiduité et absences (AB)\n" +
                 "  - 30 pts : Observations datées\n" +
                 "• Chaque conclusion dispose de « Voir la preuve » datée.\n" +
                 "• En cas de données insuffisantes : « Données insuffisantes ».\n" +
                 "• L'IA ne modifie jamais les heures, les tarifs, la paie ni les statuts.",
          style = MaterialTheme.typography.bodyMedium,
          color = TextSecondary
        )
      }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Section 3: Compte et sécurité
    Card(
      shape = RoundedCornerShape(12.dp),
      colors = CardDefaults.cardColors(containerColor = MatteSurfaceCard),
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
        Text(
          text = "Compte et sécurité",
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
          color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
          text = "• Firebase Authentication : Gestion de session et identité.\n" +
                 "• Aucune clé secrète n'est exposée côté client.\n" +
                 "• Migration non destructrice préservant les données de l'appareil.",
          style = MaterialTheme.typography.bodyMedium,
          color = TextSecondary
        )
      }
    }
  }
}
