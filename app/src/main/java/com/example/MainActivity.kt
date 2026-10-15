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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.ui.text.style.TextOverflow
import com.example.data.ArchiveRepository
import com.example.data.AuthRepository
import com.example.data.CommunicationRepository
import com.example.data.RoomAttendanceRepository
import com.example.data.SettingsRepository
import com.example.data.local.SuiviDatabase
import com.example.domain.model.AtelierSection
import com.example.domain.model.AuthGateState
import com.example.domain.model.CompanyMember
import com.example.ui.archives.ArchivesScreen
import com.example.ui.archives.ArchivesViewModel
import com.example.ui.attendance.AttendanceViewModel
import com.example.ui.attendance.DailyAttendanceScreen
import com.example.ui.attendance.TimeEntryWheelDialog
import com.example.ui.auth.OnboardingGateScreen
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
        // Resolve the database from the active google-services.json. The old
        // Firebase Studio database id must not be used in production builds.
        val firestore = FirebaseFirestore.getInstance()
        val auth = FirebaseAuth.getInstance()

        val authRepository = AuthRepository(database, firestore, auth)
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
                val gateState by authRepository.observeAuthGateState().collectAsState(initial = AuthGateState.ResolvingMembership)
                var selectedCompanyMember by remember { mutableStateOf<CompanyMember?>(null) }

                when (val state = gateState) {
                    is AuthGateState.Unauthenticated -> {
                        selectedCompanyMember = null
                        SignInScreen(
                            authRepository = authRepository,
                            onSignInSuccess = { /* gate state auto-resolves */ }
                        )
                    }
                    is AuthGateState.ActiveWorkspace -> {
                        val member = state.membership
                        val companyId = member.companyId
                        val attendanceViewModel = viewModel<AttendanceViewModel>(
                            key = "attendance_$companyId",
                            factory = AttendanceViewModel.provideFactory(attendanceRepository, archiveRepository, settingsRepository, companyId)
                        )
                        val communicationViewModel = viewModel<CommunicationViewModel>(
                            key = "comm_$companyId",
                            factory = CommunicationViewModel.Factory(communicationRepository, companyId)
                        )
                        val settingsViewModel = viewModel<SettingsViewModel>(
                            key = "settings_$companyId",
                            factory = SettingsViewModel.Factory(settingsRepository, companyId, authRepository)
                        )
                        val archivesViewModel = viewModel<ArchivesViewModel>(
                            key = "archives_$companyId",
                            factory = ArchivesViewModel.Factory(archiveRepository, settingsRepository, attendanceRepository, companyId)
                        )

                        AppMainScaffold(
                            attendanceViewModel = attendanceViewModel,
                            communicationViewModel = communicationViewModel,
                            settingsViewModel = settingsViewModel,
                            archivesViewModel = archivesViewModel,
                            activeCompanyMember = member
                        )
                    }
                    is AuthGateState.MultipleCompanies -> {
                        if (selectedCompanyMember != null && state.memberships.any { it.companyId == selectedCompanyMember!!.companyId }) {
                            val member = selectedCompanyMember!!
                            val companyId = member.companyId
                            val attendanceViewModel = viewModel<AttendanceViewModel>(
                                key = "attendance_$companyId",
                                factory = AttendanceViewModel.provideFactory(attendanceRepository, archiveRepository, settingsRepository, companyId)
                            )
                            val communicationViewModel = viewModel<CommunicationViewModel>(
                                key = "comm_$companyId",
                                factory = CommunicationViewModel.Factory(communicationRepository, companyId)
                            )
                            val settingsViewModel = viewModel<SettingsViewModel>(
                                key = "settings_$companyId",
                                factory = SettingsViewModel.Factory(settingsRepository, companyId, authRepository)
                            )
                            val archivesViewModel = viewModel<ArchivesViewModel>(
                                key = "archives_$companyId",
                                factory = ArchivesViewModel.Factory(archiveRepository, settingsRepository, attendanceRepository, companyId)
                            )

                            AppMainScaffold(
                                attendanceViewModel = attendanceViewModel,
                                communicationViewModel = communicationViewModel,
                                settingsViewModel = settingsViewModel,
                                archivesViewModel = archivesViewModel,
                                activeCompanyMember = member
                            )
                        } else {
                            OnboardingGateScreen(
                                gateState = state,
                                authRepository = authRepository,
                                onSelectWorkspace = { selectedCompanyMember = it }
                            )
                        }
                    }
                    else -> {
                        selectedCompanyMember = null
                        OnboardingGateScreen(
                            gateState = state,
                            authRepository = authRepository,
                            onSelectWorkspace = { selectedCompanyMember = it }
                        )
                    }
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
    archivesViewModel: ArchivesViewModel,
    activeCompanyMember: CompanyMember
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
                // Header Suivi: Section Title, Workshop info & Date in French
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically
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

                        Surface(
                            color = NabdPurple.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "${activeCompanyMember.companyName} (${activeCompanyMember.role.labelFrench})",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = NabdPurple,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
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
