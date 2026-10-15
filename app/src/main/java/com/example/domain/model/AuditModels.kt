package com.example.domain.model

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * High-level audit categories for workshop activity logs.
 */
enum class AuditActionCategory(val labelFr: String) {
    ALL("Tous"),
    POINTAGE("Pointage"),
    EQUIPE("Équipe"),
    PAIE("Paie"),
    COMMUNICATION("Communication"),
    PARAMETRES("Paramètres"),
    SECURITE("Sécurité & Sessions"),
    ARCHIVES("Archives")
}

/**
 * Granular action types tracked by the workshop audit system.
 */
enum class AuditActionType(val category: AuditActionCategory, val labelFr: String) {
    POINTAGE_SAVED(AuditActionCategory.POINTAGE, "Pointage enregistré"),
    POINTAGE_ABSENT(AuditActionCategory.POINTAGE, "Absence signalée"),
    POINTAGE_REST_DAY(AuditActionCategory.POINTAGE, "Jour de repos marqué"),
    POINTAGE_RESET(AuditActionCategory.POINTAGE, "Pointage réinitialisé"),

    WORKER_ADDED(AuditActionCategory.EQUIPE, "Nouvelle ouvrière ajoutée"),
    WORKER_STATUS_CHANGED(AuditActionCategory.EQUIPE, "Statut ouvrière modifié"),
    HOURLY_RATE_UPDATED(AuditActionCategory.EQUIPE, "Taux horaire modifié"),

    ADVANCE_RECORDED(AuditActionCategory.PAIE, "Acompte enregistré"),
    PAYROLL_ADJUSTED(AuditActionCategory.PAIE, "Ajustement de paie enregistré"),
    PAYMENT_SETTLED(AuditActionCategory.PAIE, "Règlement de salaire effectué"),

    ANNOUNCEMENT_CREATED(AuditActionCategory.COMMUNICATION, "Annonce publiée"),
    ANNOUNCEMENT_CANCELLED(AuditActionCategory.COMMUNICATION, "Annonce annulée"),
    MESSAGE_SENT(AuditActionCategory.COMMUNICATION, "Message envoyé"),

    COMPANY_CREATED(AuditActionCategory.PARAMETRES, "Espace entreprise créé"),
    COMPANY_UPDATED(AuditActionCategory.PARAMETRES, "Profil d'atelier mis à jour"),
    USER_PROFILE_UPDATED(AuditActionCategory.PARAMETRES, "Profil utilisateur mis à jour"),
    PERMISSION_CHANGED(AuditActionCategory.PARAMETRES, "Permissions & droits modifiés"),
    JOIN_REQUEST_SUBMITTED(AuditActionCategory.PARAMETRES, "Demande d'adhésion soumise"),
    JOIN_REQUEST_APPROVED(AuditActionCategory.PARAMETRES, "Demande d'adhésion approuvée"),
    JOIN_REQUEST_REFUSED(AuditActionCategory.PARAMETRES, "Demande d'adhésion refusée"),
    MEMBER_SUSPENDED(AuditActionCategory.PARAMETRES, "Membre d'atelier suspendu"),
    MEMBER_REMOVED(AuditActionCategory.PARAMETRES, "Accès membre révoqué"),

    SESSION_CREATED(AuditActionCategory.SECURITE, "Nouvelle session / appareil"),
    SESSION_REVOKED(AuditActionCategory.SECURITE, "Session d'appareil révoquée"),

    ARCHIVE_MONTH_CLOSED(AuditActionCategory.ARCHIVES, "Mois archivé et clôturé"),
    ARCHIVE_MONTH_REOPENED(AuditActionCategory.ARCHIVES, "Réouverture du mois pour révision"),
    ARCHIVE_MONTH_REVISED(AuditActionCategory.ARCHIVES, "Révision d'archive validée"),
    ARCHIVE_CONSULTED(AuditActionCategory.ARCHIVES, "Consultation d'archive certifiée");

    companion object {
        fun fromString(value: String?): AuditActionType {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: POINTAGE_SAVED
        }
    }
}

/**
 * Real dated audit event recording all actions taken by Directrice and Châfa.
 * Strictly isolated per company (companyId) with complete attribution.
 */
data class AuditEvent(
    val id: String,
    val companyId: String,
    val actionType: AuditActionType,
    val actorId: String,
    val actorName: String,
    val actorRole: MemberRole,
    val targetEntityId: String? = null,
    val targetEntityName: String? = null,
    val detailsFr: String,
    val metadata: Map<String, String> = emptyMap(),
    val timestampEpochMs: Long = System.currentTimeMillis(),
    val ipOrDeviceId: String = "Tablette Atelier (Android)",
    val syncStatus: MessageSyncStatus = MessageSyncStatus.SYNCED
) {
    val formattedDateTimeTunis: String
        get() {
            val instant = Instant.ofEpochMilli(timestampEpochMs)
            val zdt = instant.atZone(ZoneId.of("Africa/Tunis"))
            val formatter = DateTimeFormatter.ofPattern("dd MMM yyyy 'à' HH:mm", Locale.FRENCH)
            return "${zdt.format(formatter)} (Heure de Tunis)"
        }

    val formattedDateOnlyTunis: String
        get() {
            val instant = Instant.ofEpochMilli(timestampEpochMs)
            val zdt = instant.atZone(ZoneId.of("Africa/Tunis"))
            val formatter = DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale.FRENCH)
            return zdt.format(formatter)
        }

    val formattedTimeOnlyTunis: String
        get() {
            val instant = Instant.ofEpochMilli(timestampEpochMs)
            val zdt = instant.atZone(ZoneId.of("Africa/Tunis"))
            val formatter = DateTimeFormatter.ofPattern("HH:mm", Locale.FRENCH)
            return zdt.format(formatter)
        }
}
