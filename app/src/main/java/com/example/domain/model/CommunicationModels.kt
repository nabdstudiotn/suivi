package com.example.domain.model

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Roles for workshop members according to the hierarchy:
 * - Directrice: owner of the company, full posting and management authority.
 * - Châfa: supervisor, participates if permitted.
 * - Ouvrière: worker, participates if permitted.
 */
enum class MemberRole(val labelFrench: String, val descriptionFr: String) {
    DIRECTRICE("Directrice", "Gérante de l'atelier"),
    CHAFE("Châfa", "Superviseure d'équipe"),
    OUVRIERE("Ouvrière", "Opératrice d'atelier");

    companion object {
        fun fromString(role: String?): MemberRole {
            return when (role?.uppercase(Locale.US)?.trim()) {
                "DIRECTRICE", "ADMIN", "GERANTE", "MANAGER" -> DIRECTRICE
                "CHAFE", "CHAFA", "SUPERVISOR", "CHÂFA" -> CHAFE
                else -> OUVRIERE
            }
        }
    }
}

/**
 * Membership status of a user within a company.
 */
enum class MembershipStatus(val labelFrench: String) {
    ACTIVE("Actif"),
    SUSPENDED("Suspendu");

    companion object {
        fun fromString(status: String?): MembershipStatus {
            return if (status?.uppercase(Locale.US)?.trim() == "SUSPENDED") SUSPENDED else ACTIVE
        }
    }
}

/**
 * Company entity for workshop data isolation.
 */
data class Company(
    val id: String,
    val name: String,
    val ownerId: String,
    val createdAtIso: String = ""
)

/**
 * Workshop membership tying a Firebase Auth user to a company with verified permissions.
 */
data class CompanyMember(
    val companyId: String,
    val userId: String,
    val displayName: String,
    val role: MemberRole,
    val status: MembershipStatus = MembershipStatus.ACTIVE,
    val canPostGroup: Boolean = (role == MemberRole.DIRECTRICE),
    val canDirectMessage: Boolean = true,
    val createdAtIso: String = "",
    val id: String = "${companyId}_${userId}"
) {
    val isActive: Boolean
        get() = status == MembershipStatus.ACTIVE
}

/**
 * Conversation types supported in Communication room.
 */
enum class ConversationType(val labelFrench: String) {
    GROUP("Discussion d'atelier"),
    DIRECT("Message privé")
}

/**
 * Chat conversation representation.
 */
data class ChatConversation(
    val id: String,
    val companyId: String,
    val type: ConversationType,
    val title: String,
    val participantUids: List<String> = emptyList(),
    val otherParticipantName: String? = null,
    val otherParticipantRole: MemberRole? = null,
    val otherParticipantId: String? = null,
    val lastMessageText: String? = null,
    val lastMessageTime: Long? = null,
    val unreadCount: Int = 0
)

/**
 * Honest sync states for messaging:
 * - PENDING: En attente de synchronisation (in Room outbox).
 * - SYNCED: Synchronisé (confirmed by Firestore).
 * - FAILED: Échec de l’envoi (network or permission error).
 */
enum class MessageSyncStatus(val labelFrench: String) {
    PENDING("En attente de synchronisation"),
    SYNCED("Synchronisé"),
    FAILED("Échec de l’envoi")
}

/**
 * Real chat message domain model with idempotent ID.
 */
data class ChatMessage(
    val id: String, // UUID idempotency key
    val conversationId: String,
    val companyId: String,
    val type: ConversationType,
    val senderId: String,
    val senderName: String,
    val senderRole: MemberRole,
    val recipientId: String? = null,
    val text: String,
    val clientTimestamp: Long,
    val serverTimestamp: Long? = null,
    val syncStatus: MessageSyncStatus = MessageSyncStatus.PENDING,
    val errorMessage: String? = null
) {
    val timeFormatted: String
        get() {
            val ts = serverTimestamp ?: clientTimestamp
            val instant = Instant.ofEpochMilli(ts)
            val zdt = instant.atZone(ZoneId.of("Africa/Tunis"))
            val formatter = DateTimeFormatter.ofPattern("HH:mm", Locale.FRENCH)
            return zdt.format(formatter)
        }

    val dateFormatted: String
        get() {
            val ts = serverTimestamp ?: clientTimestamp
            val instant = Instant.ofEpochMilli(ts)
            val zdt = instant.atZone(ZoneId.of("Africa/Tunis"))
            val formatter = DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.FRENCH)
            return zdt.format(formatter).replaceFirstChar { it.uppercase() }
        }
}

/**
 * Canonical 1-to-1 conversation ID generator preventing duplicates between any two members.
 */
fun buildDirectConversationId(companyId: String, uid1: String, uid2: String): String {
    val first = if (uid1 <= uid2) uid1 else uid2
    val second = if (uid1 <= uid2) uid2 else uid1
    return "direct_${companyId}_${first}_${second}"
}

/**
 * Canonical group conversation ID for a company.
 */
fun buildGroupConversationId(companyId: String): String {
    return "group_${companyId}_main"
}

/**
 * Types of workshop announcements.
 */
enum class AnnouncementType(val labelFrench: String, val badgePrefix: String) {
    INFORMATION("Information", "Info"),
    NOTIFICATION("Notification importante", "Important"),
    SECURITE("Sécurité & hygiène", "Sécurité"),
    ORGANISATION("Organisation & horaires", "Organisation");

    companion object {
        fun fromString(value: String?): AnnouncementType {
            return when (value?.uppercase(Locale.US)?.trim()) {
                "NOTIFICATION", "IMPORTANT" -> NOTIFICATION
                "SECURITE", "HYGIENE", "SECURITY" -> SECURITE
                "ORGANISATION", "HORAIRES" -> ORGANISATION
                else -> INFORMATION
            }
        }
    }
}

/**
 * Announcement priority levels.
 */
enum class AnnouncementPriority(val labelFrench: String) {
    NORMALE("Normale"),
    IMPORTANTE("Importante"),
    URGENTE("Urgente");

    companion object {
        fun fromString(value: String?): AnnouncementPriority {
            return when (value?.uppercase(Locale.US)?.trim()) {
                "URGENTE", "URGENT" -> URGENTE
                "IMPORTANTE", "HIGH" -> IMPORTANTE
                else -> NORMALE
            }
        }
    }
}

/**
 * Target audience specification for announcements.
 */
enum class AnnouncementAudience(val labelFrench: String) {
    TOUS("Tout l'atelier"),
    CHAFAS_ONLY("Châfas uniquement"),
    OUVRIERES_ONLY("Ouvrières uniquement");

    companion object {
        fun fromString(value: String?): AnnouncementAudience {
            return when (value?.uppercase(Locale.US)?.trim()) {
                "CHAFAS_ONLY", "CHAFAS", "SUPERVISORS" -> CHAFAS_ONLY
                "OUVRIERES_ONLY", "OUVRIERES", "WORKERS" -> OUVRIERES_ONLY
                else -> TOUS
            }
        }
    }
}

/**
 * Announcement lifecycle status.
 */
enum class AnnouncementStatus(val labelFrench: String) {
    ACTIVE("Actif"),
    EXPIRED("Expiré"),
    CANCELLED("Annulé");

    companion object {
        fun fromString(value: String?): AnnouncementStatus {
            return when (value?.uppercase(Locale.US)?.trim()) {
                "EXPIRED" -> EXPIRED
                "CANCELLED", "ANNULE" -> CANCELLED
                else -> ACTIVE
            }
        }
    }
}

/**
 * Workshop announcement data model with duration, audience, and lifecycle status.
 */
data class WorkshopAnnouncement(
    val id: String, // UUID
    val companyId: String,
    val authorId: String,
    val authorName: String,
    val authorRole: MemberRole,
    val type: AnnouncementType,
    val title: String,
    val content: String,
    val priority: AnnouncementPriority,
    val targetAudience: AnnouncementAudience,
    val startEpochMs: Long,
    val endEpochMs: Long,
    val durationDays: Int,
    val status: AnnouncementStatus,
    val version: Int = 1,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long,
    val syncStatus: MessageSyncStatus = MessageSyncStatus.SYNCED,
    val isReadLocally: Boolean = false
) {
    val isCurrentlyActive: Boolean
        get() {
            if (status != AnnouncementStatus.ACTIVE) return false
            val now = System.currentTimeMillis()
            return now in startEpochMs..endEpochMs
        }

    val isExpired: Boolean
        get() {
            if (status == AnnouncementStatus.EXPIRED) return true
            return status == AnnouncementStatus.ACTIVE && System.currentTimeMillis() > endEpochMs
        }

    val isCancelled: Boolean
        get() = status == AnnouncementStatus.CANCELLED

    val formattedStartTunis: String
        get() {
            val instant = Instant.ofEpochMilli(startEpochMs)
            val zdt = instant.atZone(ZoneId.of("Africa/Tunis"))
            val formatter = DateTimeFormatter.ofPattern("d MMMM yyyy 'à' HH:mm", Locale.FRENCH)
            return zdt.format(formatter)
        }

    val formattedEndTunis: String
        get() {
            val instant = Instant.ofEpochMilli(endEpochMs)
            val zdt = instant.atZone(ZoneId.of("Africa/Tunis"))
            val formatter = DateTimeFormatter.ofPattern("d MMMM yyyy 'à' HH:mm", Locale.FRENCH)
            return zdt.format(formatter)
        }

    fun isAudienceEligible(member: CompanyMember): Boolean {
        if (member.role == MemberRole.DIRECTRICE) return true
        return when (targetAudience) {
            AnnouncementAudience.TOUS -> true
            AnnouncementAudience.CHAFAS_ONLY -> member.role == MemberRole.CHAFE
            AnnouncementAudience.OUVRIERES_ONLY -> member.role == MemberRole.OUVRIERE
        }
    }
}

/**
 * Idempotent announcement read receipt.
 */
data class AnnouncementReceipt(
    val id: String, // "${announcementId}_${recipientUserId}_v${announcementVersion}"
    val announcementId: String,
    val companyId: String,
    val recipientUserId: String,
    val recipientName: String,
    val recipientRole: MemberRole,
    val firstReadAtEpochMs: Long,
    val lastReadAtEpochMs: Long,
    val announcementVersion: Int = 1,
    val syncStatus: MessageSyncStatus = MessageSyncStatus.SYNCED
) {
    val firstReadTimeFormattedTunis: String
        get() {
            val instant = Instant.ofEpochMilli(firstReadAtEpochMs)
            val zdt = instant.atZone(ZoneId.of("Africa/Tunis"))
            val formatter = DateTimeFormatter.ofPattern("HH:mm", Locale.FRENCH)
            return zdt.format(formatter)
        }

    val firstReadDateFormattedTunis: String
        get() {
            val instant = Instant.ofEpochMilli(firstReadAtEpochMs)
            val zdt = instant.atZone(ZoneId.of("Africa/Tunis"))
            val formatter = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.FRENCH)
            return zdt.format(formatter)
        }
}

/**
 * Aggregated read statistics for manager review.
 */
data class AnnouncementReadStats(
    val announcementId: String,
    val targetAudienceCount: Int,
    val readCount: Int,
    val unreadCount: Int,
    val readers: List<AnnouncementReceipt>,
    val unreadMembers: List<CompanyMember>
) {
    val summaryTextFrench: String
        get() = "Lu par $readCount / $targetAudienceCount"

    val unreadSummaryTextFrench: String
        get() = "Non lu par $unreadCount"
}

/**
 * Audit log entry for announcement management operations.
 */
data class AnnouncementAuditEntry(
    val id: String,
    val companyId: String,
    val announcementId: String,
    val action: String, // "CREATED", "UPDATED", "CANCELLED"
    val actorId: String,
    val actorName: String,
    val actorRole: String,
    val timestampEpochMs: Long,
    val details: String = ""
)

