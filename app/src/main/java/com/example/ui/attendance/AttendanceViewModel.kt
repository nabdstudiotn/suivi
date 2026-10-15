package com.example.ui.attendance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ArchiveRepository
import com.example.data.AttendanceRepository
import com.example.data.SettingsRepository
import com.example.domain.calculation.AtelierCalculations
import com.example.domain.calculation.WorkerPayrollBreakdown
import com.example.domain.model.AdjustmentType
import com.example.domain.model.Announcement
import com.example.domain.model.ArchiveMonthStatus
import com.example.domain.model.AtelierDate
import com.example.domain.model.AtelierSection
import com.example.domain.model.AttendanceEntry
import com.example.domain.model.AttendanceStatus
import com.example.domain.model.AttendanceViewMode
import com.example.domain.model.AuditActionType
import com.example.domain.model.ChafaPermissions
import com.example.domain.model.HourlyRateRecord
import com.example.domain.model.MemberRole
import com.example.domain.model.MonthClosureRecord
import com.example.domain.model.UserAccountProfile
import com.example.domain.model.WorkedMonthSummary
import com.example.domain.model.Worker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class AttendanceUiState(
    val currentSection: AtelierSection = AtelierSection.POINTAGE,
    val currentDate: AtelierDate = AtelierDate.todayTunis(),
    val viewMode: AttendanceViewMode = AttendanceViewMode.JOUR,
    val isLoading: Boolean = false,
    val saveErrorMessage: String? = null,
    val pointageCompletionMessage: String? = null,
    val showEffacerConfirmDialog: Boolean = false,
    val workerToEffacer: Pair<Worker, AtelierDate>? = null,
    val targetDateForEdit: AtelierDate? = null,
    val workers: List<Worker> = emptyList(),
    val allWorkers: List<Worker> = emptyList(),
    val attendanceEntries: Map<String, AttendanceEntry> = emptyMap(),
    val monthAttendanceEntries: Map<String, AttendanceEntry> = emptyMap(),
    val selectedWorkerForEdit: Worker? = null,
    val selectedProfileWorker: Worker? = null,
    val selectedArchiveWorker: Worker? = null,
    val workerRateHistory: List<HourlyRateRecord> = emptyList(),
    val workedMonthsSummaries: List<WorkedMonthSummary> = emptyList(),
    val showSampleNoticeModal: Boolean = false,
    val showAddWorkerDialog: Boolean = false,
    val workerForRateDialog: Worker? = null,
    val isViewingToday: Boolean = true,
    val isViewingTomorrow: Boolean = false,
    val isTomorrowLocked: Boolean = false,
    val canGoForward: Boolean = true,
    val announcements: List<Announcement> = emptyList(),
    val payrollBreakdowns: Map<String, WorkerPayrollBreakdown> = emptyMap(),
    val currentPayrollMonth: String = AtelierDate.todayTunis().yearMonthIso,
    val workerDraft: com.example.domain.model.WorkerDraft? = null,
    val teamSearchQuery: String = "",
    val teamFilterStatus: com.example.domain.model.WorkerProfileStatus? = null,
    val currentMonthClosure: MonthClosureRecord? = null,
    val chafaPermissions: ChafaPermissions? = null,
    val currentUserProfile: UserAccountProfile? = null,
    val archiveLockWarning: String? = null
) {
    /**
     * Active operators strictly filtered by effective employment period on currentDate.
     * Rules from Section 1 (Pointage):
     * - Do not show worker before hire date.
     * - Do not show worker after cessation / archive date.
     */
    val activeWorkersForDate: List<Worker>
        get() = allWorkers.filter { it.isEmployedOn(currentDate.isoString) }

    fun activeWorkersFor(date: AtelierDate): List<Worker> =
        allWorkers.filter { it.isEmployedOn(date.isoString) }

    /**
     * Requirement 6: Pointage ordering.
     * Place workers with fewer than 5 consecutive explicit AB workdays first, alphabetically,
     * then workers with 5 or more consecutive explicit AB workdays at the bottom, alphabetically.
     * Sunday REST_DAY and À renseigner do not count as absence.
     * General lists (Équipe) remain purely alphabetical.
     */
    val pointageOrderedWorkers: List<Worker>
        get() {
            val active = activeWorkersForDate
            val (regular, persistentAb) = active.partition { worker ->
                consecutiveAbDaysFor(worker.id) < 5
            }
            val collator = java.text.Collator.getInstance(java.util.Locale.FRENCH)
            val comparator = Comparator<Worker> { a, b -> collator.compare(a.fullName, b.fullName) }
            return regular.sortedWith(comparator) + persistentAb.sortedWith(comparator)
        }

    /**
     * Calculates consecutive explicit AB workdays looking backward from currentDate.
     * Sundays (automatic REST_DAY) and unrecorded (À renseigner) do not count as absence.
     */
    fun consecutiveAbDaysFor(workerId: String): Int {
        var count = 0
        var dateCursor = currentDate
        for (i in 0 until 40) {
            val key = "${workerId}_${dateCursor.isoString}"
            val entry = if (dateCursor == currentDate) {
                attendanceEntries[workerId] ?: monthAttendanceEntries[key]
            } else {
                monthAttendanceEntries[key]
            }
            if (dateCursor.isSunday) {
                dateCursor = dateCursor.previousDay()
                continue
            }
            val status = entry?.status
            if (status == AttendanceStatus.ABSENT) {
                count++
                dateCursor = dateCursor.previousDay()
            } else if (status == AttendanceStatus.WORKED) {
                break
            } else {
                // BLANK / unrecorded: do not count as absence
                if (i > 0) {
                    break
                }
                dateCursor = dateCursor.previousDay()
            }
        }
        return count
    }

    val isCurrentMonthClosed: Boolean
        get() = currentMonthClosure?.status == ArchiveMonthStatus.CLOTURE

    val isCurrentMonthInRevision: Boolean
        get() = currentMonthClosure?.status == ArchiveMonthStatus.EN_REVISION

    val canRecordAdvances: Boolean
        get() {
            val role = currentUserProfile?.role ?: MemberRole.DIRECTRICE
            if (role == MemberRole.DIRECTRICE) return true
            return chafaPermissions?.canRecordAdvances == true
        }

    val canViewPayrollFinancials: Boolean
        get() {
            val role = currentUserProfile?.role ?: MemberRole.DIRECTRICE
            if (role == MemberRole.DIRECTRICE) return true
            return chafaPermissions?.canViewPayrollFinancials == true
        }
}

class AttendanceViewModel(
    private val repository: AttendanceRepository,
    private val archiveRepository: ArchiveRepository? = null,
    private val settingsRepository: SettingsRepository? = null,
    private val companyId: String
) : ViewModel() {

    private val today = AtelierDate.todayTunis()
    private val tomorrow = today.nextDay()

    private val _uiState = MutableStateFlow(
        AttendanceUiState(
            currentDate = today,
            isViewingToday = true,
            isViewingTomorrow = false,
            isTomorrowLocked = false,
            canGoForward = true
        )
    )
    val uiState: StateFlow<AttendanceUiState> = _uiState.asStateFlow()

    init {
        currentInstance = this

        // Observe central permissions and profile from Paramètres
        if (settingsRepository != null) {
            viewModelScope.launch {
                settingsRepository.getChafaPermissions(companyId).collectLatest { perms ->
                    _uiState.value = _uiState.value.copy(chafaPermissions = perms)
                }
            }
            viewModelScope.launch {
                settingsRepository.getUserProfile(companyId).collectLatest { profile ->
                    _uiState.value = _uiState.value.copy(currentUserProfile = profile)
                }
            }
        }

        // Collect active workers for attendance list
        viewModelScope.launch {
            repository.getActiveWorkers().collectLatest { workersList ->
                _uiState.value = _uiState.value.copy(workers = workersList)
            }
        }

        // Collect all workers for team roster
        viewModelScope.launch {
            repository.getAllWorkers().collectLatest { allList ->
                _uiState.value = _uiState.value.copy(allWorkers = allList)
            }
        }

        // Collect attendance for the active date
        observeAttendanceForActiveDate(today)

        // Observe month closure for current date
        observeMonthClosure(today.yearMonthIso)

        // Load workshop archives
        loadWorkshopArchives()

        // Observe announcements
        observeAnnouncements()
    }

    private fun observeAttendanceForActiveDate(date: AtelierDate) {
        viewModelScope.launch {
            repository.getAttendanceForDate(date.isoString).collectLatest { entries ->
                _uiState.value = _uiState.value.copy(attendanceEntries = entries)
            }
        }
        observeMonthAttendance(date.yearMonthIso)
        observeMonthClosure(date.yearMonthIso)
    }

    private fun observeMonthClosure(yearMonth: String) {
        if (archiveRepository != null) {
            viewModelScope.launch {
                archiveRepository.getMonthClosure(companyId, yearMonth).collectLatest { closure ->
                    _uiState.value = _uiState.value.copy(currentMonthClosure = closure)
                }
            }
        }
    }

    fun clearArchiveLockWarning() {
        _uiState.value = _uiState.value.copy(archiveLockWarning = null)
    }

    fun setViewMode(mode: AttendanceViewMode) {
        _uiState.value = _uiState.value.copy(viewMode = mode)
        observeMonthAttendance(_uiState.value.currentDate.yearMonthIso)
    }

    private fun observeMonthAttendance(yearMonth: String) {
        viewModelScope.launch {
            repository.getAttendanceForMonth(yearMonth).collectLatest { list ->
                val map = list.associateBy { "${it.workerId}_${it.dateIso}" }
                _uiState.value = _uiState.value.copy(monthAttendanceEntries = map)
            }
        }
    }

    fun selectSection(section: AtelierSection) {
        _uiState.value = _uiState.value.copy(
            currentSection = section,
            selectedProfileWorker = null,
            selectedArchiveWorker = null
        )
        if (section == AtelierSection.ARCHIVES) {
            loadWorkshopArchives()
        } else if (section == AtelierSection.PAIE) {
            loadPayrollForMonth(_uiState.value.currentPayrollMonth)
        }
    }

    fun previousDay() {
        val prev = _uiState.value.currentDate.previousDay()
        setDate(prev)
    }

    fun nextDay() {
        val next = _uiState.value.currentDate.nextDay()
        if (next <= tomorrow) {
            setDate(next)
        }
    }

    fun goToToday() {
        setDate(today)
    }

    fun setDate(date: AtelierDate) {
        if (date > tomorrow) return
        val isToday = (date == today)
        val isTomorrow = (date == tomorrow)
        val canForward = (date < tomorrow)

        _uiState.value = _uiState.value.copy(
            currentDate = date,
            isViewingToday = isToday,
            isViewingTomorrow = isTomorrow,
            isTomorrowLocked = isTomorrow,
            canGoForward = canForward
        )
        observeAttendanceForActiveDate(date)
    }

    fun openEditDialog(worker: Worker, date: AtelierDate = _uiState.value.currentDate) {
        // Rule: Future day (tomorrow) is strictly locked against entry
        if (date > today || date == tomorrow) return
        // Rule: Sunday in Africa/Tunis is automatic rest day, cannot be manually edited into work
        if (date.isSunday) return

        // Archive closure integrity: blocked if month is closed and not in revision
        if (_uiState.value.currentMonthClosure?.status == ArchiveMonthStatus.CLOTURE) {
            _uiState.value = _uiState.value.copy(
                archiveLockWarning = "Le mois ${date.yearMonthIso} est clôturé et certifié dans les archives. Ouvrez une révision officielle dans les Archives pour modifier le pointage."
            )
            return
        }

        // Central permissions: verify past attendance edit rights
        val role = _uiState.value.currentUserProfile?.role ?: MemberRole.DIRECTRICE
        if (date < today && role != MemberRole.DIRECTRICE && _uiState.value.chafaPermissions?.canEditPastAttendance == false) {
            _uiState.value = _uiState.value.copy(
                archiveLockWarning = "Modification des jours passés non autorisée par les droits de la Directrice (Paramètres)."
            )
            return
        }

        _uiState.value = _uiState.value.copy(
            selectedWorkerForEdit = worker,
            targetDateForEdit = date,
            saveErrorMessage = null,
            archiveLockWarning = null
        )
    }

    fun closeEditDialog() {
        _uiState.value = _uiState.value.copy(
            selectedWorkerForEdit = null,
            targetDateForEdit = null,
            saveErrorMessage = null
        )
    }

    /**
     * Requirement 4: Real repository save finishes before navigation.
     * Moves to the next worker in Pointage order (date remains fixed).
     * If this is the last worker, displays clear French completion state.
     * If save fails, remains on current worker with retry option.
     */
    fun saveWorkedHours(hours: Int, minutes: Int) {
        val worker = _uiState.value.selectedWorkerForEdit ?: return
        val date = _uiState.value.targetDateForEdit ?: _uiState.value.currentDate
        if (date > today || date == tomorrow || date.isSunday) return

        if (hours == 0 && minutes == 0) {
            _uiState.value = _uiState.value.copy(
                saveErrorMessage = "0 h 00 ne peut pas être enregistré comme temps de travail. Choisissez un état explicite ou renseignez une durée valide."
            )
            return
        }

        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, saveErrorMessage = null)
                repository.saveAttendanceEntry(
                    AttendanceEntry(
                        workerId = worker.id,
                        dateIso = date.isoString,
                        status = AttendanceStatus.WORKED,
                        hours = hours,
                        minutes = minutes
                    )
                )
                // Real save finished! Advance to next worker in Pointage order
                moveToNextWorkerOrComplete(worker, date)

                // If modified under official revision, recalculate payroll and audit event
                if (_uiState.value.isCurrentMonthInRevision) {
                    loadPayrollForMonth(date.yearMonthIso)
                    settingsRepository?.logAuditEvent(
                        companyId = companyId,
                        actionType = AuditActionType.ARCHIVE_MONTH_REVISED,
                        detailsFr = "Pointage révisé pour ${worker.fullName} le ${date.isoString} : ${hours}h ${minutes}min (Rev. #${_uiState.value.currentMonthClosure?.revisionCount ?: 1})",
                        targetEntityId = "${worker.id}_${date.isoString}",
                        targetEntityName = worker.fullName,
                        metadata = mapOf("yearMonth" to date.yearMonthIso, "hours" to hours.toString(), "minutes" to minutes.toString())
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    saveErrorMessage = "Impossible d’enregistrer. Réessayez."
                )
            } finally {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    /**
     * Requirement 4: Set AB and advance to next worker.
     */
    fun setAbsent() {
        val worker = _uiState.value.selectedWorkerForEdit ?: return
        val date = _uiState.value.targetDateForEdit ?: _uiState.value.currentDate
        if (date > today || date == tomorrow || date.isSunday) return

        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, saveErrorMessage = null)
                repository.saveAttendanceEntry(
                    AttendanceEntry(
                        workerId = worker.id,
                        dateIso = date.isoString,
                        status = AttendanceStatus.ABSENT
                    )
                )
                // Real save finished! Advance to next worker in Pointage order
                moveToNextWorkerOrComplete(worker, date)

                // If modified under official revision, recalculate payroll and audit event
                if (_uiState.value.isCurrentMonthInRevision) {
                    loadPayrollForMonth(date.yearMonthIso)
                    settingsRepository?.logAuditEvent(
                        companyId = companyId,
                        actionType = AuditActionType.ARCHIVE_MONTH_REVISED,
                        detailsFr = "Pointage marqué Absent (AB) sous révision pour ${worker.fullName} le ${date.isoString} (Rev. #${_uiState.value.currentMonthClosure?.revisionCount ?: 1})",
                        targetEntityId = "${worker.id}_${date.isoString}",
                        targetEntityName = worker.fullName,
                        metadata = mapOf("yearMonth" to date.yearMonthIso, "workerId" to worker.id, "status" to "ABSENT")
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    saveErrorMessage = "Impossible d’enregistrer. Réessayez."
                )
            } finally {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    private fun moveToNextWorkerOrComplete(currentWorker: Worker, date: AtelierDate) {
        val ordered = _uiState.value.pointageOrderedWorkers
        val currentIndex = ordered.indexOfFirst { it.id == currentWorker.id }
        if (currentIndex != -1 && currentIndex < ordered.size - 1) {
            val nextWorker = ordered[currentIndex + 1]
            _uiState.value = _uiState.value.copy(
                selectedWorkerForEdit = nextWorker,
                targetDateForEdit = date,
                saveErrorMessage = null
            )
        } else {
            // Last worker! Clear dialog, show clear French completion state and remain in Pointage
            _uiState.value = _uiState.value.copy(
                selectedWorkerForEdit = null,
                targetDateForEdit = null,
                saveErrorMessage = null,
                pointageCompletionMessage = "Pointage terminé avec succès pour toutes les opératrices du jour."
            )
        }
    }

    fun navigateNextWorker(): Boolean {
        val currentWorker = _uiState.value.selectedWorkerForEdit ?: return false
        val date = _uiState.value.targetDateForEdit ?: _uiState.value.currentDate
        val ordered = _uiState.value.pointageOrderedWorkers
        val currentIndex = ordered.indexOfFirst { it.id == currentWorker.id }
        if (currentIndex != -1 && currentIndex < ordered.size - 1) {
            _uiState.value = _uiState.value.copy(
                selectedWorkerForEdit = ordered[currentIndex + 1],
                targetDateForEdit = date,
                saveErrorMessage = null
            )
            return true
        }
        return false
    }

    fun navigatePreviousWorker(): Boolean {
        val currentWorker = _uiState.value.selectedWorkerForEdit ?: return false
        val date = _uiState.value.targetDateForEdit ?: _uiState.value.currentDate
        val ordered = _uiState.value.pointageOrderedWorkers
        val currentIndex = ordered.indexOfFirst { it.id == currentWorker.id }
        if (currentIndex > 0) {
            _uiState.value = _uiState.value.copy(
                selectedWorkerForEdit = ordered[currentIndex - 1],
                targetDateForEdit = date,
                saveErrorMessage = null
            )
            return true
        }
        return false
    }

    fun dismissPointageCompletionMessage() {
        _uiState.value = _uiState.value.copy(pointageCompletionMessage = null)
    }

    /**
     * Requirement 5: Modifier navigates to exact worker in existing Équipe route.
     */
    fun navigateToWorkerProfileInTeam(worker: Worker) {
        _uiState.value = _uiState.value.copy(
            selectedWorkerForEdit = null,
            targetDateForEdit = null,
            selectedProfileWorker = worker,
            currentSection = AtelierSection.EQUIPE
        )
        loadWorkerRateHistory(worker.id)
    }

    /**
     * Requirement 5: Supprimer du pointage deletes only the attendance record for the selected context.
     * Never deletes the worker profile, archive, payroll, rates, or other dates.
     */
    fun deleteAttendanceRecordFromPointage(worker: Worker, date: AtelierDate) {
        if (date.isSunday) return // Automatic Sunday REST_DAY cannot be deleted
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, saveErrorMessage = null)
                repository.resetAttendanceEntry(worker.id, date.isoString)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    saveErrorMessage = "Impossible de supprimer le pointage. Réessayez."
                )
            } finally {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    fun setRestDay() {
        val worker = _uiState.value.selectedWorkerForEdit ?: return
        val date = _uiState.value.targetDateForEdit ?: _uiState.value.currentDate
        if (date > today || date == tomorrow || date.isSunday) return

        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, saveErrorMessage = null)
                repository.saveAttendanceEntry(
                    AttendanceEntry(
                        workerId = worker.id,
                        dateIso = date.isoString,
                        status = AttendanceStatus.REST_DAY
                    )
                )
                closeEditDialog()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    saveErrorMessage = "Impossible d’enregistrer. Réessayez."
                )
            } finally {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    fun requestEffacer(worker: Worker, date: AtelierDate = _uiState.value.currentDate) {
        if (date > today || date == tomorrow || date.isSunday) return
        val entry = if (date == _uiState.value.currentDate) {
            _uiState.value.attendanceEntries[worker.id]
        } else {
            _uiState.value.monthAttendanceEntries["${worker.id}_${date.isoString}"]
        }
        if (entry != null && entry.status != AttendanceStatus.BLANK) {
            // Effacer requires confirmation if value was previously saved
            _uiState.value = _uiState.value.copy(
                showEffacerConfirmDialog = true,
                workerToEffacer = worker to date
            )
        } else {
            resetToBlankDirect(worker, date)
        }
    }

    fun confirmEffacer() {
        val pair = _uiState.value.workerToEffacer ?: return
        resetToBlankDirect(pair.first, pair.second)
        _uiState.value = _uiState.value.copy(
            showEffacerConfirmDialog = false,
            workerToEffacer = null
        )
    }

    fun dismissEffacer() {
        _uiState.value = _uiState.value.copy(
            showEffacerConfirmDialog = false,
            workerToEffacer = null
        )
    }

    fun resetToBlank() {
        val worker = _uiState.value.selectedWorkerForEdit ?: return
        val date = _uiState.value.targetDateForEdit ?: _uiState.value.currentDate
        requestEffacer(worker, date)
    }

    private fun resetToBlankDirect(worker: Worker, date: AtelierDate) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, saveErrorMessage = null)
                repository.resetAttendanceEntry(worker.id, date.isoString)
                closeEditDialog()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    saveErrorMessage = "Impossible d’enregistrer. Réessayez."
                )
            } finally {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    fun clearSaveError() {
        _uiState.value = _uiState.value.copy(saveErrorMessage = null)
    }

    fun markAllDayRest() {
        val dateIso = _uiState.value.currentDate.isoString
        viewModelScope.launch {
            repository.markDayAsRestDay(dateIso)
        }
    }

    fun toggleSampleDataNotice(show: Boolean) {
        _uiState.value = _uiState.value.copy(showSampleNoticeModal = show)
    }

    // Worker profile routes
    fun openWorkerProfile(worker: Worker) {
        _uiState.value = _uiState.value.copy(selectedProfileWorker = worker)
        loadWorkerRateHistory(worker.id)
    }

    fun closeWorkerProfile() {
        _uiState.value = _uiState.value.copy(selectedProfileWorker = null)
    }

    private fun loadWorkerRateHistory(workerId: String) {
        viewModelScope.launch {
            repository.getRatesForWorker(workerId).collectLatest { rates ->
                _uiState.value = _uiState.value.copy(workerRateHistory = rates)
            }
        }
    }

    // Add Worker flow
    fun openAddWorkerDialog() {
        _uiState.value = _uiState.value.copy(showAddWorkerDialog = true)
    }

    fun closeAddWorkerDialog() {
        _uiState.value = _uiState.value.copy(showAddWorkerDialog = false)
    }

    fun saveWorkerDraft(draft: com.example.domain.model.WorkerDraft) {
        _uiState.value = _uiState.value.copy(workerDraft = draft)
    }

    fun clearWorkerDraft() {
        _uiState.value = _uiState.value.copy(workerDraft = null)
    }

    fun setTeamSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(teamSearchQuery = query)
    }

    fun setTeamFilterStatus(status: com.example.domain.model.WorkerProfileStatus?) {
        _uiState.value = _uiState.value.copy(teamFilterStatus = status)
    }

    fun addWorker(
        firstName: String,
        lastName: String,
        role: String,
        hourlyRateMillimes: Long,
        hireDateIso: String
    ) {
        viewModelScope.launch {
            repository.addWorker(
                firstName = firstName.trim(),
                lastName = lastName.trim(),
                role = role.trim(),
                hourlyRateMillimes = hourlyRateMillimes,
                hireDateIso = hireDateIso.trim()
            )
            clearWorkerDraft()
            closeAddWorkerDialog()
        }
    }

    // Add Rate flow
    fun openAddRateDialog(worker: Worker) {
        _uiState.value = _uiState.value.copy(workerForRateDialog = worker)
    }

    fun closeAddRateDialog() {
        _uiState.value = _uiState.value.copy(workerForRateDialog = null)
    }

    fun updateHourlyRate(
        newRateMillimes: Long,
        effectiveDateIso: String,
        motif: String
    ) {
        val worker = _uiState.value.workerForRateDialog ?: return
        viewModelScope.launch {
            repository.updateHourlyRate(
                workerId = worker.id,
                newRateMillimes = newRateMillimes,
                effectiveDateIso = effectiveDateIso,
                motif = motif
            )
            closeAddRateDialog()
            val updatedWorker = repository.getWorkerById(worker.id).first()
            if (updatedWorker != null) {
                _uiState.value = _uiState.value.copy(selectedProfileWorker = updatedWorker)
            }
        }
    }

    // Archive routes
    fun openWorkerArchive(worker: Worker) {
        _uiState.value = _uiState.value.copy(
            currentSection = AtelierSection.ARCHIVES,
            selectedArchiveWorker = worker
        )
        viewModelScope.launch {
            val allEntries = repository.getAttendanceForWorker(worker.id).first()
            val rates = repository.getRatesForWorker(worker.id).first()
            val summaries = AtelierCalculations.calculateWorkedMonthsSummaries(
                allEntriesForWorker = allEntries,
                rateHistory = rates,
                fallbackRateMillimes = worker.currentRateMillimes
            )
            _uiState.value = _uiState.value.copy(workedMonthsSummaries = summaries)
        }
    }

    fun closeWorkerArchive() {
        _uiState.value = _uiState.value.copy(selectedArchiveWorker = null)
        loadWorkshopArchives()
    }

    private fun loadWorkshopArchives() {
        viewModelScope.launch {
            val workers = repository.getAllWorkers().first()
            val allEntries = mutableListOf<AttendanceEntry>()
            for (w in workers) {
                allEntries.addAll(repository.getAttendanceForWorker(w.id).first())
            }
            val summaries = AtelierCalculations.calculateWorkedMonthsSummaries(
                allEntriesForWorker = allEntries,
                rateHistory = emptyList(),
                fallbackRateMillimes = 6500L
            )
            _uiState.value = _uiState.value.copy(workedMonthsSummaries = summaries)
        }
    }

    fun toggleWorkerActiveStatus(worker: Worker) {
        viewModelScope.launch {
            val newStatus = !worker.isActive
            val dateIso = AtelierDate.todayTunis().isoString
            repository.setWorkerActiveStatus(worker.id, newStatus, dateIso)
            val updated = repository.getWorkerById(worker.id).first()
            if (updated != null) {
                _uiState.value = _uiState.value.copy(selectedProfileWorker = updated)
            }
        }
    }

    // Payroll methods
    fun loadPayrollForMonth(yearMonth: String) {
        observeMonthClosure(yearMonth)
        viewModelScope.launch {
            val allWorkers = repository.getAllWorkers().first()
            val monthEntries = repository.getAttendanceForMonth(yearMonth).first()
            val advances = repository.getAdvancesForMonth(yearMonth).first()
            val adjustments = repository.getAdjustmentsForMonth(yearMonth).first()
            val payments = repository.getPaymentsForMonth(yearMonth).first()

            val map = mutableMapOf<String, WorkerPayrollBreakdown>()
            for (worker in allWorkers) {
                val workerEntries = monthEntries.filter { it.workerId == worker.id }
                val workerRates = repository.getRatesForWorker(worker.id).first()
                val workerAdvances = advances.filter { it.workerId == worker.id }
                val workerAdjustments = adjustments.filter { it.workerId == worker.id || it.workerId == "ALL" }
                val workerPayments = payments.filter { it.workerId == worker.id }

                val breakdown = AtelierCalculations.calculateWorkerMonthPayroll(
                    workerId = worker.id,
                    yearMonth = yearMonth,
                    monthEntries = workerEntries,
                    rateHistory = workerRates,
                    fallbackRateMillimes = worker.currentRateMillimes,
                    advances = workerAdvances,
                    adjustments = workerAdjustments,
                    payments = workerPayments,
                    oldUnpaidBalanceMillimes = 0L,
                    worker = worker,
                    referenceDate = _uiState.value.currentDate
                )
                map[worker.id] = breakdown
            }
            _uiState.value = _uiState.value.copy(
                currentPayrollMonth = yearMonth,
                payrollBreakdowns = map
            )
        }
    }

    fun recordAdvance(workerId: String, amountMillimes: Long, motif: String) {
        if (!_uiState.value.canRecordAdvances) {
            _uiState.value = _uiState.value.copy(
                saveErrorMessage = "Enregistrement des acomptes non autorisé par les droits centraux (Paramètres)."
            )
            return
        }
        viewModelScope.launch {
            val todayIso = AtelierDate.todayTunis().isoString
            val author = _uiState.value.currentUserProfile?.displayName ?: "Gérante"
            repository.recordAdvance(
                workerId = workerId,
                dateIso = todayIso,
                amountMillimes = amountMillimes,
                motif = motif,
                author = author
            )
            loadPayrollForMonth(_uiState.value.currentPayrollMonth)
        }
    }

    fun recordAdjustment(
        type: AdjustmentType,
        workerId: String,
        amountMillimes: Long,
        motif: String,
        note: String?
    ) {
        viewModelScope.launch {
            val todayIso = AtelierDate.todayTunis().isoString
            val yearMonth = _uiState.value.currentPayrollMonth
            repository.recordAdjustment(
                type = type,
                workerId = workerId,
                yearMonth = yearMonth,
                dateIso = todayIso,
                amountMillimes = amountMillimes,
                motif = motif,
                note = note,
                author = "Gérante"
            )
            loadPayrollForMonth(yearMonth)
        }
    }

    fun recordPayment(
        workerId: String,
        amountMillimes: Long,
        method: String,
        note: String?
    ) {
        viewModelScope.launch {
            val todayIso = AtelierDate.todayTunis().isoString
            val yearMonth = _uiState.value.currentPayrollMonth
            repository.recordPayment(
                workerId = workerId,
                yearMonth = yearMonth,
                dateIso = todayIso,
                amountMillimes = amountMillimes,
                method = method,
                payer = "Gérante",
                note = note
            )
            loadPayrollForMonth(yearMonth)
        }
    }

    // Communication methods
    private fun observeAnnouncements() {
        viewModelScope.launch {
            repository.getAnnouncements().collectLatest { list ->
                _uiState.value = _uiState.value.copy(announcements = list)
            }
        }
    }

    fun createAnnouncement(
        title: String,
        content: String,
        audience: String,
        expirationDateIso: String
    ) {
        viewModelScope.launch {
            repository.createAnnouncement(
                title = title,
                content = content,
                audience = audience,
                expirationDateIso = expirationDateIso,
                author = "Gérante"
            )
        }
    }

    fun deleteAnnouncement(id: String) {
        viewModelScope.launch {
            repository.deleteAnnouncement(id)
        }
    }

    companion object {
        @Volatile
        var currentInstance: AttendanceViewModel? = null

        fun provideFactory(
            repository: AttendanceRepository,
            archiveRepository: ArchiveRepository? = null,
            settingsRepository: SettingsRepository? = null,
            companyId: String
        ): androidx.lifecycle.ViewModelProvider.Factory =
            object : androidx.lifecycle.ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                    return AttendanceViewModel(repository, archiveRepository, settingsRepository, companyId) as T
                }
            }
    }
}
