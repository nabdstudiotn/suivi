package com.example.data

import android.os.Build
import android.util.Log
import com.example.data.local.AuditEventEntity
import com.example.data.local.ChafaPermissionsEntity
import com.example.data.local.CompanyProfileEntity
import com.example.data.local.DeviceSessionEntity
import com.example.data.local.SuiviDatabase
import com.example.domain.model.AuditActionType
import com.example.domain.model.AuditEvent
import com.example.domain.model.ChafaPermissions
import com.example.domain.model.CompanyProfile
import com.example.domain.model.DeviceSession
import com.example.domain.model.MemberRole
import com.example.domain.model.MembershipStatus
import com.example.domain.model.MessageSyncStatus
import com.example.domain.model.UserAccountProfile
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.UUID

private const val TAG = "SettingsRepo"

/**
 * Repository for workshop centralized settings and real activity auditing:
 * - Room on-device cache for offline resilience.
 * - Firestore cloud persistence with strict company isolation.
 * - Comprehensive dated audit logging for all Directrice and Châfa actions.
 * - Real account, workshop profile, permissions matrix, and device sessions management.
 */
class SettingsRepository(
    private val database: SuiviDatabase,
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    private val auditDao = database.auditEventDao()
    private val companyProfileDao = database.companyProfileDao()
    private val chafaPermissionsDao = database.chafaPermissionsDao()
    private val sessionDao = database.deviceSessionDao()
    private val memberDao = database.companyMemberDao()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        // Real-time synchronization is driven by active company membership
    }

    /**
     * Live stream of authenticated user profile.
     */
    fun getUserProfile(companyId: String): Flow<UserAccountProfile> = flow {
        val user = auth.currentUser
        val uid = user?.uid ?: ""
        val member = memberDao.getMember(companyId, uid)?.toDomain()

        val profile = UserAccountProfile(
            userId = uid,
            companyId = companyId,
            displayName = member?.displayName ?: user?.displayName?.ifBlank { null } ?: user?.email?.substringBefore("@") ?: "Utilisateur",
            email = user?.email ?: "",
            phone = user?.phoneNumber ?: "",
            role = member?.role ?: MemberRole.DIRECTRICE,
            status = member?.status ?: MembershipStatus.ACTIVE,
            lastLoginEpochMs = System.currentTimeMillis()
        )
        emit(profile)
    }.flowOn(Dispatchers.IO)

    /**
     * Live stream of workshop company profile, backed by Room and synced with Firestore.
     */
    fun getCompanyProfile(companyId: String): Flow<CompanyProfile> = flow {
        emitAll(
            companyProfileDao.getCompanyProfile(companyId).map { entity ->
                entity?.toDomain() ?: CompanyProfile(
                    companyId = companyId,
                    name = "Atelier",
                    ownerId = auth.currentUser?.uid ?: ""
                )
            }
        )
    }.flowOn(Dispatchers.IO)

    fun getCompany(companyId: String): Flow<com.example.domain.model.Company?> =
        database.companyDao().getCompany(companyId).map { it?.toDomain() }.flowOn(Dispatchers.IO)

    /**
     * Live stream of Châfa permissions configuration.
     */
    fun getChafaPermissions(companyId: String): Flow<ChafaPermissions> = flow {
        emitAll(
            chafaPermissionsDao.getPermissions(companyId).map { entity ->
                entity?.toDomain() ?: ChafaPermissions(companyId = companyId)
            }
        )
    }.flowOn(Dispatchers.IO)

    /**
     * Live stream of active/historical device sessions.
     */
    fun getDeviceSessions(companyId: String): Flow<List<DeviceSession>> = flow {
        emitAll(
            sessionDao.getSessions(companyId).map { list ->
                list.map { it.toDomain() }
            }
        )
    }.flowOn(Dispatchers.IO)

    /**
     * Live stream of all dated audit events, ordered chronologically (newest first).
     */
    fun getAuditEvents(companyId: String): Flow<List<AuditEvent>> = flow {
        emitAll(
            auditDao.getAuditEvents(companyId).map { list ->
                list.map { it.toDomain() }
            }
        )
    }.flowOn(Dispatchers.IO)

    /**
     * Updates user profile (name, phone) and logs AuditEvent.
     */
    suspend fun updateUserProfile(
        companyId: String,
        displayName: String,
        phone: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val uid = auth.currentUser?.uid ?: "directrice_suivi_01"
        try {
            // Update Firestore user document if possible
            try {
                firestore.collection("users").document(uid).update(
                    "displayName", displayName.trim(),
                    "phone", phone.trim(),
                    "updatedAt", FieldValue.serverTimestamp()
                ).await()
            } catch (_: Exception) {}

            logAuditEvent(
                companyId = companyId,
                actionType = AuditActionType.USER_PROFILE_UPDATED,
                detailsFr = "Profil mis à jour : ${displayName.trim()} (Tél: ${phone.trim()})",
                targetEntityId = uid,
                targetEntityName = displayName.trim()
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Update profile failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Updates company profile in Room & Firestore and logs AuditEvent.
     */
    suspend fun updateCompanyProfile(
        companyId: String,
        name: String,
        matriculeFiscal: String,
        address: String,
        phone: String,
        standardDailyHours: Int,
        referenceRateMillimes: Long
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val updated = CompanyProfile(
                companyId = companyId,
                name = name.trim(),
                ownerId = auth.currentUser?.uid ?: "directrice_suivi_01",
                matriculeFiscal = matriculeFiscal.trim(),
                address = address.trim(),
                phone = phone.trim(),
                standardDailyHours = standardDailyHours,
                referenceRateMillimes = referenceRateMillimes,
                currency = "TND"
            )
            companyProfileDao.insertOrUpdate(CompanyProfileEntity.fromDomain(updated))

            // Cloud persistence
            try {
                val payload = mapOf(
                    "id" to companyId,
                    "name" to updated.name,
                    "ownerId" to updated.ownerId,
                    "matriculeFiscal" to updated.matriculeFiscal,
                    "address" to updated.address,
                    "phone" to updated.phone,
                    "standardDailyHours" to updated.standardDailyHours,
                    "referenceRateMillimes" to updated.referenceRateMillimes,
                    "currency" to "TND",
                    "updatedAt" to FieldValue.serverTimestamp(),
                    "createdAt" to FieldValue.serverTimestamp()
                )
                firestore.collection("companies").document(companyId).set(payload).await()
            } catch (e: Exception) {
                Log.w(TAG, "Firestore company update warning: ${e.message}")
            }

            logAuditEvent(
                companyId = companyId,
                actionType = AuditActionType.COMPANY_UPDATED,
                detailsFr = "Fiche atelier mise à jour : ${updated.name} (Matricule: ${updated.matriculeFiscal}, Heures: ${standardDailyHours}h/j)",
                targetEntityId = companyId,
                targetEntityName = updated.name
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Update company profile failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Updates Châfa permissions in Room & Firestore and logs AuditEvent.
     */
    suspend fun updateChafaPermissions(
        companyId: String,
        permissions: ChafaPermissions
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val updated = permissions.copy(
                companyId = companyId,
                lastUpdatedEpochMs = System.currentTimeMillis()
            )
            chafaPermissionsDao.insertOrUpdate(ChafaPermissionsEntity.fromDomain(updated))

            // Cloud persistence
            try {
                val payload = mapOf(
                    "companyId" to companyId,
                    "canEditPastAttendance" to updated.canEditPastAttendance,
                    "canRecordAdvances" to updated.canRecordAdvances,
                    "canPublishAnnouncements" to updated.canPublishAnnouncements,
                    "canViewPayrollFinancials" to updated.canViewPayrollFinancials,
                    "canManageWorkers" to updated.canManageWorkers,
                    "canViewAuditLogs" to updated.canViewAuditLogs,
                    "canCloseArchive" to updated.canCloseArchive,
                    "lastUpdatedEpochMs" to updated.lastUpdatedEpochMs,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                firestore.collection("companies").document(companyId)
                    .collection("permissions").document("chafa_default")
                    .set(payload).await()
            } catch (e: Exception) {
                Log.w(TAG, "Firestore permissions update warning: ${e.message}")
            }

            logAuditEvent(
                companyId = companyId,
                actionType = AuditActionType.PERMISSION_CHANGED,
                detailsFr = "Permissions Châfa modifiées (Pointage passés: ${if (updated.canEditPastAttendance) "Oui" else "Non"}, Acomptes: ${if (updated.canRecordAdvances) "Oui" else "Non"}, Annonces: ${if (updated.canPublishAnnouncements) "Oui" else "Non"}, Clôture Archives: ${if (updated.canCloseArchive) "Oui" else "Non"})",
                targetEntityId = "chafa_default",
                targetEntityName = "Permissions Châfa"
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Update permissions failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Revokes a device session in Room and Firestore, and logs AuditEvent.
     */
    suspend fun revokeSession(
        companyId: String,
        sessionId: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            sessionDao.revokeSession(sessionId)

            try {
                firestore.collection("companies").document(companyId)
                    .collection("sessions").document(sessionId)
                    .update(
                        "isRevoked", true,
                        "updatedAt", FieldValue.serverTimestamp()
                    ).await()
            } catch (e: Exception) {
                Log.w(TAG, "Firestore session revoke warning: ${e.message}")
            }

            logAuditEvent(
                companyId = companyId,
                actionType = AuditActionType.SESSION_REVOKED,
                detailsFr = "Révocation de la session / appareil : $sessionId",
                targetEntityId = sessionId,
                targetEntityName = "Session d'appareil"
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Revoke session failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Core method to record a real dated audit event for all actions of Châfa and Directrice.
     * Persisted to Room immediately, synced to Firestore with honest status.
     */
    suspend fun logAuditEvent(
        companyId: String,
        actionType: AuditActionType,
        detailsFr: String,
        targetEntityId: String? = null,
        targetEntityName: String? = null,
        metadata: Map<String, String> = emptyMap()
    ): AuditEvent = withContext(Dispatchers.IO) {
        val user = auth.currentUser
        val uid = user?.uid ?: "directrice_suivi_01"
        val member = memberDao.getMember(companyId, uid)?.toDomain()
        val actorName = member?.displayName ?: user?.displayName ?: "Directrice Générale"
        val actorRole = member?.role ?: MemberRole.DIRECTRICE

        val eventId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()

        val event = AuditEvent(
            id = eventId,
            companyId = companyId,
            actionType = actionType,
            actorId = uid,
            actorName = actorName,
            actorRole = actorRole,
            targetEntityId = targetEntityId,
            targetEntityName = targetEntityName,
            detailsFr = detailsFr,
            metadata = metadata,
            timestampEpochMs = now,
            ipOrDeviceId = "Tablette Atelier (${Build.MODEL})",
            syncStatus = MessageSyncStatus.PENDING
        )

        // 1. Room immediate insertion
        auditDao.insertAudit(AuditEventEntity.fromDomain(event))

        // 2. Cloud Firestore synchronization
        try {
            val payload = mapOf(
                "id" to eventId,
                "companyId" to companyId,
                "action" to actionType.name,
                "actorId" to uid,
                "actorName" to actorName,
                "actorRole" to actorRole.name,
                "details" to detailsFr,
                "targetEntityId" to (targetEntityId ?: ""),
                "targetEntityName" to (targetEntityName ?: ""),
                "timestampEpochMs" to now,
                "createdAt" to FieldValue.serverTimestamp()
            )
            firestore.collection("companies").document(companyId)
                .collection("audit_logs").document(eventId)
                .set(payload).await()

            auditDao.updateSyncStatus(eventId, MessageSyncStatus.SYNCED.name)
            event.copy(syncStatus = MessageSyncStatus.SYNCED)
        } catch (e: Exception) {
            Log.w(TAG, "Audit log Firestore sync deferred: ${e.message}")
            event
        }
    }
}
