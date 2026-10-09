package com.example.ui.archives

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.WorkedMonthSummary
import com.example.domain.model.Worker
import com.example.ui.theme.NabdBone
import com.example.ui.theme.NabdBorder
import com.example.ui.theme.NabdDeepViolet
import com.example.ui.theme.NabdMineralGray
import com.example.ui.theme.NabdObsidian
import com.example.ui.theme.NabdPurple
import com.example.ui.theme.NabdSurfaceSubtle
import com.example.ui.theme.NabdSurfaceWhite

/**
 * Archive route showing only months in which actual work occurred.
 * Rule: Shows only months in which the worker actually worked.
 */
@Composable
fun ArchivesScreen(
    workerFilter: Worker?,
    workedMonths: List<WorkedMonthSummary>,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (workerFilter != null) {
        BackHandler(onBack = onBack)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NabdBone)
            .padding(horizontal = 16.dp)
            .testTag("archives_screen")
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (workerFilter != null) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("btn_archives_back")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Retour",
                        tint = NabdObsidian
                    )
                }
            }
            Column {
                Text(
                    text = if (workerFilter != null) "Archives : ${workerFilter.fullName}" else "Archives d'atelier",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = NabdObsidian
                )
                Text(
                    text = if (workerFilter != null)
                        "Mois effectivement travaillés par ${workerFilter.firstName}"
                    else
                        "Mois d'activité enregistrés",
                    style = MaterialTheme.typography.bodySmall,
                    color = NabdMineralGray
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (workedMonths.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(NabdSurfaceWhite)
                    .border(1.dp, NabdBorder, RoundedCornerShape(16.dp))
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Archive,
                        contentDescription = null,
                        tint = NabdMineralGray,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Aucun mois de travail effectif",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = NabdObsidian
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Les mois sans pointage ni heures travaillées ne figurent pas dans cette archive.",
                        style = MaterialTheme.typography.bodySmall,
                        color = NabdMineralGray,
                        lineHeight = 18.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(workedMonths, key = { it.yearMonth }) { month ->
                    WorkedMonthCard(summary = month)
                }

                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
private fun WorkedMonthCard(summary: WorkedMonthSummary) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, NabdBorder, RoundedCornerShape(14.dp))
            .testTag("month_card_${summary.yearMonth}"),
        color = NabdSurfaceWhite,
        tonalElevation = 1.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
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
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = null,
                        tint = NabdPurple,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = summary.displayNameFrench,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = NabdObsidian
                    )
                }

                Text(
                    text = summary.grossPayFormattedTnd,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = NabdDeepViolet
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(NabdSurfaceSubtle)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = NabdDeepViolet,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "${summary.totalHoursFormatted} (${summary.workedDaysCount} j travaillés)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = NabdObsidian
                    )
                }

                if (summary.absentDaysCount > 0) {
                    Text(
                        text = "${summary.absentDaysCount} AB",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}
