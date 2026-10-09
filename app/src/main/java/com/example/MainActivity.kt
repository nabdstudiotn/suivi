package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.RoomAttendanceRepository
import com.example.data.local.SuiviDatabase
import com.example.domain.model.AtelierSection
import com.example.ui.archives.ArchivesScreen
import com.example.ui.attendance.AttendanceViewModel
import com.example.ui.attendance.DailyAttendanceScreen
import com.example.ui.attendance.TimeEntryWheelDialog
import com.example.ui.communication.CommunicationScreen
import com.example.ui.components.PlaceholderSectionScreen
import com.example.ui.components.SampleDataNoticeModal
import com.example.ui.components.SectionNavigationBar
import com.example.ui.payroll.PayrollScreen
import com.example.ui.team.AddRateDialog
import com.example.ui.team.AddWorkerDialog
import com.example.ui.team.TeamScreen
import com.example.ui.team.WorkerProfileScreen
import com.example.ui.theme.GestionAtelierTheme
import com.example.ui.theme.NabdBone
import com.example.ui.theme.NabdDeepViolet
import com.example.ui.theme.NabdMineralGray
import com.example.ui.theme.NabdObsidian
import com.example.ui.theme.NabdPurple
import com.example.ui.theme.NabdSurfaceSubtle
import com.example.ui.theme.NabdSurfaceWhite

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.FirestoreAttendanceRepository
import com.example.ui.auth.SignInScreen
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

internal fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { auth ->
        trySend(auth.currentUser)
    }
    addAuthStateListener(listener)
    awaitClose { removeAuthStateListener(listener) }
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            GestionAtelierTheme {
                val auth = remember { FirebaseAuth.getInstance() }
                val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)

                val user = currentUser
                if (user == null) {
                    SignInScreen(
                        onSignInSuccess = {
                            // Handled reactively by authStateFlow
                        }
                    )
                } else {
                    val databaseId = stringResource(R.string.firestore_database_id)
                    val firestore = remember(databaseId) {
                        FirebaseFirestore.getInstance(databaseId)
                    }
                    val repository = remember(firestore, user.uid) {
                        FirestoreAttendanceRepository(firestore, auth)
                    }
                    val viewModel: AttendanceViewModel = viewModel(
                        key = user.uid,
                        factory = AttendanceViewModel.provideFactory(repository)
                    )

                    SuiviApp(
                        viewModel = viewModel,
                        currentUser = user,
                        onSignOut = {
                            auth.signOut()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun SuiviApp(
    viewModel: AttendanceViewModel,
    currentUser: FirebaseUser? = null,
    onSignOut: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(NabdBone),
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(NabdSurfaceWhite)
                    .windowInsetsPadding(WindowInsets.statusBars)
            ) {
                // Header with Product Name "Suivi" & Screen Title "Pointage du jour"
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = NabdSurfaceWhite
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Suivi",
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NabdObsidian,
                                    modifier = Modifier.testTag("app_brand_title")
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = when (state.currentSection) {
                                        AtelierSection.POINTAGE -> "• Pointage du jour"
                                        AtelierSection.EQUIPE -> "• Équipe d'atelier"
                                        AtelierSection.PAIE -> "• Paie & Règlements"
                                        AtelierSection.COMMUNICATION -> "• Communication"
                                        AtelierSection.ARCHIVES -> "• Archives de travail"
                                        AtelierSection.PARAMETRES -> "• Paramètres"
                                    },
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = NabdPurple
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // Cloud sync indicator
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(NabdSurfaceSubtle)
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.CloudDone,
                                            contentDescription = "Synchronisé avec Firebase",
                                            tint = NabdDeepViolet,
                                            modifier = Modifier.padding(end = 4.dp)
                                        )
                                        Text(
                                            text = "Firebase Cloud",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = NabdDeepViolet
                                        )
                                    }
                                }

                                if (currentUser != null) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    IconButton(
                                        onClick = onSignOut,
                                        modifier = Modifier.testTag("sign_out_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.Logout,
                                            contentDescription = "Déconnexion",
                                            tint = NabdObsidian
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = state.currentDate.formattedFrench,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = NabdMineralGray
                        )
                    }
                }

                // Section Navigation Tabs: Pointage, Équipe, Paie, Communication, Archives, Paramètres
                SectionNavigationBar(
                    currentSection = state.currentSection,
                    onSectionSelected = { section -> viewModel.selectSection(section) }
                )
            }
        },
        bottomBar = {
            Box(modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars))
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (state.currentSection) {
                AtelierSection.POINTAGE -> {
                    DailyAttendanceScreen(
                        state = state,
                        onPreviousDay = { viewModel.previousDay() },
                        onNextDay = { viewModel.nextDay() },
                        onToday = { viewModel.goToToday() },
                        onViewModeSelected = { mode -> viewModel.setViewMode(mode) },
                        onWorkerClicked = { worker -> viewModel.openEditDialog(worker) },
                        onWorkerClickedForDate = { worker, date -> viewModel.openEditDialog(worker, date) },
                        onConfirmEffacer = { viewModel.confirmEffacer() },
                        onDismissEffacer = { viewModel.dismissEffacer() },
                        onClearError = { viewModel.clearSaveError() },
                        onOpenSampleNotice = { viewModel.toggleSampleDataNotice(true) },
                        onNavigateToTeam = { viewModel.selectSection(AtelierSection.EQUIPE) }
                    )
                }

                AtelierSection.EQUIPE -> {
                    if (state.selectedProfileWorker != null) {
                        WorkerProfileScreen(
                            worker = state.selectedProfileWorker!!,
                            rateHistory = state.workerRateHistory,
                            onBack = { viewModel.closeWorkerProfile() },
                            onViewWorkerArchive = { worker -> viewModel.openWorkerArchive(worker) },
                            onToggleActiveStatus = { worker -> viewModel.toggleWorkerActiveStatus(worker) },
                            onAddRateClicked = { worker -> viewModel.openAddRateDialog(worker) }
                        )
                    } else {
                        TeamScreen(
                            workers = state.allWorkers,
                            onWorkerClicked = { worker -> viewModel.openWorkerProfile(worker) },
                            onAddWorkerClicked = { viewModel.openAddWorkerDialog() }
                        )
                    }
                }

                AtelierSection.ARCHIVES -> {
                    ArchivesScreen(
                        workerFilter = state.selectedArchiveWorker,
                        workedMonths = state.workedMonthsSummaries,
                        onBack = { viewModel.closeWorkerArchive() }
                    )
                }

                AtelierSection.PAIE -> {
                    PayrollScreen(
                        currentMonthIso = state.currentPayrollMonth,
                        workers = state.allWorkers,
                        breakdowns = state.payrollBreakdowns,
                        onRecordAdvance = { workerId, amount, motif ->
                            viewModel.recordAdvance(workerId, amount, motif)
                        },
                        onRecordAdjustment = { type, workerId, amount, motif, note ->
                            viewModel.recordAdjustment(type, workerId, amount, motif, note)
                        },
                        onRecordPayment = { workerId, amount, method, note ->
                            viewModel.recordPayment(workerId, amount, method, note)
                        }
                    )
                }

                AtelierSection.COMMUNICATION -> {
                    CommunicationScreen(
                        announcements = state.announcements,
                        onCreateAnnouncement = { title, content, audience, exp ->
                            viewModel.createAnnouncement(title, content, audience, exp)
                        },
                        onDeleteAnnouncement = { id ->
                            viewModel.deleteAnnouncement(id)
                        }
                    )
                }

                else -> {
                    PlaceholderSectionScreen(
                        section = state.currentSection,
                        onBackToPointage = { viewModel.selectSection(AtelierSection.POINTAGE) }
                    )
                }
            }

            // Edit Attendance Dialog (Time wheel, AB, Repos, Reset)
            state.selectedWorkerForEdit?.let { worker ->
                val targetDate = state.targetDateForEdit ?: state.currentDate
                val currentEntry = if (targetDate == state.currentDate) {
                    state.attendanceEntries[worker.id]
                } else {
                    state.monthAttendanceEntries["${worker.id}_${targetDate.isoString}"]
                }
                TimeEntryWheelDialog(
                    worker = worker,
                    currentEntry = currentEntry,
                    dateFrench = targetDate.shortFrench,
                    onDismiss = { viewModel.closeEditDialog() },
                    onSaveWorkedHours = { h, m -> viewModel.saveWorkedHours(h, m) },
                    onSetAbsent = { viewModel.setAbsent() },
                    onSetRestDay = { viewModel.setRestDay() },
                    onResetToBlank = { viewModel.resetToBlank() }
                )
            }

            // Real Add Worker Dialog with Draft & Duplicate Review
            if (state.showAddWorkerDialog) {
                AddWorkerDialog(
                    initialDraft = state.workerDraft,
                    existingWorkers = state.allWorkers,
                    onDismiss = { viewModel.closeAddWorkerDialog() },
                    onSaveDraft = { draft -> viewModel.saveWorkerDraft(draft) },
                    onDiscardDraft = { viewModel.clearWorkerDraft() },
                    onConfirm = { fn, ln, r, rm, hd -> viewModel.addWorker(fn, ln, r, rm, hd) }
                )
            }

            // Real Add Rate Dialog
            state.workerForRateDialog?.let { worker ->
                AddRateDialog(
                    worker = worker,
                    onDismiss = { viewModel.closeAddRateDialog() },
                    onConfirm = { rm, ed, m -> viewModel.updateHourlyRate(rm, ed, m) }
                )
            }

            // Real-only policy & Transparency Status Modal
            if (state.showSampleNoticeModal) {
                SampleDataNoticeModal(
                    onDismiss = { viewModel.toggleSampleDataNotice(false) }
                )
            }
        }
    }
}
