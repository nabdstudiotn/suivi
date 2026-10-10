package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.AttendanceRepository
import com.example.ui.attendance.AttendanceViewModel
import com.example.ui.attendance.PointageScreen
import com.example.ui.settings.SettingsScreen
import com.example.ui.theme.MatteSurfaceContainer
import com.example.ui.theme.SuiviTheme
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

class MainActivity : ComponentActivity() {
  @OptIn(ExperimentalMaterial3Api::class)
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

        Scaffold(
          topBar = {
            TopAppBar(
              title = {
                Text(
                  text = "Suivi",
                  style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                  color = TextPrimary
                )
              },
              colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background
              )
            )
          },
          bottomBar = {
            NavigationBar(
              containerColor = MatteSurfaceContainer
            ) {
              NavigationBarItem(
                selected = uiState.activeTab == "POINTAGE",
                onClick = { viewModel.setTab("POINTAGE") },
                icon = { Icon(Icons.Default.DateRange, contentDescription = "Pointage") },
                label = { Text("Pointage") },
                modifier = Modifier.testTag("nav_pointage")
              )
              NavigationBarItem(
                selected = uiState.activeTab == "EQUIPE",
                onClick = { viewModel.setTab("EQUIPE") },
                icon = { Icon(Icons.Default.Group, contentDescription = "Équipe") },
                label = { Text("Équipe") },
                modifier = Modifier.testTag("nav_equipe")
              )
              NavigationBarItem(
                selected = uiState.activeTab == "PARAMETRES",
                onClick = { viewModel.setTab("PARAMETRES") },
                icon = { Icon(Icons.Default.Settings, contentDescription = "Paramètres") },
                label = { Text("Paramètres") },
                modifier = Modifier.testTag("nav_parametres")
              )
            }
          },
          modifier = Modifier.fillMaxSize()
        ) { innerPadding ->
          Box(
            modifier = Modifier
              .fillMaxSize()
              .padding(innerPadding)
          ) {
            when (uiState.activeTab) {
              "POINTAGE" -> {
                PointageScreen(
                  selectedDate = uiState.selectedDate,
                  workers = uiState.workers,
                  onDateClick = { /* Date picker dialog will be implemented in section scope */ }
                )
              }
              "EQUIPE" -> {
                Box(
                  modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                  contentAlignment = androidx.compose.ui.Alignment.Center
                ) {
                  Text(
                    text = "Section Équipe\n(Les modifications d'Équipe suivront les règles de cadrage établies)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                  )
                }
              }
              "PARAMETRES" -> {
                SettingsScreen(syncState = uiState.syncState)
              }
            }
          }
        }
      }
    }
  }
}
