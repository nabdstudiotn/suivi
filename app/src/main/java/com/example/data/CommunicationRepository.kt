package com.example.data

import android.util.Log
import com.example.data.local.AnnouncementAuditEntity
import com.example.data.local.AnnouncementReceiptEntity
import com.example.data.local.ChatConversationEntity
import com.example.data.local.ChatMessageEntity
import com.example.data.local.CompanyMemberEntity
import com.example.data.local.SuiviDatabase
import com.example.data.local.WorkshopAnnouncementEntity
import com.example.domain.model.AnnouncementAudience
import com.example.domain.model.AnnouncementAuditEntry
import com.example.domain.model.AnnouncementPriority
import com.example.domain.model.AnnouncementReceipt
import com.example.domain.model.AnnouncementReadStats
import com.example.domain.model.AnnouncementStatus
import com.example.domain.model.AnnouncementType
import com.example.domain.model.ChatConversation
import com.example.domain.model.ChatMessage
import com.example.domain.model.CompanyMember
import com.example.domain.model.ConversationType
import com.example.domain.model.MemberRole
import com.example.domain.model.MembershipStatus
import com.example.domain.model.MessageSyncStatus
import com.example.domain.model.WorkshopAnnouncement
import com.example.domain.model.buildDirectConversationId
import com.example.domain.model.buildGroupConversationId
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

private const val TAG = "CommunicationRepo"

/**
 * Repository for workshop communication:
 * - Room on-device cache and outbox queue.
 * - Firestore cloud persistence with company & membership isolation.
 * - Honest sync states (En attente de synchronisation / Synchronisé / Échec de l'envoi).
 * - Idempotent message retries without duplication.
 */
class CommunicationRepository(
    private val database: SuiviDatabase,
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val messageDao = database.chatMessageDao()
    private val conversationDao = database.chatConversationDao()
    private val memberDao = database.companyMemberDao()
    private val announcementDao = database.workshopAnnouncementDao()
    private val receiptDao = database.announcementReceiptDao()
    private val auditDao = database.announcementAuditDao()

    private fun requireUserId(): String {
        return auth.currentUser?.uid ?: throw IllegalStateException("Utilisateur non authentifié avec Google")
    }

    /**
     * Observes the active company member record for current user.
     */
    fun observeCurrentMember(companyId: String): Flow<CompanyMember?> {
        val uid = auth.currentUser?.uid ?: return kotlinx.coroutines.flow.flowOf(null)
        return memberDao.getMembersForCompany(companyId).map { list ->
            list.firstOrNull { it.userId == uid }?.toDomain()
        }
    }

    /**
     * Initializes or refreshes user's company membership.
     * Enforces that Directrice owns company and has group posting authority.
     */
    suspend fun ensureCurrentMember(
        companyId: String,
        defaultDisplayName: String = "Utilisateur",
        defaultRole: MemberRole = MemberRole.DIRECTRICE
    ): CompanyMember = withContext(Dispatchers.IO) {
        val uid = requireUserId()
        val existing = memberDao.getMember(companyId, uid)
        if (existing != null) {
            val domain = existing.toDomain()
            // Sync with Firestore in background
            syncMemberToFirestore(domain)
            return@withContext domain
        }

        // Check Firestore first
        val memberDocRef = firestore.collection("companies").document(companyId)
            .collection("members").document(uid)
        val snapshot = runCatching { memberDocRef.get().await() }.getOrNull()

        val member = if (snapshot != null && snapshot.exists()) {
            val roleStr = snapshot.getString("role")
            val statusStr = snapshot.getString("status")
            val canPost = snapshot.getBoolean("canPostGroup") ?: (defaultRole == MemberRole.DIRECTRICE)
            val canDm = snapshot.getBoolean("canDirectMessage") ?: true
            val name = snapshot.getString("displayName") ?: defaultDisplayName
            CompanyMember(
                companyId = companyId,
                userId = uid,
                displayName = name,
                role = MemberRole.fromString(roleStr),
                status = MembershipStatus.fromString(statusStr),
                canPostGroup = canPost,
                canDirectMessage = canDm,
                createdAtIso = snapshot.getTimestamp("createdAt")?.toDate()?.toString() ?: ""
            )
        } else {
            // First time registration: create company container and member doc
            val nowIso = java.time.Instant.now().toString()
            val newMember = CompanyMember(
                companyId = companyId,
                userId = uid,
                displayName = auth.currentUser?.displayName ?: defaultDisplayName,
                role = defaultRole,
                status = MembershipStatus.ACTIVE,
                canPostGroup = (defaultRole == MemberRole.DIRECTRICE),
                canDirectMessage = true,
                createdAtIso = nowIso
            )

            // Register company in Firestore if Directrice
            if (defaultRole == MemberRole.DIRECTRICE) {
                runCatching {
                    firestore.collection("companies").document(companyId).set(
                        mapOf(
                            "id" to companyId,
                            "name" to "Atelier NABD",
                            "ownerId" to uid,
                            "createdAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                }
            }

            syncMemberToFirestore(newMember)
            newMember
        }

        // Cache in Room
        memberDao.insertOrUpdate(CompanyMemberEntity.fromDomain(member))

        // Ensure default Group Conversation exists in Room & Firestore
        ensureGroupConversation(companyId)

        member
    }

    private suspend fun syncMemberToFirestore(member: CompanyMember) {
        runCatching {
            firestore.collection("companies").document(member.companyId)
                .collection("members").document(member.userId).set(
                    mapOf(
                        "userId" to member.userId,
                        "companyId" to member.companyId,
                        "displayName" to member.displayName,
                        "role" to member.role.name,
                        "status" to member.status.name,
                        "canPostGroup" to member.canPostGroup,
                        "canDirectMessage" to member.canDirectMessage,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                ).await()
        }.onFailure {
            Log.w(TAG, "Failed to sync member to Firestore (offline or pending rules): ${it.message}")
        }
    }

    private suspend fun ensureGroupConversation(companyId: String) {
        val groupId = buildGroupConversationId(companyId)
        val existing = conversationDao.getConversationById(groupId)
        if (existing == null) {
            val entity = ChatConversationEntity(
                id = groupId,
                companyId = companyId,
                type = ConversationType.GROUP.name,
                title = "Discussion d'atelier",
                participantUids = "",
                lastMessageText = null,
                lastMessageTime = System.currentTimeMillis()
            )
            conversationDao.insertOrUpdate(entity)

            // Firestore sync
            runCatching {
                firestore.collection("companies").document(companyId)
                    .collection("conversations").document(groupId).set(
                        mapOf(
                            "id" to groupId,
                            "companyId" to companyId,
                            "type" to ConversationType.GROUP.name,
                            "title" to "Discussion d'atelier",
                            "createdAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
            }
        }
    }

    /**
     * Observes all company members for direct messaging discovery.
     * Note: Search and discovery is strictly isolated to members of this company.
     */
    fun observeCompanyMembers(companyId: String): Flow<List<CompanyMember>> {
        // Also kick off Firestore snapshot listener to keep members list updated
        startMembersRealtimeListener(companyId)
        return memberDao.getActiveMembersForCompany(companyId).map { list ->
            list.map { it.toDomain() }
        }
    }

    private fun startMembersRealtimeListener(companyId: String) {
        if (auth.currentUser == null) return
        repositoryScope.launch {
            try {
                firestore.collection("companies").document(companyId)
                    .collection("members")
                    .snapshots()
                    .collect { snapshot ->
                        val entities = snapshot.documents.mapNotNull { doc ->
                            val uid = doc.getString("userId") ?: doc.id
                            val name = doc.getString("displayName") ?: return@mapNotNull null
                            val role = doc.getString("role") ?: "OUVRIERE"
                            val status = doc.getString("status") ?: "ACTIVE"
                            val canPost = doc.getBoolean("canPostGroup") ?: false
                            val canDm = doc.getBoolean("canDirectMessage") ?: true
                            CompanyMemberEntity(
                                id = "${companyId}_$uid",
                                companyId = companyId,
                                userId = uid,
                                displayName = name,
                                role = role,
                                status = status,
                                canPostGroup = canPost,
                                canDirectMessage = canDm
                            )
                        }
                        if (entities.isNotEmpty()) {
                            memberDao.insertMembers(entities)
                        }
                    }
            } catch (e: Exception) {
                Log.w(TAG, "Members listener paused or offline: ${e.message}")
            }
        }
    }

    /**
     * Observes conversations for the current company.
     */
    fun observeConversations(companyId: String): Flow<List<ChatConversation>> {
        return conversationDao.getConversationsForCompany(companyId).map { list ->
            list.map { it.toDomain() }
        }
    }

    /**
     * Gets or creates a canonical 1-to-1 conversation between current user and another company member.
     */
    suspend fun createOrGetDirectConversation(companyId: String, otherMember: CompanyMember): ChatConversation = withContext(Dispatchers.IO) {
        val currentUid = requireUserId()
        require(otherMember.companyId == companyId) { "Impossible de communiquer en dehors de l'atelier." }
        require(otherMember.isActive) { "L'adhésion de ce membre n'est pas active." }

        val convId = buildDirectConversationId(companyId, currentUid, otherMember.userId)
        val existing = conversationDao.getConversationById(convId)
        if (existing != null) {
            return@withContext existing.toDomain()
        }

        val conv = ChatConversation(
            id = convId,
            companyId = companyId,
            type = ConversationType.DIRECT,
            title = otherMember.displayName,
            participantUids = listOf(currentUid, otherMember.userId),
            otherParticipantName = otherMember.displayName,
            otherParticipantRole = otherMember.role,
            otherParticipantId = otherMember.userId,
            lastMessageText = null,
            lastMessageTime = System.currentTimeMillis()
        )

        conversationDao.insertOrUpdate(ChatConversationEntity.fromDomain(conv))

        runCatching {
            firestore.collection("companies").document(companyId)
                .collection("conversations").document(convId).set(
                    mapOf(
                        "id" to convId,
                        "companyId" to companyId,
                        "type" to ConversationType.DIRECT.name,
                        "title" to otherMember.displayName,
                        "participantUids" to listOf(currentUid, otherMember.userId),
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                ).await()
        }

        conv
    }

    /**
     * Observes messages in a conversation from Room cache.
     */
    fun observeMessages(conversationId: String): Flow<List<ChatMessage>> {
        return messageDao.getMessagesForConversation(conversationId).map { list ->
            list.map { it.toDomain() }
        }
    }

    /**
     * Realtime snapshot listener from Firestore to update Room cache.
     */
    fun startRealtimeMessagesListener(companyId: String, conversationId: String) {
        if (auth.currentUser == null) return
        repositoryScope.launch {
            try {
                firestore.collection("companies").document(companyId)
                    .collection("conversations").document(conversationId)
                    .collection("messages")
                    .orderBy("clientTimestamp")
                    .snapshots()
                    .collect { snapshot ->
                        val entities = snapshot.documents.mapNotNull { doc ->
                            val id = doc.getString("id") ?: doc.id
                            val text = doc.getString("text") ?: return@mapNotNull null
                            val senderId = doc.getString("senderId") ?: ""
                            val senderName = doc.getString("senderName") ?: "Membre"
                            val senderRole = doc.getString("senderRole") ?: "OUVRIERE"
                            val type = doc.getString("type") ?: "GROUP"
                            val recipientId = doc.getString("recipientId")
                            val clientTs = doc.getLong("clientTimestamp") ?: System.currentTimeMillis()
                            val serverTs = doc.getTimestamp("createdAt")?.toDate()?.time

                            ChatMessageEntity(
                                id = id,
                                conversationId = conversationId,
                                companyId = companyId,
                                type = type,
                                senderId = senderId,
                                senderName = senderName,
                                senderRole = senderRole,
                                recipientId = recipientId,
                                text = text,
                                clientTimestamp = clientTs,
                                serverTimestamp = serverTs,
                                syncStatus = MessageSyncStatus.SYNCED.name,
                                errorMessage = null
                            )
                        }
                        if (entities.isNotEmpty()) {
                            messageDao.insertMessages(entities)
                        }
                    }
            } catch (e: Exception) {
                Log.w(TAG, "Realtime messages listener paused: ${e.message}")
            }
        }
    }

    /**
     * Sends a real message with idempotent UUID, Room cache, honest sync state and Firestore write.
     */
    suspend fun sendMessage(
        companyId: String,
        conversationId: String,
        text: String,
        type: ConversationType,
        recipientId: String? = null
    ): Result<ChatMessage> = withContext(Dispatchers.IO) {
        val uid = auth.currentUser?.uid
            ?: return@withContext Result.failure(IllegalStateException("Utilisateur non connecté avec Google."))

        if (text.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Le message ne peut pas être vide."))
        }

        // Verify membership & permissions in data layer
        val member = memberDao.getMember(companyId, uid)?.toDomain()
            ?: ensureCurrentMember(companyId)

        if (!member.isActive) {
            return@withContext Result.failure(SecurityException("Votre adhésion à l'atelier est suspendue."))
        }

        if (type == ConversationType.GROUP && !member.canPostGroup && member.role != MemberRole.DIRECTRICE) {
            return@withContext Result.failure(SecurityException("Publication réservée à la gérante pour le groupe d'atelier."))
        }

        if (type == ConversationType.DIRECT) {
            if (!member.canDirectMessage) {
                return@withContext Result.failure(SecurityException("L'envoi de messages privés n'est pas autorisé pour votre profil."))
            }
            if (recipientId == null) {
                return@withContext Result.failure(IllegalArgumentException("Destinataire manquant pour le message privé."))
            }
            val recipient = memberDao.getMember(companyId, recipientId)
            if (recipient == null) {
                return@withContext Result.failure(SecurityException("Le destinataire ne fait pas partie de cet atelier."))
            }
        }

        val messageId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()

        // 1. Immediately store in Room outbox as PENDING
        val localEntity = ChatMessageEntity(
            id = messageId,
            conversationId = conversationId,
            companyId = companyId,
            type = type.name,
            senderId = uid,
            senderName = member.displayName,
            senderRole = member.role.name,
            recipientId = recipientId,
            text = text.trim(),
            clientTimestamp = now,
            serverTimestamp = null,
            syncStatus = MessageSyncStatus.PENDING.name,
            errorMessage = null
        )
        messageDao.insertMessage(localEntity)

        // Update conversation preview
        val conv = conversationDao.getConversationById(conversationId)
        if (conv != null) {
            conversationDao.insertOrUpdate(conv.copy(
                lastMessageText = text.trim(),
                lastMessageTime = now
            ))
        }

        // 2. Perform honest Firestore write with idempotent ID
        return@withContext try {
            val payload = hashMapOf<String, Any>(
                "id" to messageId,
                "conversationId" to conversationId,
                "companyId" to companyId,
                "type" to type.name,
                "senderId" to uid,
                "senderName" to member.displayName,
                "senderRole" to member.role.name,
                "text" to text.trim(),
                "clientTimestamp" to now,
                "participantUids" to if (type == ConversationType.DIRECT) listOf(uid, recipientId!!) else emptyList<String>(),
                "createdAt" to FieldValue.serverTimestamp()
            )
            if (recipientId != null) {
                payload["recipientId"] = recipientId
            }

            firestore.collection("companies").document(companyId)
                .collection("conversations").document(conversationId)
                .collection("messages").document(messageId)
                .set(payload).await()

            // Confirm honest SYNCED status
            val syncTime = System.currentTimeMillis()
            messageDao.updateSyncStatus(
                id = messageId,
                status = MessageSyncStatus.SYNCED.name,
                serverTimestamp = syncTime,
                errorMessage = null
            )
            Result.success(localEntity.toDomain().copy(syncStatus = MessageSyncStatus.SYNCED, serverTimestamp = syncTime))
        } catch (e: Exception) {
            Log.e(TAG, "Message send failed (kept in outbox as PENDING / FAILED)", e)
            val isAuthOrSecurity = e is SecurityException || (e.message?.contains("PERMISSION_DENIED") == true)
            val newStatus = if (isAuthOrSecurity) MessageSyncStatus.FAILED.name else MessageSyncStatus.PENDING.name
            messageDao.updateSyncStatus(
                id = messageId,
                status = newStatus,
                serverTimestamp = null,
                errorMessage = e.localizedMessage ?: "Échec de l'envoi"
            )
            Result.failure(e)
        }
    }

    /**
     * Idempotent retry: Re-attempts sending a message using its existing message ID.
     * Guaranteed never to duplicate messages on reconnection.
     */
    suspend fun retryMessage(messageId: String): Result<Unit> = withContext(Dispatchers.IO) {
        val message = messageDao.getMessageById(messageId)
            ?: return@withContext Result.failure(IllegalArgumentException("Message introuvable"))

        try {
            val payload = hashMapOf<String, Any>(
                "id" to message.id,
                "conversationId" to message.conversationId,
                "companyId" to message.companyId,
                "type" to message.type,
                "senderId" to message.senderId,
                "senderName" to message.senderName,
                "senderRole" to message.senderRole,
                "text" to message.text,
                "clientTimestamp" to message.clientTimestamp,
                "participantUids" to if (message.type == "DIRECT" && message.recipientId != null)
                    listOf(message.senderId, message.recipientId) else emptyList<String>(),
                "createdAt" to FieldValue.serverTimestamp()
            )
            if (message.recipientId != null) {
                payload["recipientId"] = message.recipientId
            }

            firestore.collection("companies").document(message.companyId)
                .collection("conversations").document(message.conversationId)
                .collection("messages").document(message.id)
                .set(payload).await()

            messageDao.updateSyncStatus(
                id = message.id,
                status = MessageSyncStatus.SYNCED.name,
                serverTimestamp = System.currentTimeMillis(),
                errorMessage = null
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Retry failed for message ${message.id}", e)
            messageDao.updateSyncStatus(
                id = message.id,
                status = MessageSyncStatus.FAILED.name,
                serverTimestamp = null,
                errorMessage = e.localizedMessage
            )
            Result.failure(e)
        }
    }

    /**
     * Flushes outbox queue of pending messages.
     */
    suspend fun flushOutbox(): Int = withContext(Dispatchers.IO) {
        val pending = messageDao.getPendingMessages()
        var count = 0
        for (m in pending) {
            val result = retryMessage(m.id)
            if (result.isSuccess) count++
        }
        count
    }

    /**
     * Observes all announcements for the company from Room cache.
     */
    fun observeAnnouncements(companyId: String): Flow<List<WorkshopAnnouncement>> {
        return announcementDao.getAnnouncementsForCompany(companyId).map { list ->
            list.map { it.toDomain() }
        }
    }

    /**
     * Starts listening for realtime announcement updates in Firestore for the company.
     */
    fun startAnnouncementsRealtimeListener(companyId: String) {
        repositoryScope.launch {
            try {
                firestore.collection("companies").document(companyId)
                    .collection("announcements")
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            Log.w(TAG, "Announcements listener error: ${error.message}")
                            return@addSnapshotListener
                        }
                        if (snapshot == null) return@addSnapshotListener

                        val entities = snapshot.documents.mapNotNull { doc ->
                            val id = doc.getString("id") ?: doc.id
                            val authorId = doc.getString("authorId") ?: ""
                            val authorName = doc.getString("authorName") ?: "Direction"
                            val authorRole = doc.getString("authorRole") ?: "DIRECTRICE"
                            val type = doc.getString("type") ?: "INFORMATION"
                            val title = doc.getString("title") ?: ""
                            val content = doc.getString("content") ?: ""
                            val priority = doc.getString("priority") ?: "NORMALE"
                            val audience = doc.getString("targetAudience") ?: "TOUS"
                            val startMs = doc.getLong("startEpochMs") ?: System.currentTimeMillis()
                            val endMs = doc.getLong("endEpochMs") ?: (startMs + 7 * 86400000L)
                            val durationDays = doc.getLong("durationDays")?.toInt() ?: 7
                            val status = doc.getString("status") ?: "ACTIVE"
                            val version = doc.getLong("version")?.toInt() ?: 1
                            val createdTs = doc.getTimestamp("createdAt")?.toDate()?.time ?: startMs
                            val updatedTs = doc.getTimestamp("updatedAt")?.toDate()?.time ?: createdTs

                            WorkshopAnnouncementEntity(
                                id = id,
                                companyId = companyId,
                                authorId = authorId,
                                authorName = authorName,
                                authorRole = authorRole,
                                type = type,
                                title = title,
                                content = content,
                                priority = priority,
                                targetAudience = audience,
                                startEpochMs = startMs,
                                endEpochMs = endMs,
                                durationDays = durationDays,
                                status = status,
                                version = version,
                                createdAtEpochMs = createdTs,
                                updatedAtEpochMs = updatedTs,
                                syncStatus = MessageSyncStatus.SYNCED.name
                            )
                        }
                        if (entities.isNotEmpty()) {
                            repositoryScope.launch {
                                announcementDao.insertAnnouncements(entities)
                            }
                        }
                    }
            } catch (e: Exception) {
                Log.w(TAG, "Announcements listener paused: ${e.message}")
            }
        }
    }

    /**
     * Publishes a real workshop announcement authorized by Directrice/Manager.
     */
    suspend fun createAnnouncement(
        companyId: String,
        type: AnnouncementType,
        title: String,
        content: String,
        priority: AnnouncementPriority,
        targetAudience: AnnouncementAudience,
        durationDays: Int
    ): Result<WorkshopAnnouncement> = withContext(Dispatchers.IO) {
        val uid = auth.currentUser?.uid
            ?: return@withContext Result.failure(IllegalStateException("Utilisateur non authentifié avec Google."))

        if (title.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Le titre de l'annonce est obligatoire."))
        }
        if (content.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Le contenu de l'annonce est obligatoire."))
        }
        if (durationDays < 1) {
            return@withContext Result.failure(IllegalArgumentException("La durée minimale est de 1 jour."))
        }

        val member = memberDao.getMember(companyId, uid)?.toDomain()
            ?: ensureCurrentMember(companyId)

        if (member.role != MemberRole.DIRECTRICE && !member.canPostGroup) {
            return@withContext Result.failure(SecurityException("Seule la direction de l'atelier peut publier des annonces."))
        }

        val now = System.currentTimeMillis()
        val announcementId = UUID.randomUUID().toString()
        val endEpochMs = now + (durationDays.toLong() * 86_400_000L)

        val announcement = WorkshopAnnouncement(
            id = announcementId,
            companyId = companyId,
            authorId = uid,
            authorName = member.displayName,
            authorRole = member.role,
            type = type,
            title = title.trim(),
            content = content.trim(),
            priority = priority,
            targetAudience = targetAudience,
            startEpochMs = now,
            endEpochMs = endEpochMs,
            durationDays = durationDays,
            status = AnnouncementStatus.ACTIVE,
            version = 1,
            createdAtEpochMs = now,
            updatedAtEpochMs = now,
            syncStatus = MessageSyncStatus.PENDING
        )

        // 1. Room local insert
        announcementDao.insertOrUpdate(WorkshopAnnouncementEntity.fromDomain(announcement))

        // 2. Cloud Firestore sync
        try {
            val payload = mapOf(
                "id" to announcementId,
                "companyId" to companyId,
                "authorId" to uid,
                "authorName" to member.displayName,
                "authorRole" to member.role.name,
                "type" to type.name,
                "title" to title.trim(),
                "content" to content.trim(),
                "priority" to priority.name,
                "targetAudience" to targetAudience.name,
                "startEpochMs" to now,
                "endEpochMs" to endEpochMs,
                "durationDays" to durationDays,
                "status" to AnnouncementStatus.ACTIVE.name,
                "version" to 1,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            firestore.collection("companies").document(companyId)
                .collection("announcements").document(announcementId)
                .set(payload).await()

            // Audit record
            val auditId = UUID.randomUUID().toString()
            firestore.collection("companies").document(companyId)
                .collection("audit_logs").document(auditId)
                .set(mapOf(
                    "id" to auditId,
                    "companyId" to companyId,
                    "announcementId" to announcementId,
                    "action" to "CREATED",
                    "actorId" to uid,
                    "actorName" to member.displayName,
                    "timestampEpochMs" to now,
                    "createdAt" to FieldValue.serverTimestamp()
                )).await()

            auditDao.insertAudit(AnnouncementAuditEntity(
                id = auditId,
                companyId = companyId,
                announcementId = announcementId,
                action = "CREATED",
                actorId = uid,
                actorName = member.displayName,
                actorRole = member.role.name,
                timestampEpochMs = now,
                details = "Publication de l'annonce '${title.trim()}'"
            ))

            val synced = announcement.copy(syncStatus = MessageSyncStatus.SYNCED)
            announcementDao.insertOrUpdate(WorkshopAnnouncementEntity.fromDomain(synced))
            Result.success(synced)
        } catch (e: Exception) {
            Log.e(TAG, "Announcement cloud write failed: ${e.message}", e)
            announcementDao.insertOrUpdate(
                WorkshopAnnouncementEntity.fromDomain(announcement.copy(syncStatus = MessageSyncStatus.FAILED))
            )
            Result.failure(e)
        }
    }

    /**
     * Cancels an active announcement.
     */
    suspend fun cancelAnnouncement(
        companyId: String,
        announcementId: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val uid = auth.currentUser?.uid
            ?: return@withContext Result.failure(IllegalStateException("Utilisateur non authentifié avec Google."))

        val member = memberDao.getMember(companyId, uid)?.toDomain()
            ?: ensureCurrentMember(companyId)

        if (member.role != MemberRole.DIRECTRICE && !member.canPostGroup) {
            return@withContext Result.failure(SecurityException("Seule la direction peut annuler une annonce."))
        }

        val now = System.currentTimeMillis()
        try {
            firestore.collection("companies").document(companyId)
                .collection("announcements").document(announcementId)
                .update(
                    "status", AnnouncementStatus.CANCELLED.name,
                    "updatedAt", FieldValue.serverTimestamp()
                ).await()

            announcementDao.updateStatus(announcementId, AnnouncementStatus.CANCELLED.name, now)

            val auditId = UUID.randomUUID().toString()
            firestore.collection("companies").document(companyId)
                .collection("audit_logs").document(auditId)
                .set(mapOf(
                    "id" to auditId,
                    "companyId" to companyId,
                    "announcementId" to announcementId,
                    "action" to "CANCELLED",
                    "actorId" to uid,
                    "actorName" to member.displayName,
                    "timestampEpochMs" to now,
                    "createdAt" to FieldValue.serverTimestamp()
                )).await()

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Cancel announcement failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Records a read receipt when user explicitly opens announcement details.
     * Idempotent per announcement version.
     */
    suspend fun recordAnnouncementRead(
        companyId: String,
        announcement: WorkshopAnnouncement
    ): Result<AnnouncementReceipt> = withContext(Dispatchers.IO) {
        val uid = auth.currentUser?.uid
            ?: return@withContext Result.failure(IllegalStateException("Utilisateur non authentifié avec Google."))

        val member = memberDao.getMember(companyId, uid)?.toDomain()
            ?: ensureCurrentMember(companyId)

        val now = System.currentTimeMillis()
        val receiptId = "${announcement.id}_${uid}_v${announcement.version}"
        val existing = receiptDao.getReceiptForUser(announcement.id, uid)

        val receipt = if (existing != null) {
            existing.toDomain().copy(lastReadAtEpochMs = now)
        } else {
            AnnouncementReceipt(
                id = receiptId,
                announcementId = announcement.id,
                companyId = companyId,
                recipientUserId = uid,
                recipientName = member.displayName,
                recipientRole = member.role,
                firstReadAtEpochMs = now,
                lastReadAtEpochMs = now,
                announcementVersion = announcement.version,
                syncStatus = MessageSyncStatus.PENDING
            )
        }

        // Room local cache
        receiptDao.insertOrUpdate(AnnouncementReceiptEntity.fromDomain(receipt))
        announcementDao.markLocallyRead(announcement.id)

        // Sync receipt to Firestore
        try {
            val payload = mapOf(
                "companyId" to companyId,
                "announcementId" to announcement.id,
                "recipientUserId" to uid,
                "recipientName" to member.displayName,
                "recipientRole" to member.role.name,
                "firstReadAtEpochMs" to receipt.firstReadAtEpochMs,
                "lastReadAtEpochMs" to receipt.lastReadAtEpochMs,
                "announcementVersion" to receipt.announcementVersion,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            firestore.collection("companies").document(companyId)
                .collection("announcements").document(announcement.id)
                .collection("receipts").document(uid)
                .set(payload).await()

            receiptDao.updateSyncStatus(receipt.id, MessageSyncStatus.SYNCED.name)
            Result.success(receipt.copy(syncStatus = MessageSyncStatus.SYNCED))
        } catch (e: Exception) {
            Log.w(TAG, "Receipt offline: sync deferred (${e.message})")
            receiptDao.updateSyncStatus(receipt.id, MessageSyncStatus.PENDING.name)
            Result.success(receipt.copy(syncStatus = MessageSyncStatus.PENDING))
        }
    }

    /**
     * Observes real read receipts for a given announcement.
     */
    fun observeReceiptsForAnnouncement(
        companyId: String,
        announcementId: String
    ): Flow<List<AnnouncementReceipt>> {
        startReceiptsRealtimeListener(companyId, announcementId)
        return receiptDao.getReceiptsForAnnouncement(announcementId).map { list ->
            list.map { it.toDomain() }
        }
    }

    /**
     * Starts listening to receipts for announcement in Firestore (authorized for Manager/Author).
     */
    fun startReceiptsRealtimeListener(companyId: String, announcementId: String) {
        repositoryScope.launch {
            try {
                firestore.collection("companies").document(companyId)
                    .collection("announcements").document(announcementId)
                    .collection("receipts")
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            Log.w(TAG, "Receipts listener error: ${error.message}")
                            return@addSnapshotListener
                        }
                        if (snapshot == null) return@addSnapshotListener

                        val entities = snapshot.documents.mapNotNull { doc ->
                            val uid = doc.getString("recipientUserId") ?: doc.id
                            val name = doc.getString("recipientName") ?: ""
                            val role = doc.getString("recipientRole") ?: "OUVRIERE"
                            val firstRead = doc.getLong("firstReadAtEpochMs") ?: System.currentTimeMillis()
                            val lastRead = doc.getLong("lastReadAtEpochMs") ?: firstRead
                            val version = doc.getLong("announcementVersion")?.toInt() ?: 1
                            AnnouncementReceiptEntity(
                                id = "${announcementId}_${uid}_v$version",
                                announcementId = announcementId,
                                companyId = companyId,
                                recipientUserId = uid,
                                recipientName = name,
                                recipientRole = role,
                                firstReadAtEpochMs = firstRead,
                                lastReadAtEpochMs = lastRead,
                                announcementVersion = version,
                                syncStatus = MessageSyncStatus.SYNCED.name
                            )
                        }
                        if (entities.isNotEmpty()) {
                            repositoryScope.launch {
                                receiptDao.insertReceipts(entities)
                            }
                        }
                    }
            } catch (e: Exception) {
                Log.w(TAG, "Receipts listener paused: ${e.message}")
            }
        }
    }

    /**
     * Combines active audience members and receipts into full read breakdown for the manager.
     */
    fun getReadStatsForAnnouncement(
        companyId: String,
        announcement: WorkshopAnnouncement
    ): Flow<AnnouncementReadStats> {
        return combine(
            receiptDao.getReceiptsForAnnouncement(announcement.id),
            memberDao.getActiveMembersForCompany(companyId)
        ) { receipts, members ->
            val eligibleMembers = members.map { it.toDomain() }
                .filter { announcement.isAudienceEligible(it) }
            val readers = receipts.map { it.toDomain() }
            val readUids = readers.map { it.recipientUserId }.toSet()
            val unreadMembers = eligibleMembers.filter { it.userId !in readUids }

            AnnouncementReadStats(
                announcementId = announcement.id,
                targetAudienceCount = eligibleMembers.size,
                readCount = readers.size,
                unreadCount = unreadMembers.size,
                readers = readers,
                unreadMembers = unreadMembers
            )
        }
    }
}
