package com.example.ui.attendance

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.Worker
import com.example.ui.theme.MatteBorder
import com.example.ui.theme.MatteSurfaceCard
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PointageScreen(
  selectedDate: LocalDate,
  workers: List<Worker>,
  onDateClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  val frenchFormatter = DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", Locale.FRENCH)
  val formattedDate = selectedDate.format(frenchFormatter).replaceFirstChar { it.uppercase() }

  Column(
    modifier = modifier
      .fillMaxSize()
      .padding(horizontal = 16.dp, vertical = 12.dp)
  ) {
    // Compact task-focused header: Suivi, Interactive real date selector
    // Removed static cloud / synchronized badge, sign-out button, and repeated text per specification
    Surface(
      onClick = onDateClick,
      shape = RoundedCornerShape(12.dp),
      color = MatteSurfaceCard,
      tonalElevation = 1.dp,
      modifier = Modifier
        .fillMaxWidth()
        .testTag("date_selector_button")
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
      ) {
        Column {
          Text(
            text = "Date de pointage",
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary
          )
          Spacer(modifier = Modifier.height(2.dp))
          Text(
            text = formattedDate,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = TextPrimary
          )
        }
        Icon(
          imageVector = Icons.Default.CalendarToday,
          contentDescription = "Choisir une date",
          tint = MaterialTheme.colorScheme.primary
        )
      }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Real workers list or honest empty state (No sample workers or fake records)
    if (workers.isEmpty()) {
      Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f)
          .padding(32.dp)
      ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Text(
            text = "Aucune ouvrière enregistrée",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Medium),
            color = TextSecondary
          )
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = "Les ouvrières ajoutées dans Équipe s'afficheront ici.",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary
          )
        }
      }
    } else {
      LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.weight(1f)
      ) {
        items(workers, key = { it.id }) { worker ->
          WorkerAttendanceRow(worker = worker)
        }
      }
    }
  }
}

@Composable
fun WorkerAttendanceRow(
  worker: Worker,
  modifier: Modifier = Modifier
) {
  Card(
    shape = RoundedCornerShape(10.dp),
    colors = CardDefaults.cardColors(containerColor = MatteSurfaceCard),
    elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp),
    modifier = modifier.fillMaxWidth()
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
      modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
      Column {
        Text(
          text = worker.fullName,
          style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
          color = TextPrimary
        )
        Text(
          text = worker.status.labelFr,
          style = MaterialTheme.typography.bodySmall,
          color = TextSecondary
        )
      }
    }
  }
}
