package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.domain.model.AnnouncementAudience
import com.example.domain.model.AnnouncementAuditEntry
import com.example.domain.model.AnnouncementPriority
import com.example.domain.model.AnnouncementReceipt
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

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey val id: String, // UUID idempotency key
    val conversationId: String,
    val companyId: String,
    val type: String, // "GROUP" or "DIRECT"
    val senderId: String,
    val senderName: String,
    val senderRole: String,
    val recipientId: String? = null,
    val text: String,
    val clientTimestamp: Long,
    val serverTimestamp: Long? = null,
    val syncStatus: String, // "PENDING", "SYNCED", "FAILED"
    val errorMessage: String? = null
) {
    fun toDomain(): ChatMessage = ChatMessage(
        id = id,
        conversationId = conversationId,
        companyId = companyId,
        type = if (type == "DIRECT") ConversationType.DIRECT else ConversationType.GROUP,
        senderId = senderId,
        senderName = senderName,
        senderRole = MemberRole.fromString(senderRole),
        recipientId = recipientId,
        text = text,
        clientTimestamp = clientTimestamp,
        serverTimestamp = serverTimestamp,
        syncStatus = runCatching { MessageSyncStatus.valueOf(syncStatus) }.getOrDefault(MessageSyncStatus.PENDING),
        errorMessage = errorMessage
    )

    companion object {
        fun fromDomain(m: ChatMessage): ChatMessageEntity = ChatMessageEntity(
            id = m.id,
            conversationId = m.conversationId,
            companyId = m.companyId,
            type = m.type.name,
            senderId = m.senderId,
            senderName = m.senderName,
            senderRole = m.senderRole.name,
            recipientId = m.recipientId,
            text = m.text,
            clientTimestamp = m.clientTimestamp,
            serverTimestamp = m.serverTimestamp,
            syncStatus = m.syncStatus.name,
            errorMessage = m.errorMessage
        )
    }
}

@Entity(tableName = "chat_conversations")
data class ChatConversationEntity(
    @PrimaryKey val id: String,
    val companyId: String,
    val type: String,
    val title: String,
    val participantUids: String, // Comma-separated UIDs
    val otherParticipantName: String? = null,
    val otherParticipantRole: String? = null,
    val otherParticipantId: String? = null,
    val lastMessageText: String? = null,
    val lastMessageTime: Long? = null,
    val unreadCount: Int = 0
) {
    fun toDomain(): ChatConversation = ChatConversation(
        id = id,
        companyId = companyId,
        type = if (type == "DIRECT") ConversationType.DIRECT else ConversationType.GROUP,
        title = title,
        participantUids = if (participantUids.isBlank()) emptyList() else participantUids.split(",").filter { it.isNotBlank() },
        otherParticipantName = otherParticipantName,
        otherParticipantRole = otherParticipantRole?.let { MemberRole.fromString(it) },
        otherParticipantId = otherParticipantId,
        lastMessageText = lastMessageText,
        lastMessageTime = lastMessageTime,
        unreadCount = unreadCount
    )

    companion object {
        fun fromDomain(c: ChatConversation): ChatConversationEntity = ChatConversationEntity(
            id = c.id,
            companyId = c.companyId,
            type = c.type.name,
            title = c.title,
            participantUids = c.participantUids.joinToString(","),
            otherParticipantName = c.otherParticipantName,
            otherParticipantRole = c.otherParticipantRole?.name,
            otherParticipantId = c.otherParticipantId,
            lastMessageText = c.lastMessageText,
            lastMessageTime = c.lastMessageTime,
            unreadCount = c.unreadCount
        )
    }
}

@Entity(tableName = "company_members")
data class CompanyMemberEntity(
    @PrimaryKey val id: String, // "${companyId}_${userId}"
    val companyId: String,
    val userId: String,
    val displayName: String,
    val role: String,
    val status: String,
    val companyName: String = "",
    val canPostGroup: Boolean,
    val canDirectMessage: Boolean,
    val assignedWorkerId: String? = null,
    val createdAtIso: String = "",
    val createdAtEpochMs: Long = System.currentTimeMillis()
) {
    fun toDomain(): CompanyMember = CompanyMember(
        id = id,
        companyId = companyId,
        userId = userId,
        displayName = displayName,
        role = MemberRole.fromString(role),
        status = MembershipStatus.fromString(status),
        companyName = companyName,
        canPostGroup = canPostGroup,
        canDirectMessage = canDirectMessage,
        assignedWorkerId = assignedWorkerId,
        createdAtIso = createdAtIso,
        createdAtEpochMs = createdAtEpochMs
    )

    companion object {
        fun fromDomain(m: CompanyMember): CompanyMemberEntity = CompanyMemberEntity(
            id = m.id,
            companyId = m.companyId,
            userId = m.userId,
            displayName = m.displayName,
            role = m.role.name,
            status = m.status.name,
            companyName = m.companyName,
            canPostGroup = m.canPostGroup,
            canDirectMessage = m.canDirectMessage,
            assignedWorkerId = m.assignedWorkerId,
            createdAtIso = m.createdAtIso,
            createdAtEpochMs = m.createdAtEpochMs
        )
    }
}

@Entity(tableName = "workshop_announcements")
data class WorkshopAnnouncementEntity(
    @PrimaryKey val id: String,
    val companyId: String,
    val authorId: String,
    val authorName: String,
    val authorRole: String,
    val type: String,
    val title: String,
    val content: String,
    val priority: String,
    val targetAudience: String,
    val startEpochMs: Long,
    val endEpochMs: Long,
    val durationDays: Int,
    val status: String,
    val version: Int = 1,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long,
    val syncStatus: String,
    val isReadLocally: Boolean = false
) {
    fun toDomain(): WorkshopAnnouncement = WorkshopAnnouncement(
        id = id,
        companyId = companyId,
        authorId = authorId,
        authorName = authorName,
        authorRole = MemberRole.fromString(authorRole),
        type = AnnouncementType.fromString(type),
        title = title,
        content = content,
        priority = AnnouncementPriority.fromString(priority),
        targetAudience = AnnouncementAudience.fromString(targetAudience),
        startEpochMs = startEpochMs,
        endEpochMs = endEpochMs,
        durationDays = durationDays,
        status = AnnouncementStatus.fromString(status),
        version = version,
        createdAtEpochMs = createdAtEpochMs,
        updatedAtEpochMs = updatedAtEpochMs,
        syncStatus = runCatching { MessageSyncStatus.valueOf(syncStatus) }.getOrDefault(MessageSyncStatus.SYNCED),
        isReadLocally = isReadLocally
    )

    companion object {
        fun fromDomain(a: WorkshopAnnouncement): WorkshopAnnouncementEntity = WorkshopAnnouncementEntity(
            id = a.id,
            companyId = a.companyId,
            authorId = a.authorId,
            authorName = a.authorName,
            authorRole = a.authorRole.name,
            type = a.type.name,
            title = a.title,
            content = a.content,
            priority = a.priority.name,
            targetAudience = a.targetAudience.name,
            startEpochMs = a.startEpochMs,
            endEpochMs = a.endEpochMs,
            durationDays = a.durationDays,
            status = a.status.name,
            version = a.version,
            createdAtEpochMs = a.createdAtEpochMs,
            updatedAtEpochMs = a.updatedAtEpochMs,
            syncStatus = a.syncStatus.name,
            isReadLocally = a.isReadLocally
        )
    }
}

@Entity(tableName = "announcement_receipts")
data class AnnouncementReceiptEntity(
    @PrimaryKey val id: String, // "${announcementId}_${recipientUserId}_v${announcementVersion}"
    val announcementId: String,
    val companyId: String,
    val recipientUserId: String,
    val recipientName: String,
    val recipientRole: String,
    val firstReadAtEpochMs: Long,
    val lastReadAtEpochMs: Long,
    val announcementVersion: Int = 1,
    val syncStatus: String
) {
    fun toDomain(): AnnouncementReceipt = AnnouncementReceipt(
        id = id,
        announcementId = announcementId,
        companyId = companyId,
        recipientUserId = recipientUserId,
        recipientName = recipientName,
        recipientRole = MemberRole.fromString(recipientRole),
        firstReadAtEpochMs = firstReadAtEpochMs,
        lastReadAtEpochMs = lastReadAtEpochMs,
        announcementVersion = announcementVersion,
        syncStatus = runCatching { MessageSyncStatus.valueOf(syncStatus) }.getOrDefault(MessageSyncStatus.SYNCED)
    )

    companion object {
        fun fromDomain(r: AnnouncementReceipt): AnnouncementReceiptEntity = AnnouncementReceiptEntity(
            id = r.id,
            announcementId = r.announcementId,
            companyId = r.companyId,
            recipientUserId = r.recipientUserId,
            recipientName = r.recipientName,
            recipientRole = r.recipientRole.name,
            firstReadAtEpochMs = r.firstReadAtEpochMs,
            lastReadAtEpochMs = r.lastReadAtEpochMs,
            announcementVersion = r.announcementVersion,
            syncStatus = r.syncStatus.name
        )
    }
}

@Entity(tableName = "announcement_audit_logs")
data class AnnouncementAuditEntity(
    @PrimaryKey val id: String,
    val companyId: String,
    val announcementId: String,
    val action: String,
    val actorId: String,
    val actorName: String,
    val actorRole: String,
    val timestampEpochMs: Long,
    val details: String = ""
) {
    fun toDomain(): AnnouncementAuditEntry = AnnouncementAuditEntry(
        id = id,
        companyId = companyId,
        announcementId = announcementId,
        action = action,
        actorId = actorId,
        actorName = actorName,
        actorRole = actorRole,
        timestampEpochMs = timestampEpochMs,
        details = details
    )

    companion object {
        fun fromDomain(a: AnnouncementAuditEntry): AnnouncementAuditEntity = AnnouncementAuditEntity(
            id = a.id,
            companyId = a.companyId,
            announcementId = a.announcementId,
            action = a.action,
            actorId = a.actorId,
            actorName = a.actorName,
            actorRole = a.actorRole,
            timestampEpochMs = a.timestampEpochMs,
            details = a.details
        )
    }
}

