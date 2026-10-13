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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.ArchiveRepository
import com.example.data.CommunicationRepository
import com.example.data.RoomAttendanceRepository
import com.example.data.SettingsRepository
import com.example.data.local.SuiviDatabase
import com.example.domain.model.AtelierSection
import com.example.ui.archives.ArchivesScreen
import com.example.ui.archives.ArchivesViewModel
import com.example.ui.attendance.AttendanceViewModel
import com.example.ui.attendance.DailyAttendanceScreen
import com.example.ui.attendance.TimeEntryWheelDialog
import com.example.ui.auth.SignInScreen
import com.example.ui.communication.CommunicationScreen
import com.example.ui.communication.CommunicationViewModel
import com.example.ui.components.PlaceholderSectionScreen
import com.example.ui.components.SectionNavigationBar
import com.example.ui.payroll.PayrollScreen
import com.example.ui.settings.ParametresScreen
import com.example.ui.settings.SettingsViewModel
import com.example.ui.team.AddRateDialog
import com.example.ui.team.AddWorkerDialog
import com.example.ui.team.TeamScreen
import com.example.ui.team.WorkerProfileScreen
import com.example.ui.theme.NabdBone
import com.example.ui.theme.NabdBorder
import com.example.ui.theme.NabdDeepViolet
import com.example.ui.theme.NabdObsidian
import com.example.ui.theme.NabdPurple
import com.example.ui.theme.SuiviTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = SuiviDatabase.getDatabase(applicationContext)
        val firestoreDbId = getString(R.string.firestore_database_id)
        val firestore = FirebaseFirestore.getInstance(firestoreDbId)
        val auth = FirebaseAuth.getInstance()

        val attendanceRepository = RoomAttendanceRepository(database)
        val communicationRepository = CommunicationRepository(database, firestore, auth)
        val settingsRepository = SettingsRepository(database, firestore, auth)
        val archiveRepository = ArchiveRepository(
            monthClosureDao = database.monthClosureDao(),
            workerDao = database.workerDao(),
            attendanceDao = database.attendanceDao(),
            hourlyRateDao = database.hourlyRateDao(),
            settingsRepository = settingsRepository,
            firestore = firestore,
            auth = auth
        )

        setContent {
            SuiviTheme {
                var currentUser by remember { mutableStateOf(auth.currentUser) }

                DisposableEffect(Unit) {
                    val listener = FirebaseAuth.AuthStateListener {
                        currentUser = it.currentUser
                    }
                    auth.addAuthStateListener(listener)
                    onDispose {
                        auth.removeAuthStateListener(listener)
                    }
                }

                if (currentUser == null) {
                    SignInScreen(
                        onSignInSuccess = {
                            currentUser = auth.currentUser
                        }
                    )
                } else {
                    val attendanceViewModel = viewModel<AttendanceViewModel>(
                        factory = AttendanceViewModel.provideFactory(attendanceRepository)
                    )
                    val communicationViewModel = viewModel<CommunicationViewModel>(
                        factory = CommunicationViewModel.Factory(communicationRepository)
                    )
                    val settingsViewModel = viewModel<SettingsViewModel>(
                        factory = SettingsViewModel.Factory(settingsRepository)
                    )
                    val archivesViewModel = viewModel<ArchivesViewModel>(
                        factory = ArchivesViewModel.Factory(
                            archiveRepository = archiveRepository,
                            settingsRepository = settingsRepository,
                            attendanceRepository = attendanceRepository
                        )
                    )

                    AppMainScaffold(
                        attendanceViewModel = attendanceViewModel,
                        communicationViewModel = communicationViewModel,
                        settingsViewModel = settingsViewModel,
                        archivesViewModel = archivesViewModel
                    )
                }
            }
        }
    }
}

@Composable
private fun AppMainScaffold(
    attendanceViewModel: AttendanceViewModel,
    communicationViewModel: CommunicationViewModel,
    settingsViewModel: SettingsViewModel,
    archivesViewModel: ArchivesViewModel
) {
    val uiState by attendanceViewModel.uiState.collectAsState()
    val commState by communicationViewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(NabdBone)
                    .statusBarsPadding()
            ) {
                // Header Suivi: Section Title & Date in French
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
                            color = NabdObsidian
                        )
                        Text(
                            text = "  •  ${uiState.currentSection.subtitleFr}",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp
                            ),
                            color = NabdPurple
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = uiState.currentDate.displayDateFrench,
                        style = MaterialTheme.typography.bodySmall,
                        color = NabdDeepViolet
                    )
                }

                // Horizontal Section Navigation (Pointage, Équipe, Paie, Communication, Archives, Paramètres)
                SectionNavigationBar(
                    currentSection = uiState.currentSection,
                    onSectionSelected = { attendanceViewModel.selectSection(it) },
                    unreadCommunicationCount = commState.unreadAnnouncementsCount
                )
                HorizontalDivider(
                    color = NabdBorder,
                    thickness = 0.5.dp
                )
            }
        },
        containerColor = NabdBone,
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (uiState.currentSection) {
                AtelierSection.POINTAGE -> {
                    DailyAttendanceScreen(
                        state = uiState,
                        onPreviousDay = { attendanceViewModel.previousDay() },
                        onNextDay = { attendanceViewModel.nextDay() },
                        onToday = { attendanceViewModel.goToToday() },
                        onViewModeSelected = { attendanceViewModel.setViewMode(it) },
                        onWorkerClicked = { attendanceViewModel.openEditDialog(it) },
                        onWorkerClickedForDate = { w, d -> attendanceViewModel.openEditDialog(w, d) },
                        onConfirmEffacer = { attendanceViewModel.confirmEffacer() },
                        onDismissEffacer = { attendanceViewModel.dismissEffacer() },
                        onClearError = { attendanceViewModel.clearSaveError() },
                        onOpenSampleNotice = { attendanceViewModel.toggleSampleDataNotice(true) },
                        onNavigateToTeam = { attendanceViewModel.selectSection(AtelierSection.EQUIPE) }
                    )
                }
                AtelierSection.EQUIPE -> {
                    if (uiState.selectedProfileWorker != null) {
                        WorkerProfileScreen(
                            worker = uiState.selectedProfileWorker!!,
                            rateHistory = uiState.workerRateHistory,
                            onBack = { attendanceViewModel.closeWorkerProfile() },
                            onViewWorkerArchive = { attendanceViewModel.openWorkerArchive(it) },
                            onToggleActiveStatus = { attendanceViewModel.toggleWorkerActiveStatus(it) },
                            onAddRateClicked = { attendanceViewModel.openAddRateDialog(it) }
                        )
                    } else {
                        TeamScreen(
                            workers = uiState.allWorkers,
                            onWorkerClicked = { attendanceViewModel.openWorkerProfile(it) },
                            onAddWorkerClicked = { attendanceViewModel.openAddWorkerDialog() }
                        )
                    }
                }
                AtelierSection.PAIE -> {
                    PayrollScreen(
                        currentMonthIso = uiState.currentPayrollMonth,
                        workers = uiState.allWorkers,
                        breakdowns = uiState.payrollBreakdowns,
                        onRecordAdvance = { wId, amt, motif ->
                            attendanceViewModel.recordAdvance(wId, amt, motif)
                        },
                        onRecordAdjustment = { type, wId, amt, motif, note ->
                            attendanceViewModel.recordAdjustment(type, wId, amt, motif, note)
                        },
                        onRecordPayment = { wId, amt, method, note ->
                            attendanceViewModel.recordPayment(wId, amt, method, note)
                        }
                    )
                }
                AtelierSection.COMMUNICATION -> {
                    CommunicationScreen(
                        state = commState,
                        onSelectTab = { communicationViewModel.selectTab(it) },
                        onMessageInputChange = { communicationViewModel.setMessageInput(it) },
                        onSendGroupMessage = { communicationViewModel.sendGroupMessage() },
                        onOpenDirectChat = { communicationViewModel.openDirectChat(it) },
                        onCloseDirectChat = { communicationViewModel.closeDirectChat() },
                        onSendDirectMessage = { communicationViewModel.sendDirectMessage() },
                        onRetryMessage = { communicationViewModel.retryMessage(it) },
                        onOpenAnnouncementsSheet = { communicationViewModel.openAnnouncementsSheet() },
                        onCloseAnnouncementsSheet = { communicationViewModel.closeAnnouncementsSheet() },
                        onOpenCreateAnnouncementDialog = { communicationViewModel.openCreateAnnouncementDialog() },
                        onCloseCreateAnnouncementDialog = { communicationViewModel.closeCreateAnnouncementDialog() },
                        onNewAnnouncementTypeChange = { communicationViewModel.setNewAnnouncementType(it) },
                        onNewAnnouncementTitleChange = { communicationViewModel.setNewAnnouncementTitle(it) },
                        onNewAnnouncementContentChange = { communicationViewModel.setNewAnnouncementContent(it) },
                        onNewAnnouncementPriorityChange = { communicationViewModel.setNewAnnouncementPriority(it) },
                        onNewAnnouncementAudienceChange = { communicationViewModel.setNewAnnouncementAudience(it) },
                        onNewAnnouncementDurationDaysChange = { communicationViewModel.setNewAnnouncementDurationDays(it) },
                        onPublishAnnouncement = { communicationViewModel.publishAnnouncement() },
                        onCancelAnnouncement = { communicationViewModel.cancelAnnouncement(it) },
                        onOpenAnnouncementDetails = { communicationViewModel.openAnnouncementDetails(it) },
                        onCloseAnnouncementDetails = { communicationViewModel.closeAnnouncementDetails() },
                        onClearFeedbackMessage = { communicationViewModel.clearAnnouncementFeedback() }
                    )
                }
                AtelierSection.ARCHIVES -> {
                    ArchivesScreen(
                        workerFilter = uiState.selectedArchiveWorker,
                        workedMonths = uiState.workedMonthsSummaries,
                        onBack = { attendanceViewModel.closeWorkerArchive() },
                        viewModel = archivesViewModel
                    )
                }
                AtelierSection.PARAMETRES -> {
                    ParametresScreen(
                        viewModel = settingsViewModel
                    )
                }
            }

            // Dialogs
            if (uiState.selectedWorkerForEdit != null) {
                val worker = uiState.selectedWorkerForEdit!!
                val targetDate = uiState.targetDateForEdit ?: uiState.currentDate
                val currentEntry = uiState.attendanceEntries[worker.id]
                TimeEntryWheelDialog(
                    worker = worker,
                    currentEntry = currentEntry,
                    dateFrench = targetDate.displayDateFrench,
                    onDismiss = { attendanceViewModel.closeEditDialog() },
                    onSaveWorkedHours = { h, m -> attendanceViewModel.saveWorkedHours(h, m) },
                    onSetAbsent = { attendanceViewModel.setAbsent() },
                    onSetRestDay = { attendanceViewModel.setRestDay() },
                    onResetToBlank = { attendanceViewModel.resetToBlank() }
                )
            }

            if (uiState.showAddWorkerDialog) {
                AddWorkerDialog(
                    initialDraft = uiState.workerDraft,
                    existingWorkers = uiState.allWorkers,
                    onDismiss = { attendanceViewModel.closeAddWorkerDialog() },
                    onSaveDraft = { attendanceViewModel.saveWorkerDraft(it) },
                    onDiscardDraft = { attendanceViewModel.clearWorkerDraft() },
                    onConfirm = { fn, ln, role, rate, hireDate ->
                        attendanceViewModel.addWorker(fn, ln, role, rate, hireDate)
                    }
                )
            }

            if (uiState.workerForRateDialog != null) {
                AddRateDialog(
                    worker = uiState.workerForRateDialog!!,
                    onDismiss = { attendanceViewModel.closeAddRateDialog() },
                    onConfirm = { rate, effDate, motif ->
                        attendanceViewModel.updateHourlyRate(rate, effDate, motif)
                    }
                )
            }
        }
    }
}
