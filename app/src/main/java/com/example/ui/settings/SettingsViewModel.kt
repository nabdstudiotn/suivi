package com.example.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.AuthRepository
import com.example.data.SettingsRepository
import com.example.domain.model.AuditActionCategory
import com.example.domain.model.AuditDateFilter
import com.example.domain.model.AuditEvent
import com.example.domain.model.ChafaPermissions
import com.example.domain.model.CompanyProfile
import com.example.domain.model.DeviceSession
import com.example.domain.model.JoinRequest
import com.example.domain.model.MemberRole
import com.example.domain.model.SettingsTab
import com.example.domain.model.UserAccountProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class SettingsUiState(
    val selectedTab: SettingsTab = SettingsTab.COMPTE,
    val userProfile: UserAccountProfile? = null,
    val companyProfile: CompanyProfile? = null,
    val permissions: ChafaPermissions = ChafaPermissions(companyId = ""),
    val sessions: List<DeviceSession> = emptyList(),
    val auditEvents: List<AuditEvent> = emptyList(),
    val filteredAuditEvents: List<AuditEvent> = emptyList(),
    val pendingRequests: List<JoinRequest> = emptyList(),
    val requestToApprove: JoinRequest? = null,
    val companyCode: String = "",
    val selectedAuditCategory: AuditActionCategory = AuditActionCategory.ALL,
    val selectedDateFilter: AuditDateFilter = AuditDateFilter.TOUS,
    val auditSearchQuery: String = "",
    val feedbackMessage: String? = null,
    val isErrorFeedback: Boolean = false,
    val isLoading: Boolean = false,

    // Account Form Inputs
    val accountNameInput: String = "",
    val accountPhoneInput: String = "",

    // Company Form Inputs
    val companyNameInput: String = "",
    val matriculeInput: String = "",
    val addressInput: String = "",
    val companyPhoneInput: String = "",
    val dailyHoursInput: String = "8",
    val referenceRateInput: String = "3.500",

    // Session Revoke Dialog
    val sessionToRevoke: DeviceSession? = null
)

class SettingsViewModel(
    private val repository: SettingsRepository,
    private val companyId: String,
    private val authRepository: AuthRepository? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            repository.getUserProfile(companyId).collectLatest { profile ->
                _uiState.update { current ->
                    current.copy(
                        userProfile = profile,
                        accountNameInput = if (current.accountNameInput.isEmpty()) profile.displayName else current.accountNameInput,
                        accountPhoneInput = if (current.accountPhoneInput.isEmpty()) profile.phone else current.accountPhoneInput
                    )
                }
            }
        }

        viewModelScope.launch {
            repository.getCompanyProfile(companyId).collectLatest { company ->
                _uiState.update { current ->
                    current.copy(
                        companyProfile = company,
                        companyNameInput = if (current.companyNameInput.isEmpty()) company.name else current.companyNameInput,
                        matriculeInput = if (current.matriculeInput.isEmpty()) company.matriculeFiscal else current.matriculeInput,
                        addressInput = if (current.addressInput.isEmpty()) company.address else current.addressInput,
                        companyPhoneInput = if (current.companyPhoneInput.isEmpty()) company.phone else current.companyPhoneInput,
                        dailyHoursInput = if (current.dailyHoursInput.isEmpty()) company.standardDailyHours.toString() else current.dailyHoursInput,
                        referenceRateInput = if (current.referenceRateInput.isEmpty()) String.format(java.util.Locale.US, "%.3f", company.referenceRateMillimes / 1000.0) else current.referenceRateInput
                    )
                }
            }
        }

        viewModelScope.launch {
            repository.getCompany(companyId).collectLatest { comp ->
                if (comp != null) {
                    _uiState.update { it.copy(companyCode = comp.companyCode) }
                }
            }
        }

        viewModelScope.launch {
            repository.getChafaPermissions(companyId).collectLatest { perms ->
                _uiState.update { it.copy(permissions = perms) }
            }
        }

        viewModelScope.launch {
            repository.getDeviceSessions(companyId).collectLatest { list ->
                _uiState.update { it.copy(sessions = list) }
            }
        }

        viewModelScope.launch {
            repository.getAuditEvents(companyId).collectLatest { list ->
                _uiState.update { current ->
                    current.copy(
                        auditEvents = list,
                        filteredAuditEvents = applyAuditFilters(
                            events = list,
                            category = current.selectedAuditCategory,
                            dateFilter = current.selectedDateFilter,
                            query = current.auditSearchQuery
                        )
                    )
                }
            }
        }

        if (authRepository != null) {
            viewModelScope.launch {
                authRepository.pullPendingRequestsForCompany(companyId)
                authRepository.getPendingRequestsForCompany(companyId).collectLatest { list ->
                    _uiState.update { it.copy(pendingRequests = list) }
                }
            }
        }
    }

    fun selectTab(tab: SettingsTab) {
        _uiState.update { it.copy(selectedTab = tab, feedbackMessage = null) }
    }

    fun setAccountName(value: String) {
        _uiState.update { it.copy(accountNameInput = value) }
    }

    fun setAccountPhone(value: String) {
        _uiState.update { it.copy(accountPhoneInput = value) }
    }

    fun saveUserProfile() {
        val name = _uiState.value.accountNameInput.trim()
        val phone = _uiState.value.accountPhoneInput.trim()
        if (name.length < 2) {
            _uiState.update { it.copy(feedbackMessage = "Le nom doit comporter au moins 2 caractères.", isErrorFeedback = true) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, feedbackMessage = null) }
            val res = repository.updateUserProfile(companyId, name, phone)
            _uiState.update {
                it.copy(
                    isLoading = false,
                    feedbackMessage = if (res.isSuccess) "Profil mis à jour avec succès et consigné dans l'audit." else "Erreur lors de la mise à jour du profil.",
                    isErrorFeedback = res.isFailure
                )
            }
        }
    }

    fun setCompanyName(value: String) { _uiState.update { it.copy(companyNameInput = value) } }
    fun setMatricule(value: String) { _uiState.update { it.copy(matriculeInput = value) } }
    fun setAddress(value: String) { _uiState.update { it.copy(addressInput = value) } }
    fun setCompanyPhone(value: String) { _uiState.update { it.copy(companyPhoneInput = value) } }
    fun setDailyHours(value: String) { _uiState.update { it.copy(dailyHoursInput = value) } }
    fun setReferenceRate(value: String) { _uiState.update { it.copy(referenceRateInput = value) } }

    fun saveCompanyProfile() {
        val name = _uiState.value.companyNameInput.trim()
        val matricule = _uiState.value.matriculeInput.trim()
        val address = _uiState.value.addressInput.trim()
        val phone = _uiState.value.companyPhoneInput.trim()
        val hours = _uiState.value.dailyHoursInput.toIntOrNull() ?: 8
        val rateDinars = _uiState.value.referenceRateInput.replace(',', '.').toDoubleOrNull() ?: 3.5
        val rateMillimes = (rateDinars * 1000).toLong()

        if (name.length < 2) {
            _uiState.update { it.copy(feedbackMessage = "Le nom de l'atelier est obligatoire.", isErrorFeedback = true) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, feedbackMessage = null) }
            val res = repository.updateCompanyProfile(
                companyId = companyId,
                name = name,
                matriculeFiscal = matricule,
                address = address,
                phone = phone,
                standardDailyHours = hours,
                referenceRateMillimes = rateMillimes
            )
            _uiState.update {
                it.copy(
                    isLoading = false,
                    feedbackMessage = if (res.isSuccess) "Fiche d'atelier enregistrée et consignée dans le journal d'activité." else "Erreur lors de la mise à jour.",
                    isErrorFeedback = res.isFailure
                )
            }
        }
    }

    fun togglePermission(key: String, value: Boolean) {
        val current = _uiState.value.permissions
        val updated = when (key) {
            "canEditPastAttendance" -> current.copy(canEditPastAttendance = value)
            "canRecordAdvances" -> current.copy(canRecordAdvances = value)
            "canPublishAnnouncements" -> current.copy(canPublishAnnouncements = value)
            "canViewPayrollFinancials" -> current.copy(canViewPayrollFinancials = value)
            "canManageWorkers" -> current.copy(canManageWorkers = value)
            "canViewAuditLogs" -> current.copy(canViewAuditLogs = value)
            "canCloseArchive" -> current.copy(canCloseArchive = value)
            else -> current
        }
        _uiState.update { it.copy(permissions = updated) }
    }

    fun savePermissions() {
        val perms = _uiState.value.permissions
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, feedbackMessage = null) }
            val res = repository.updateChafaPermissions(companyId, perms)
            _uiState.update {
                it.copy(
                    isLoading = false,
                    feedbackMessage = if (res.isSuccess) "Matrice des permissions Châfa mise à jour et auditée." else "Échec de sauvegarde des permissions.",
                    isErrorFeedback = res.isFailure
                )
            }
        }
    }

    fun openRevokeDialog(session: DeviceSession) {
        _uiState.update { it.copy(sessionToRevoke = session) }
    }

    fun dismissRevokeDialog() {
        _uiState.update { it.copy(sessionToRevoke = null) }
    }

    fun confirmRevokeSession() {
        val session = _uiState.value.sessionToRevoke ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, sessionToRevoke = null) }
            val res = repository.revokeSession(companyId, session.id)
            _uiState.update {
                it.copy(
                    isLoading = false,
                    feedbackMessage = if (res.isSuccess) "Accès révoqué pour ${session.deviceName}." else "Erreur lors de la révocation.",
                    isErrorFeedback = res.isFailure
                )
            }
        }
    }

    fun selectAuditCategory(cat: AuditActionCategory) {
        _uiState.update { current ->
            current.copy(
                selectedAuditCategory = cat,
                filteredAuditEvents = applyAuditFilters(
                    events = current.auditEvents,
                    category = cat,
                    dateFilter = current.selectedDateFilter,
                    query = current.auditSearchQuery
                )
            )
        }
    }

    fun selectDateFilter(filter: AuditDateFilter) {
        _uiState.update { current ->
            current.copy(
                selectedDateFilter = filter,
                filteredAuditEvents = applyAuditFilters(
                    events = current.auditEvents,
                    category = current.selectedAuditCategory,
                    dateFilter = filter,
                    query = current.auditSearchQuery
                )
            )
        }
    }

    fun setAuditSearchQuery(query: String) {
        _uiState.update { current ->
            current.copy(
                auditSearchQuery = query,
                filteredAuditEvents = applyAuditFilters(
                    events = current.auditEvents,
                    category = current.selectedAuditCategory,
                    dateFilter = current.selectedDateFilter,
                    query = query
                )
            )
        }
    }

    fun clearFeedback() {
        _uiState.update { it.copy(feedbackMessage = null) }
    }

    private fun applyAuditFilters(
        events: List<AuditEvent>,
        category: AuditActionCategory,
        dateFilter: AuditDateFilter,
        query: String
    ): List<AuditEvent> {
        val nowTunis = LocalDate.now(ZoneId.of("Africa/Tunis"))
        val normalizedQuery = query.trim().lowercase()

        return events.filter { event ->
            // Category filter
            val matchesCategory = (category == AuditActionCategory.ALL) || (event.actionType.category == category)

            // Date filter
            val eventDateTunis = Instant.ofEpochMilli(event.timestampEpochMs)
                .atZone(ZoneId.of("Africa/Tunis"))
                .toLocalDate()

            val matchesDate = when (dateFilter) {
                AuditDateFilter.TOUS -> true
                AuditDateFilter.AUJOURDHUI -> eventDateTunis == nowTunis
                AuditDateFilter.SEMAINE -> !eventDateTunis.isBefore(nowTunis.minusDays(7))
            }

            // Search query filter
            val matchesQuery = if (normalizedQuery.isEmpty()) {
                true
            } else {
                event.detailsFr.lowercase().contains(normalizedQuery) ||
                event.actorName.lowercase().contains(normalizedQuery) ||
                (event.targetEntityName?.lowercase()?.contains(normalizedQuery) == true) ||
                event.actionType.labelFr.lowercase().contains(normalizedQuery)
            }

            matchesCategory && matchesDate && matchesQuery
        }
    }

    fun openApproveDialog(request: JoinRequest) {
        _uiState.update { it.copy(requestToApprove = request) }
    }

    fun dismissApproveDialog() {
        _uiState.update { it.copy(requestToApprove = null) }
    }

    fun approveJoinRequest(requestId: String, finalRole: MemberRole, assignedWorkerId: String? = null) {
        viewModelScope.launch {
            val actorName = _uiState.value.userProfile?.displayName ?: "Directrice"
            val actorUid = _uiState.value.userProfile?.userId ?: ""
            val res = authRepository?.approveJoinRequest(companyId, requestId, finalRole, assignedWorkerId, actorName, actorUid)
            if (res?.isSuccess == true) {
                _uiState.update { it.copy(requestToApprove = null, feedbackMessage = "Adhésion approuvée avec succès !", isErrorFeedback = false) }
            } else {
                _uiState.update { it.copy(feedbackMessage = "Erreur lors de l'approbation : ${res?.exceptionOrNull()?.message}", isErrorFeedback = true) }
            }
        }
    }

    fun refuseJoinRequest(requestId: String) {
        viewModelScope.launch {
            val actorName = _uiState.value.userProfile?.displayName ?: "Directrice"
            val actorUid = _uiState.value.userProfile?.userId ?: ""
            val res = authRepository?.refuseJoinRequest(companyId, requestId, actorName, actorUid)
            if (res?.isSuccess == true) {
                _uiState.update { it.copy(feedbackMessage = "Demande d'adhésion refusée", isErrorFeedback = false) }
            }
        }
    }

    fun signOut() {
        authRepository?.signOut()
    }

    class Factory(
        private val repository: SettingsRepository,
        private val companyId: String,
        private val authRepository: AuthRepository? = null
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SettingsViewModel(repository, companyId, authRepository) as T
        }
    }
}
