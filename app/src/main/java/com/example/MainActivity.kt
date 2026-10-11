package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.AttendanceRepository
import com.example.ui.attendance.AttendanceViewModel
import com.example.ui.attendance.PointageScreen
import com.example.ui.components.SectionPlaceholderScreen
import com.example.ui.components.SuiviSection
import com.example.ui.components.TopSectionTabs
import com.example.ui.settings.SettingsScreen
import com.example.ui.theme.*
import java.time.format.DateTimeFormatter
import java.util.Locale

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    val repository = AttendanceRepository(applicationContext)

    setContent {
      SuiviTheme {
        val viewModel = viewModel<AttendanceViewModel> {
          AttendanceViewModel(repository)
        }
        val uiState by viewModel.uiState.collectAsState()

        val frenchDateFormatter = DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", Locale.FRENCH)
        val headerDateText = uiState.today.format(frenchDateFormatter).replaceFirstChar { it.uppercase() }

        // Scaffold structure:
        // topBar: Header Suivi + Top Section Tabs (Horizontal, 6 sections)
        // content: Current section
        // bottomBar: None (strictly NO bottom navigation)
        Scaffold(
          topBar = {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .background(BoneBackground)
                .statusBarsPadding()
            ) {
              // Header Suivi:
              // Left: Suivi • Current Section Title in Purple
              // Subtitle: Today's date in French
              Column(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(horizontal = 16.dp, vertical = 10.dp)
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.Start
                ) {
                  Text(
                    text = "Suivi",
                    style = MaterialTheme.typography.titleLarge.copy(
                      fontWeight = FontWeight.Bold,
                      fontSize = 20.sp
                    ),
                    color = TextObsidian
                  )
                  Text(
                    text = "  •  ${uiState.selectedSection.subtitleFr}",
                    style = MaterialTheme.typography.titleSmall.copy(
                      fontWeight = FontWeight.SemiBold,
                      fontSize = 15.sp
                    ),
                    color = PurpleBrand
                  )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                  text = headerDateText,
                  style = MaterialTheme.typography.bodySmall,
                  color = TextMuted
                )
              }

              // Top Section Navigation (Horizontal tabs: Pointage, Équipe, Paie, Communication, Archives, Paramètres)
              TopSectionTabs(
                selectedSection = uiState.selectedSection,
                onSectionSelected = { viewModel.setSection(it) }
              )
              HorizontalDivider(
                color = BorderSubtle,
                thickness = 0.5.dp
              )
            }
          },
          bottomBar = {
            // Strictly NO bottom navigation bar per user instructions
          },
          containerColor = BoneBackground,
          modifier = Modifier.fillMaxSize()
        ) { innerPadding ->
          Box(
            modifier = Modifier
              .fillMaxSize()
              .padding(innerPadding)
          ) {
            when (uiState.selectedSection) {
              SuiviSection.POINTAGE -> {
                PointageScreen(
                  selectedDate = uiState.selectedDate,
                  workers = uiState.workers,
                  onPreviousDay = { viewModel.selectPreviousDay() },
                  onNextDay = { viewModel.selectNextDay() },
                  canGoNext = uiState.canGoNext,
                  isTomorrowLocked = uiState.isTomorrow
                )
              }
              SuiviSection.EQUIPE -> {
                SectionPlaceholderScreen(section = SuiviSection.EQUIPE)
              }
              SuiviSection.PAIE -> {
                SectionPlaceholderScreen(section = SuiviSection.PAIE)
              }
              SuiviSection.COMMUNICATION -> {
                SectionPlaceholderScreen(section = SuiviSection.COMMUNICATION)
              }
              SuiviSection.ARCHIVES -> {
                SectionPlaceholderScreen(section = SuiviSection.ARCHIVES)
              }
              SuiviSection.PARAMETRES -> {
                SettingsScreen(syncState = uiState.syncState)
              }
            }
          }
        }
      }
    }
  }
}
