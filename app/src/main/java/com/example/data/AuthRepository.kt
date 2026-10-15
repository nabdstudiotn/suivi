package com.example.data

import android.util.Log
import com.example.data.local.CompanyEntity
import com.example.data.local.CompanyMemberEntity
import com.example.data.local.CompanyProfileEntity
import com.example.data.local.ChafaPermissionsEntity
import com.example.data.local.JoinRequestEntity
import com.example.data.local.SuiviDatabase
import com.example.data.local.UserAccountEntity
import com.example.data.local.WorkerEntity
import com.example.domain.model.AuditActionType
import com.example.domain.model.AuthGateState
import com.example.domain.model.ChafaPermissions
import com.example.domain.model.Company
import com.example.domain.model.CompanyMember
import com.example.domain.model.CompanyProfile
import com.example.domain.model.JoinRequest
import com.example.domain.model.JoinRequestStatus
import com.example.domain.model.MemberRole
import com.example.domain.model.MembershipStatus
import com.example.domain.model.UserAccount
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Locale
import java.util.UUID
import kotlin.random.Random

/**
 * Stage: Real Suivi Authentication, Company Provisioning, and Membership Gate.
 * Guarantees that no user enters the workshop workspace without a verified, ACTIVE membership.
 */
class AuthRepository(
    private val database: SuiviDatabase,
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    companion object {
        private const val TAG = "AuthRepository"
    }

    private val companyDao = database.companyDao()
    private val memberDao = database.companyMemberDao()
    private val joinRequestDao = database.joinRequestDao()
    private val userAccountDao = database.userAccountDao()
    private val companyProfileDao = database.companyProfileDao()
    private val permissionsDao = database.chafaPermissionsDao()
    private val workerDao = database.workerDao()

    val currentFirebaseUser: FirebaseUser?
        get() = auth.currentUser

    // -------------------------------------------------------------
    // Firebase Auth Operations
    // -------------------------------------------------------------

    suspend fun signInWithEmail(email: String, password: String): Result<FirebaseUser> = withContext(Dispatchers.IO) {
        try {
            val result = auth.signInWithEmailAndPassword(email.trim(), password).await()
            val user = checkNotNull(result.user) { "Échec de connexion : utilisateur introuvable" }
            syncUserAccountAndMemberships(user)
            Result.success(user)
        } catch (e: Exception) {
            Log.e(TAG, "signInWithEmail failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun signUpWithEmail(email: String, password: String, displayName: String): Result<FirebaseUser> = withContext(Dispatchers.IO) {
        try {
            val result = auth.createUserWithEmailAndPassword(email.trim(), password).await()
            val user = checkNotNull(result.user) { "Échec de création du compte" }
            user.updateProfile(UserProfileChangeRequest.Builder().setDisplayName(displayName.trim()).build()).await()
            syncUserAccountAndMemberships(user, displayName = displayName.trim())
            Result.success(user)
        } catch (e: Exception) {
            Log.e(TAG, "signUpWithEmail failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun signInWithGoogle(idToken: String): Result<FirebaseUser> = withContext(Dispatchers.IO) {
        try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val result = auth.signInWithCredential(credential).await()
            val user = checkNotNull(result.user) { "Échec de connexion Google" }
            syncUserAccountAndMemberships(user)
            Result.success(user)
        } catch (e: Exception) {
            Log.e(TAG, "signInWithGoogle failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun sendPasswordResetEmail(email: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            auth.sendPasswordResetEmail(email.trim()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "sendPasswordResetEmail failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    fun signOut() {
        auth.signOut()
    }

    // -------------------------------------------------------------
    // User Sync & Memberships Lookup
    // -------------------------------------------------------------

    suspend fun syncUserAccountAndMemberships(user: FirebaseUser, displayName: String? = null) = withContext(Dispatchers.IO) {
        val uid = user.uid
        val name = displayName?.ifBlank { null } ?: user.displayName?.ifBlank { null } ?: user.email?.substringBefore("@") ?: "Utilisateur"
        val email = user.email ?: ""

        val localUser = UserAccountEntity(
            uid = uid,
            email = email,
            displayName = name,
            phone = user.phoneNumber ?: "",
            createdAtEpochMs = System.currentTimeMillis()
        )
        userAccountDao.insertOrUpdate(localUser)

        // Firestore is the source of truth. Do not report a usable session when this fails.
        val userPayload = mapOf(
            "userId" to uid,
            "email" to email,
            "displayName" to name,
            "role" to "manager",
            "createdAt" to FieldValue.serverTimestamp()
        )
        firestore.collection("users").document(uid).set(userPayload, SetOptions.merge()).await()
        pullUserMembershipsFromCloud(uid)
    }

    suspend fun pullUserMembershipsFromCloud(uid: String) = withContext(Dispatchers.IO) {
            val snapshot = firestore.collection("users").document(uid).collection("memberships").get().await()
            val list = mutableListOf<CompanyMemberEntity>()
            for (doc in snapshot.documents) {
                val companyId = doc.getString("companyId") ?: continue
                val companyName = doc.getString("companyName") ?: "Atelier"
                val role = doc.getString("role") ?: "OUVRIERE"
                val status = doc.getString("status") ?: "ACTIVE"
                val assignedWorkerId = doc.getString("assignedWorkerId")
                val entity = CompanyMemberEntity(
                    id = "${companyId}_${uid}",
                    companyId = companyId,
                    userId = uid,
                    displayName = doc.getString("displayName") ?: "Membre",
                    role = role,
                    status = status,
                    companyName = companyName,
                    canPostGroup = (role == "DIRECTRICE"),
                    canDirectMessage = true,
                    assignedWorkerId = assignedWorkerId,
                    createdAtIso = "",
                    createdAtEpochMs = doc.getLong("createdAtEpochMs") ?: System.currentTimeMillis()
                )
                list.add(entity)
            }
            if (list.isNotEmpty()) {
                memberDao.insertMembers(list)
            }

            // Also fetch user's join requests
            val reqSnapshot = firestore.collection("users").document(uid).collection("join_requests").get().await()
            for (doc in reqSnapshot.documents) {
                val req = JoinRequestEntity(
                    id = doc.id,
                    companyId = doc.getString("companyId") ?: "",
                    companyName = doc.getString("companyName") ?: "",
                    userId = uid,
                    userEmail = doc.getString("userEmail") ?: "",
                    userFullName = doc.getString("userFullName") ?: "",
                    userPhone = doc.getString("userPhone") ?: "",
                    requestedRole = doc.getString("requestedRole") ?: "OUVRIERE",
                    note = doc.getString("note"),
                    status = doc.getString("status") ?: "PENDING",
                    reviewedByUid = doc.getString("reviewedByUid"),
                    reviewedByName = doc.getString("reviewedByName"),
                    assignedWorkerId = doc.getString("assignedWorkerId"),
                    createdAtEpochMs = doc.getLong("createdAtEpochMs") ?: System.currentTimeMillis(),
                    reviewedAtEpochMs = doc.getLong("reviewedAtEpochMs")
                )
                joinRequestDao.insertOrUpdate(req)
            }
    }

    suspend fun pullPendingRequestsForCompany(companyId: String) = withContext(Dispatchers.IO) {
            val snapshot = firestore.collection("companies").document(companyId)
                .collection("join_requests")
                .whereEqualTo("status", "PENDING")
                .get().await()
            for (doc in snapshot.documents) {
                val req = JoinRequestEntity(
                    id = doc.id,
                    companyId = companyId,
                    companyName = doc.getString("companyName") ?: "",
                    userId = doc.getString("userId") ?: "",
                    userEmail = doc.getString("userEmail") ?: "",
                    userFullName = doc.getString("userFullName") ?: "",
                    userPhone = doc.getString("userPhone") ?: "",
                    requestedRole = doc.getString("requestedRole") ?: "OUVRIERE",
                    note = doc.getString("note"),
                    status = doc.getString("status") ?: "PENDING",
                    reviewedByUid = doc.getString("reviewedByUid"),
                    reviewedByName = doc.getString("reviewedByName"),
                    assignedWorkerId = doc.getString("assignedWorkerId"),
                    createdAtEpochMs = doc.getLong("createdAtEpochMs") ?: System.currentTimeMillis(),
                    reviewedAtEpochMs = doc.getLong("reviewedAtEpochMs")
                )
                joinRequestDao.insertOrUpdate(req)
            }
    }

    // -------------------------------------------------------------
    // Company Creation (Manager / Directrice Flow)
    // -------------------------------------------------------------

    suspend fun createCompanyAndOwner(
        companyName: String,
        managerFullName: String,
        managerPhone: String,
        companyPhone: String,
        companyAddress: String = ""
    ): Result<CompanyMember> = withContext(Dispatchers.IO) {
        val user = auth.currentUser ?: return@withContext Result.failure(IllegalStateException("Session expirée. Veuillez vous reconnecter."))
        val uid = user.uid
        val email = user.email ?: ""
        try {
            require(companyName.trim().isNotEmpty()) { "Le nom de l'entreprise est obligatoire." }
            require(managerFullName.trim().isNotEmpty()) { "Le nom de la directrice est obligatoire." }
            val companyId = "comp_${UUID.randomUUID().toString().replace("-", "").take(8)}"
            val shortCode = "NABD-${Random.nextInt(1000, 9999)}"
            val now = System.currentTimeMillis()
            val company = Company(companyId, companyName.trim(), uid, managerFullName.trim(), companyPhone.trim(), shortCode, createdAtEpochMs = now)
            val member = CompanyMember(companyId, uid, managerFullName.trim(), MemberRole.DIRECTRICE, MembershipStatus.ACTIVE,
                company.name, canPostGroup = true, canDirectMessage = true, createdAtEpochMs = now)
            val profile = CompanyProfile(
                companyId = companyId,
                name = company.name,
                ownerId = uid,
                ownerName = managerFullName.trim(),
                address = companyAddress.ifBlank { "Tunis, Tunisie" },
                phone = companyPhone.trim(),
                standardDailyHours = 8,
                referenceRateMillimes = 3500L,
                currency = "TND"
            )
            val perms = ChafaPermissions(companyId, false, true, false, false, false, true, false)
            val auditId = UUID.randomUUID().toString()
            val companyRef = firestore.collection("companies").document(companyId)
            val memberRef = companyRef.collection("members").document(uid)
            val membershipRef = firestore.collection("users").document(uid).collection("memberships").document(companyId)
            val userRef = firestore.collection("users").document(uid)
            val batch = firestore.batch()
            batch.set(companyRef, mapOf(
                "id" to companyId, "name" to company.name, "ownerId" to uid,
                "ownerName" to company.ownerName, "phone" to company.phone,
                "companyCode" to shortCode, "address" to profile.address,
                "createdAt" to FieldValue.serverTimestamp(), "updatedAt" to FieldValue.serverTimestamp()))
            batch.set(memberRef, mapOf(
                "id" to member.id, "companyId" to companyId, "userId" to uid,
                "displayName" to member.displayName, "role" to member.role.name,
                "status" to member.status.name, "canPostGroup" to true,
                "canDirectMessage" to true, "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()))
            batch.set(membershipRef, mapOf(
                "companyId" to companyId, "companyName" to company.name,
                "role" to member.role.name, "status" to member.status.name,
                "displayName" to member.displayName, "createdAtEpochMs" to now,
                "updatedAt" to FieldValue.serverTimestamp()))
            batch.set(userRef, mapOf(
                "userId" to uid, "email" to email, "displayName" to managerFullName.trim(),
                "role" to "manager", "createdAt" to FieldValue.serverTimestamp()), SetOptions.merge())
            batch.set(companyRef.collection("permissions").document("chafa"), mapOf(
                "companyId" to companyId, "canEditPastAttendance" to perms.canEditPastAttendance,
                "canRecordAdvances" to perms.canRecordAdvances,
                "canPublishAnnouncements" to perms.canPublishAnnouncements,
                "canViewPayrollFinancials" to perms.canViewPayrollFinancials,
                "canManageWorkers" to perms.canManageWorkers, "canViewAuditLogs" to perms.canViewAuditLogs,
                "canCloseArchive" to perms.canCloseArchive, "lastUpdatedEpochMs" to now,
                "updatedAt" to FieldValue.serverTimestamp()))
            batch.set(companyRef.collection("audit_logs").document(auditId), mapOf(
                "id" to auditId, "companyId" to companyId,
                "actionType" to AuditActionType.COMPANY_CREATED.name, "actorId" to uid,
                "actorName" to managerFullName.trim(), "actorRole" to MemberRole.DIRECTRICE.name,
                "detailsFr" to "Création de l'entreprise ${company.name} (Code : $shortCode)",
                "timestampEpochMs" to now, "createdAt" to FieldValue.serverTimestamp()))
            batch.commit().await()

            companyDao.insertOrUpdate(CompanyEntity.fromDomain(company))
            companyProfileDao.insertOrUpdate(CompanyProfileEntity.fromDomain(profile))
            permissionsDao.insertOrUpdate(ChafaPermissionsEntity.fromDomain(perms))
            memberDao.insertOrUpdate(CompanyMemberEntity.fromDomain(member))
            userAccountDao.insertOrUpdate(UserAccountEntity(uid, email, managerFullName.trim(), managerPhone.trim(), now))
            Result.success(member)
        } catch (e: Exception) {
            Log.e(TAG, "createCompanyAndOwner failed", e)
            Result.failure(e)
        }
    }

    // -------------------------------------------------------------
    // Join Company Flow (Ouvrière / Châfa)
    // -------------------------------------------------------------

    fun searchCompanies(query: String): Flow<List<Company>> {
        return companyDao.searchCompanies(query.trim()).map { list -> list.map { it.toDomain() } }
    }

    suspend fun searchCompanyByCodeOnline(code: String): Company? = withContext(Dispatchers.IO) {
        val cleanCode = code.trim().uppercase(Locale.US)
        try {
            // Check local first
            val local = companyDao.getCompanyByCode(cleanCode)
            if (local != null) return@withContext local.toDomain()

            // Check Firestore
            val snapshot = firestore.collection("companies")
                .whereEqualTo("companyCode", cleanCode)
                .limit(1)
                .get()
                .await()

            val doc = snapshot.documents.firstOrNull() ?: return@withContext null
            val c = Company(
                id = doc.id,
                name = doc.getString("name") ?: "Atelier",
                ownerId = doc.getString("ownerId") ?: "",
                ownerName = doc.getString("ownerName") ?: "",
                phone = doc.getString("phone") ?: "",
                companyCode = cleanCode,
                createdAtEpochMs = doc.getLong("createdAtEpochMs") ?: System.currentTimeMillis()
            )
            companyDao.insertOrUpdate(CompanyEntity.fromDomain(c))
            c
        } catch (e: Exception) {
            Log.e(TAG, "searchCompanyByCodeOnline failed", e)
            throw e
        }
    }

    suspend fun submitJoinRequest(
        companyId: String,
        companyName: String,
        requestedRole: MemberRole,
        userFullName: String,
        userPhone: String,
        note: String? = null
    ): Result<JoinRequest> = withContext(Dispatchers.IO) {
        val user = auth.currentUser ?: return@withContext Result.failure(IllegalStateException("Non authentifié"))
        val uid = user.uid
        val email = user.email ?: ""
        try {
            require(requestedRole == MemberRole.OUVRIERE || requestedRole == MemberRole.CHAFE) {
                "Seules les demandes Ouvrière ou Châfa sont autorisées."
            }
            val requestId = "req_${companyId}_$uid"
            val now = System.currentTimeMillis()
            val req = JoinRequest(requestId, companyId, companyName.trim(), uid, email,
                userFullName.trim(), userPhone.trim(), requestedRole, note?.trim()?.ifBlank { null },
                JoinRequestStatus.PENDING, createdAtEpochMs = now)
            val payload = mapOf(
                "id" to requestId, "companyId" to companyId, "companyName" to req.companyName,
                "userId" to uid, "userEmail" to email, "userFullName" to req.userFullName,
                "userPhone" to req.userPhone, "requestedRole" to requestedRole.name,
                "note" to req.note, "status" to JoinRequestStatus.PENDING.name,
                "createdAtEpochMs" to now, "createdAt" to FieldValue.serverTimestamp())
            val batch = firestore.batch()
            batch.set(firestore.collection("companies").document(companyId).collection("join_requests").document(requestId), payload)
            batch.set(firestore.collection("users").document(uid).collection("join_requests").document(requestId), payload)
            batch.commit().await()
            joinRequestDao.insertOrUpdate(JoinRequestEntity.fromDomain(req))
            Result.success(req)
        } catch (e: Exception) {
            Log.e(TAG, "submitJoinRequest failed", e)
            Result.failure(e)
        }
    }

    fun getPendingRequestsForCompany(companyId: String): Flow<List<JoinRequest>> {
        return joinRequestDao.getPendingRequestsForCompany(companyId).map { list -> list.map { it.toDomain() } }
    }

    fun getRequestsForUser(userId: String): Flow<List<JoinRequest>> {
        return joinRequestDao.getRequestsForUser(userId).map { list -> list.map { it.toDomain() } }
    }

    suspend fun approveJoinRequest(
        companyId: String, requestId: String, finalRole: MemberRole, assignedWorkerId: String?,
        actorName: String, actorUid: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            require(finalRole == MemberRole.OUVRIERE || finalRole == MemberRole.CHAFE) { "Rôle final invalide." }
            val requestRef = firestore.collection("companies").document(companyId).collection("join_requests").document(requestId)
            val snapshot = requestRef.get().await()
            check(snapshot.exists()) { "Demande d'adhésion introuvable." }
            check(snapshot.getString("status") == JoinRequestStatus.PENDING.name) { "Cette demande a déjà été traitée." }
            val targetUid = snapshot.getString("userId") ?: error("Demande invalide : utilisateur absent.")
            val targetName = snapshot.getString("userFullName") ?: error("Demande invalide : nom absent.")
            val companyName = snapshot.getString("companyName") ?: "Atelier"
            val now = System.currentTimeMillis()
            val auditId = UUID.randomUUID().toString()
            val memberPayload = mapOf(
                "id" to "${companyId}_$targetUid", "companyId" to companyId, "userId" to targetUid,
                "displayName" to targetName, "role" to finalRole.name, "status" to MembershipStatus.ACTIVE.name,
                "canPostGroup" to false, "canDirectMessage" to true, "assignedWorkerId" to assignedWorkerId,
                "createdAt" to FieldValue.serverTimestamp(), "updatedAt" to FieldValue.serverTimestamp())
            val membershipPayload = mapOf(
                "companyId" to companyId, "companyName" to companyName, "role" to finalRole.name,
                "status" to MembershipStatus.ACTIVE.name, "displayName" to targetName,
                "assignedWorkerId" to assignedWorkerId, "createdAtEpochMs" to now,
                "updatedAt" to FieldValue.serverTimestamp())
            val update = mapOf("status" to JoinRequestStatus.APPROVED.name, "reviewedByUid" to actorUid,
                "reviewedByName" to actorName, "reviewedAtEpochMs" to now, "assignedWorkerId" to assignedWorkerId,
                "finalRole" to finalRole.name, "updatedAt" to FieldValue.serverTimestamp())
            val audit = mapOf("id" to auditId, "companyId" to companyId,
                "actionType" to AuditActionType.JOIN_REQUEST_APPROVED.name, "actorId" to actorUid,
                "actorName" to actorName, "actorRole" to MemberRole.DIRECTRICE.name,
                "detailsFr" to "Adhésion approuvée pour $targetName en qualité de ${finalRole.labelFrench}",
                "timestampEpochMs" to now, "createdAt" to FieldValue.serverTimestamp())
            val batch = firestore.batch()
            batch.update(requestRef, update)
            batch.update(firestore.collection("users").document(targetUid).collection("join_requests").document(requestId), update)
            batch.set(firestore.collection("companies").document(companyId).collection("members").document(targetUid), memberPayload)
            batch.set(firestore.collection("users").document(targetUid).collection("memberships").document(companyId), membershipPayload)
            batch.set(firestore.collection("companies").document(companyId).collection("audit_logs").document(auditId), audit)
            batch.commit().await()

            joinRequestDao.updateStatus(requestId, JoinRequestStatus.APPROVED.name, actorUid, actorName, now, assignedWorkerId)
            val member = CompanyMember(companyId, targetUid, targetName, finalRole, MembershipStatus.ACTIVE,
                companyName, canPostGroup = false, canDirectMessage = true, assignedWorkerId = assignedWorkerId, createdAtEpochMs = now)
            memberDao.insertOrUpdate(CompanyMemberEntity.fromDomain(member))
            if (assignedWorkerId == null) {
                val names = targetName.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
                val first = names.firstOrNull() ?: targetName
                val last = names.drop(1).joinToString(" ").ifBlank { "Atelier" }
                workerDao.insertOrUpdate(WorkerEntity(UUID.randomUUID().toString(), first, last,
                    "${first.take(1)}${last.take(1)}".uppercase(), 3500L,
                    if (finalRole == MemberRole.CHAFE) "Superviseuse (Châfa)" else "Couturière",
                    true, java.time.LocalDate.now().toString()))
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "approveJoinRequest failed", e)
            Result.failure(e)
        }
    }

    suspend fun refuseJoinRequest(companyId: String, requestId: String, actorName: String, actorUid: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val requestRef = firestore.collection("companies").document(companyId).collection("join_requests").document(requestId)
            val snapshot = requestRef.get().await()
            check(snapshot.exists()) { "Demande d'adhésion introuvable." }
            check(snapshot.getString("status") == JoinRequestStatus.PENDING.name) { "Cette demande a déjà été traitée." }
            val targetUid = snapshot.getString("userId") ?: error("Demande invalide : utilisateur absent.")
            val now = System.currentTimeMillis()
            val update = mapOf("status" to JoinRequestStatus.REFUSED.name, "reviewedByUid" to actorUid,
                "reviewedByName" to actorName, "reviewedAtEpochMs" to now, "updatedAt" to FieldValue.serverTimestamp())
            val batch = firestore.batch()
            batch.update(requestRef, update)
            batch.update(firestore.collection("users").document(targetUid).collection("join_requests").document(requestId), update)
            batch.commit().await()
            joinRequestDao.updateStatus(requestId, JoinRequestStatus.REFUSED.name, actorUid, actorName, now, null)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "refuseJoinRequest failed", e)
            Result.failure(e)
        }
    }

    suspend fun updateMemberStatus(companyId: String, targetUserId: String, newStatus: MembershipStatus,
        actorName: String, actorUid: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val now = System.currentTimeMillis()
            val update = mapOf("status" to newStatus.name, "updatedAt" to FieldValue.serverTimestamp())
            val auditId = UUID.randomUUID().toString()
            val audit = mapOf("id" to auditId, "companyId" to companyId,
                "actionType" to (if (newStatus == MembershipStatus.SUSPENDED) AuditActionType.MEMBER_SUSPENDED else AuditActionType.MEMBER_REMOVED).name,
                "actorId" to actorUid, "actorName" to actorName, "actorRole" to MemberRole.DIRECTRICE.name,
                "detailsFr" to "Statut du membre $targetUserId changé en ${newStatus.labelFrench}",
                "timestampEpochMs" to now, "createdAt" to FieldValue.serverTimestamp())
            val batch = firestore.batch()
            batch.update(firestore.collection("companies").document(companyId).collection("members").document(targetUserId), update)
            batch.update(firestore.collection("users").document(targetUserId).collection("memberships").document(companyId), update)
            batch.set(firestore.collection("companies").document(companyId).collection("audit_logs").document(auditId), audit)
            batch.commit().await()
            memberDao.updateMemberStatus(companyId, targetUserId, newStatus.name)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "updateMemberStatus failed", e)
            Result.failure(e)
        }
    }

    // -------------------------------------------------------------
    // Auth Gate Resolution
    // -------------------------------------------------------------

    fun observeAuthGateState(): Flow<AuthGateState> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { fbAuth ->
            trySend(fbAuth.currentUser)
        }
        auth.addAuthStateListener(listener)
        trySend(auth.currentUser)
        awaitClose { auth.removeAuthStateListener(listener) }
    }.flatMapLatest { user ->
        if (user == null) {
            flowOf<AuthGateState>(AuthGateState.Unauthenticated)
        } else {
            flow<AuthGateState> {
                emit(AuthGateState.ResolvingMembership)
                // Pull cloud memberships into Room
                pullUserMembershipsFromCloud(user.uid)

                val uid = user.uid
                val userAcc = UserAccount(
                    uid = uid,
                    email = user.email ?: "",
                    displayName = user.displayName?.ifBlank { null } ?: user.email?.substringBefore("@") ?: "Utilisateur",
                    phone = user.phoneNumber ?: ""
                )

                val membershipsFlow = memberDao.getMembershipsForUser(uid).map { list -> list.map { it.toDomain() } }
                val joinRequestsFlow = joinRequestDao.getRequestsForUser(uid).map { list -> list.map { it.toDomain() } }

                emitAll(
                    combine(membershipsFlow, joinRequestsFlow) { memberships, requests ->
                        val activeList = memberships.filter { it.status == MembershipStatus.ACTIVE }
                        val suspended = memberships.firstOrNull { it.status == MembershipStatus.SUSPENDED || it.status == MembershipStatus.REMOVED }
                        val pendingReq = requests.firstOrNull { it.status == JoinRequestStatus.PENDING }

                        when {
                            activeList.size > 1 -> AuthGateState.MultipleCompanies(userAcc, activeList)
                            activeList.size == 1 -> AuthGateState.ActiveWorkspace(userAcc, activeList.first())
                            suspended != null -> AuthGateState.Blocked(userAcc, suspended, suspended.status)
                            pendingReq != null -> AuthGateState.PendingApproval(userAcc, pendingReq)
                            else -> AuthGateState.NoCompany(userAcc)
                        }
                    }
                )
            }
        }
    }.flowOn(Dispatchers.IO)
}
