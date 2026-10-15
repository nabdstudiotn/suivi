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

        // Sync user doc in Firestore
        try {
            val userPayload = mapOf(
                "userId" to uid,
                "email" to email,
                "displayName" to name,
                "role" to "manager",
                "createdAt" to FieldValue.serverTimestamp()
            )
            firestore.collection("users").document(uid).set(userPayload, SetOptions.merge()).await()
        } catch (e: Exception) {
            Log.w(TAG, "Firestore user profile sync warning: ${e.message}")
        }

        // Pull memberships from Firestore
        pullUserMembershipsFromCloud(uid)
    }

    suspend fun pullUserMembershipsFromCloud(uid: String) = withContext(Dispatchers.IO) {
        try {
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
        } catch (e: Exception) {
            Log.w(TAG, "pullUserMembershipsFromCloud warning: ${e.message}")
        }
    }

    suspend fun pullPendingRequestsForCompany(companyId: String) = withContext(Dispatchers.IO) {
        try {
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
        } catch (e: Exception) {
            Log.w(TAG, "pullPendingRequestsForCompany warning: ${e.message}")
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
            // Generate unique random company ID and safe 8-char short code
            val randomIdPart = UUID.randomUUID().toString().replace("-", "").take(8)
            val companyId = "comp_$randomIdPart"
            val shortCode = "NABD-${Random.nextInt(1000, 9999)}"

            val now = System.currentTimeMillis()

            // 1. Company Record
            val company = Company(
                id = companyId,
                name = companyName.trim(),
                ownerId = uid,
                ownerName = managerFullName.trim(),
                phone = companyPhone.trim(),
                companyCode = shortCode,
                createdAtEpochMs = now
            )
            companyDao.insertOrUpdate(CompanyEntity.fromDomain(company))

            // 2. Company Profile
            val profile = CompanyProfile(
                companyId = companyId,
                name = companyName.trim(),
                ownerId = uid,
                ownerName = managerFullName.trim(),
                address = companyAddress.ifBlank { "Tunis, Tunisie" },
                phone = companyPhone.trim(),
                standardDailyHours = 8,
                referenceRateMillimes = 3500L,
                currency = "TND"
            )
            companyProfileDao.insertOrUpdate(CompanyProfileEntity.fromDomain(profile))

            // 3. Central Permissions for company
            val perms = ChafaPermissions(
                companyId = companyId,
                canEditPastAttendance = false,
                canRecordAdvances = true,
                canPublishAnnouncements = false,
                canViewPayrollFinancials = false,
                canManageWorkers = false,
                canViewAuditLogs = true,
                canCloseArchive = false
            )
            permissionsDao.insertOrUpdate(ChafaPermissionsEntity.fromDomain(perms))

            // 4. Membership: Owner is ACTIVE DIRECTRICE
            val member = CompanyMember(
                companyId = companyId,
                userId = uid,
                displayName = managerFullName.trim(),
                role = MemberRole.DIRECTRICE,
                status = MembershipStatus.ACTIVE,
                companyName = companyName.trim(),
                canPostGroup = true,
                canDirectMessage = true,
                createdAtEpochMs = now
            )
            memberDao.insertOrUpdate(CompanyMemberEntity.fromDomain(member))

            // 5. Update User Account
            userAccountDao.insertOrUpdate(UserAccountEntity(
                uid = uid,
                email = email,
                displayName = managerFullName.trim(),
                phone = managerPhone.trim(),
                createdAtEpochMs = now
            ))

            // 6. Firestore synchronization
            try {
                // Save Company
                val companyPayload = mapOf(
                    "id" to companyId,
                    "name" to company.name,
                    "ownerId" to uid,
                    "ownerName" to company.ownerName,
                    "phone" to company.phone,
                    "companyCode" to company.companyCode,
                    "createdAt" to FieldValue.serverTimestamp()
                )
                firestore.collection("companies").document(companyId).set(companyPayload).await()

                // Save Member in Company
                val memberPayload = mapOf(
                    "id" to member.id,
                    "companyId" to companyId,
                    "userId" to uid,
                    "displayName" to member.displayName,
                    "role" to member.role.name,
                    "status" to member.status.name,
                    "canPostGroup" to member.canPostGroup,
                    "canDirectMessage" to member.canDirectMessage,
                    "createdAtEpochMs" to now,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                firestore.collection("companies").document(companyId)
                    .collection("members").document(uid).set(memberPayload).await()

                // Save Membership in User's directory
                val userMembershipPayload = mapOf(
                    "companyId" to companyId,
                    "companyName" to company.name,
                    "role" to member.role.name,
                    "status" to member.status.name,
                    "displayName" to member.displayName,
                    "createdAtEpochMs" to now,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                firestore.collection("users").document(uid)
                    .collection("memberships").document(companyId).set(userMembershipPayload).await()

                // Permissions
                val permsPayload = mapOf(
                    "companyId" to companyId,
                    "canEditPastAttendance" to false,
                    "canRecordAdvances" to true,
                    "canPublishAnnouncements" to false,
                    "canViewPayroll" to false,
                    "canManageWorkers" to false,
                    "canViewAuditLogs" to true,
                    "canCloseArchive" to false,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                firestore.collection("companies").document(companyId)
                    .collection("permissions").document("chafa").set(permsPayload).await()

                // Audit event
                val auditPayload = mapOf(
                    "id" to UUID.randomUUID().toString(),
                    "companyId" to companyId,
                    "actionType" to AuditActionType.COMPANY_CREATED.name,
                    "actorId" to uid,
                    "actorName" to managerFullName.trim(),
                    "actorRole" to MemberRole.DIRECTRICE.name,
                    "detailsFr" to "Création de l'entreprise ${company.name} par $managerFullName (Code d'invitation : $shortCode)",
                    "timestampEpochMs" to now,
                    "createdAt" to FieldValue.serverTimestamp()
                )
                firestore.collection("companies").document(companyId)
                    .collection("audit_logs").document(auditPayload["id"] as String).set(auditPayload).await()
            } catch (e: Exception) {
                Log.w(TAG, "Firestore company creation sync warning: ${e.message}")
            }

            Result.success(member)
        } catch (e: Exception) {
            Log.e(TAG, "createCompanyAndOwner failed: ${e.message}", e)
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
            Log.w(TAG, "searchCompanyByCodeOnline warning: ${e.message}")
            null
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
        val requestId = "req_${companyId}_$uid"
        val now = System.currentTimeMillis()

        try {
            val req = JoinRequest(
                id = requestId,
                companyId = companyId,
                companyName = companyName,
                userId = uid,
                userEmail = email,
                userFullName = userFullName.trim(),
                userPhone = userPhone.trim(),
                requestedRole = requestedRole,
                note = note?.trim()?.ifBlank { null },
                status = JoinRequestStatus.PENDING,
                createdAtEpochMs = now
            )

            // Save in Room
            joinRequestDao.insertOrUpdate(JoinRequestEntity.fromDomain(req))

            // Sync with Firestore
            try {
                val payload = mapOf(
                    "id" to requestId,
                    "companyId" to companyId,
                    "companyName" to companyName,
                    "userId" to uid,
                    "userEmail" to email,
                    "userFullName" to req.userFullName,
                    "userPhone" to req.userPhone,
                    "requestedRole" to requestedRole.name,
                    "note" to req.note,
                    "status" to "PENDING",
                    "createdAtEpochMs" to now,
                    "createdAt" to FieldValue.serverTimestamp()
                )

                // Save in company's join_requests
                firestore.collection("companies").document(companyId)
                    .collection("join_requests").document(requestId).set(payload).await()

                // Mirror in user's directory for instant status monitoring
                firestore.collection("users").document(uid)
                    .collection("join_requests").document(requestId).set(payload).await()
            } catch (e: Exception) {
                Log.w(TAG, "Firestore join request sync warning: ${e.message}")
            }

            Result.success(req)
        } catch (e: Exception) {
            Log.e(TAG, "submitJoinRequest failed: ${e.message}", e)
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
        companyId: String,
        requestId: String,
        finalRole: MemberRole,
        assignedWorkerId: String?,
        actorName: String,
        actorUid: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val reqEntity = joinRequestDao.getRequestById(requestId).map { it?.toDomain() }
            val now = System.currentTimeMillis()

            // Update request in Room
            joinRequestDao.updateStatus(
                id = requestId,
                status = JoinRequestStatus.APPROVED.name,
                reviewerId = actorUid,
                reviewerName = actorName,
                reviewedAt = now,
                workerId = assignedWorkerId
            )

            // Extract target user details
            val snapshot = try {
                firestore.collection("companies").document(companyId)
                    .collection("join_requests").document(requestId).get().await()
            } catch (e: Exception) { null }

            val targetUid = snapshot?.getString("userId") ?: requestId.substringAfterLast("_")
            val targetName = snapshot?.getString("userFullName") ?: "Collaboratrice"
            val companyName = snapshot?.getString("companyName") ?: "Atelier"

            // 1. Create or activate CompanyMember in Room
            val member = CompanyMember(
                companyId = companyId,
                userId = targetUid,
                displayName = targetName,
                role = finalRole,
                status = MembershipStatus.ACTIVE,
                companyName = companyName,
                canPostGroup = (finalRole == MemberRole.DIRECTRICE),
                canDirectMessage = true,
                assignedWorkerId = assignedWorkerId,
                createdAtEpochMs = now
            )
            memberDao.insertOrUpdate(CompanyMemberEntity.fromDomain(member))

            // 2. If no existing worker profile was linked, create a new worker profile
            if (assignedWorkerId == null) {
                val names = targetName.split(" ")
                val fName = names.firstOrNull() ?: targetName
                val lName = names.drop(1).joinToString(" ").ifBlank { "Atelier" }
                val inits = "${fName.take(1)}${lName.take(1)}".uppercase()
                val w = WorkerEntity(
                    id = UUID.randomUUID().toString(),
                    firstName = fName,
                    lastName = lName,
                    initials = inits,
                    role = if (finalRole == MemberRole.CHAFE) "Superviseuse (Châfa)" else "Couturière",
                    currentRateMillimes = 3500L,
                    hireDateIso = java.time.LocalDate.now().toString(),
                    isActive = true
                )
                workerDao.insertOrUpdate(w)
            }

            // 3. Sync approval and active membership to Firestore
            try {
                // Update join request status
                firestore.collection("companies").document(companyId)
                    .collection("join_requests").document(requestId)
                    .update(
                        "status", "APPROVED",
                        "reviewedByUid", actorUid,
                        "reviewedByName", actorName,
                        "reviewedAtEpochMs", now,
                        "assignedWorkerId", assignedWorkerId,
                        "finalRole", finalRole.name,
                        "updatedAt", FieldValue.serverTimestamp()
                    ).await()

                // Mirror in user's requests
                firestore.collection("users").document(targetUid)
                    .collection("join_requests").document(requestId)
                    .update(
                        "status", "APPROVED",
                        "reviewedByUid", actorUid,
                        "reviewedByName", actorName,
                        "reviewedAtEpochMs", now,
                        "assignedWorkerId", assignedWorkerId,
                        "finalRole", finalRole.name,
                        "updatedAt", FieldValue.serverTimestamp()
                    ).await()

                // Add active membership in company
                val memberPayload = mapOf(
                    "id" to member.id,
                    "companyId" to companyId,
                    "userId" to targetUid,
                    "displayName" to targetName,
                    "role" to finalRole.name,
                    "status" to "ACTIVE",
                    "canPostGroup" to member.canPostGroup,
                    "canDirectMessage" to true,
                    "assignedWorkerId" to assignedWorkerId,
                    "createdAtEpochMs" to now,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                firestore.collection("companies").document(companyId)
                    .collection("members").document(targetUid).set(memberPayload).await()

                // Add active membership in user's directory
                val userMemPayload = mapOf(
                    "companyId" to companyId,
                    "companyName" to companyName,
                    "role" to finalRole.name,
                    "status" to "ACTIVE",
                    "displayName" to targetName,
                    "assignedWorkerId" to assignedWorkerId,
                    "createdAtEpochMs" to now,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                firestore.collection("users").document(targetUid)
                    .collection("memberships").document(companyId).set(userMemPayload).await()

                // Audit Event
                val auditPayload = mapOf(
                    "id" to UUID.randomUUID().toString(),
                    "companyId" to companyId,
                    "actionType" to AuditActionType.JOIN_REQUEST_APPROVED.name,
                    "actorId" to actorUid,
                    "actorName" to actorName,
                    "actorRole" to MemberRole.DIRECTRICE.name,
                    "detailsFr" to "Adhésion approuvée pour $targetName en qualité de ${finalRole.labelFrench} par $actorName",
                    "timestampEpochMs" to now,
                    "createdAt" to FieldValue.serverTimestamp()
                )
                firestore.collection("companies").document(companyId)
                    .collection("audit_logs").document(auditPayload["id"] as String).set(auditPayload).await()
            } catch (e: Exception) {
                Log.w(TAG, "Firestore approval sync warning: ${e.message}")
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "approveJoinRequest failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun refuseJoinRequest(
        companyId: String,
        requestId: String,
        actorName: String,
        actorUid: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        try {
            joinRequestDao.updateStatus(
                id = requestId,
                status = JoinRequestStatus.REFUSED.name,
                reviewerId = actorUid,
                reviewerName = actorName,
                reviewedAt = now,
                workerId = null
            )

            try {
                firestore.collection("companies").document(companyId)
                    .collection("join_requests").document(requestId)
                    .update(
                        "status", "REFUSED",
                        "reviewedByUid", actorUid,
                        "reviewedByName", actorName,
                        "reviewedAtEpochMs", now,
                        "updatedAt", FieldValue.serverTimestamp()
                    ).await()

                val targetUid = requestId.substringAfterLast("_")
                firestore.collection("users").document(targetUid)
                    .collection("join_requests").document(requestId)
                    .update(
                        "status", "REFUSED",
                        "reviewedByUid", actorUid,
                        "reviewedByName", actorName,
                        "reviewedAtEpochMs", now,
                        "updatedAt", FieldValue.serverTimestamp()
                    ).await()
            } catch (e: Exception) {
                Log.w(TAG, "Firestore refuse sync warning: ${e.message}")
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "refuseJoinRequest failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun updateMemberStatus(
        companyId: String,
        targetUserId: String,
        newStatus: MembershipStatus,
        actorName: String,
        actorUid: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            memberDao.updateMemberStatus(companyId, targetUserId, newStatus.name)

            try {
                firestore.collection("companies").document(companyId)
                    .collection("members").document(targetUserId)
                    .update("status", newStatus.name, "updatedAt", FieldValue.serverTimestamp()).await()

                firestore.collection("users").document(targetUserId)
                    .collection("memberships").document(companyId)
                    .update("status", newStatus.name, "updatedAt", FieldValue.serverTimestamp()).await()

                val auditPayload = mapOf(
                    "id" to UUID.randomUUID().toString(),
                    "companyId" to companyId,
                    "actionType" to (if (newStatus == MembershipStatus.SUSPENDED) AuditActionType.MEMBER_SUSPENDED else AuditActionType.MEMBER_REMOVED).name,
                    "actorId" to actorUid,
                    "actorName" to actorName,
                    "actorRole" to MemberRole.DIRECTRICE.name,
                    "detailsFr" to "Statut du membre $targetUserId changé en ${newStatus.labelFrench} par $actorName",
                    "timestampEpochMs" to System.currentTimeMillis(),
                    "createdAt" to FieldValue.serverTimestamp()
                )
                firestore.collection("companies").document(companyId)
                    .collection("audit_logs").document(auditPayload["id"] as String).set(auditPayload).await()
            } catch (e: Exception) {
                Log.w(TAG, "Firestore member status update warning: ${e.message}")
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "updateMemberStatus failed: ${e.message}", e)
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
