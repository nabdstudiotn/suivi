package com.example.domain.model

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Tab destinations within the centralized Paramètres section.
 */
enum class SettingsTab(val labelFr: String) {
    COMPTE("Mon Compte"),
    ENTREPRISE("Atelier"),
    PERMISSIONS("Permissions"),
    APPAREILS("Appareils"),
    AUDIT_LOG("Journal d'activité")
}

/**
 * Time filter for the audit log in Paramètres.
 */
enum class AuditDateFilter(val labelFr: String) {
    TOUS("Tous les jours"),
    AUJOURDHUI("Aujourd'hui"),
    SEMAINE("7 derniers jours")
}

/**
 * Account profile of the authenticated workshop user (Directrice or Châfa).
 */
data class UserAccountProfile(
    val userId: String,
    val companyId: String,
    val displayName: String,
    val email: String,
    val phone: String,
    val role: MemberRole,
    val status: MembershipStatus = MembershipStatus.ACTIVE,
    val lastLoginEpochMs: Long = System.currentTimeMillis()
) {
    val formattedLastLoginTunis: String
        get() {
            val instant = Instant.ofEpochMilli(lastLoginEpochMs)
            val zdt = instant.atZone(ZoneId.of("Africa/Tunis"))
            val formatter = DateTimeFormatter.ofPattern("dd MMM yyyy 'à' HH:mm", Locale.FRENCH)
            return "${zdt.format(formatter)} (Heure de Tunis)"
        }
}

/**
 * Workshop / Company profile settings.
 */
data class CompanyProfile(
    val companyId: String,
    val name: String,
    val ownerId: String,
    val ownerName: String = "Directrice",
    val matriculeFiscal: String = "1458921/A/M/000",
    val address: String = "Zone Industrielle La Charguia 1, 2035 Tunis",
    val phone: String = "+216 71 840 220",
    val standardDailyHours: Int = 8,
    val referenceRateMillimes: Long = 3500L,
    val currency: String = "TND"
) {
    val referenceRateFormattedTnd: String
        get() = formatMillimesAsTnd(referenceRateMillimes)
}

/**
 * Granular permissions granted by the Directrice to the Châfa.
 * Enforced on operations and fully tracked via audit log.
 */
data class ChafaPermissions(
    val companyId: String,
    val canEditPastAttendance: Boolean = false,
    val canRecordAdvances: Boolean = true,
    val canPublishAnnouncements: Boolean = false,
    val canViewPayrollFinancials: Boolean = false,
    val canManageWorkers: Boolean = false,
    val canViewAuditLogs: Boolean = true,
    val canCloseArchive: Boolean = false,
    val lastUpdatedEpochMs: Long = System.currentTimeMillis()
)

/**
 * Active device session connected to the workshop platform.
 */
data class DeviceSession(
    val id: String,
    val companyId: String,
    val userId: String,
    val userName: String,
    val userRole: MemberRole,
    val deviceName: String,
    val deviceType: String = "TABLET", // TABLET, PHONE, DESKTOP
    val ipOrLocation: String = "Tunis, Tunisie",
    val isCurrentDevice: Boolean = false,
    val isRevoked: Boolean = false,
    val lastActiveEpochMs: Long = System.currentTimeMillis()
) {
    val formattedLastActiveTunis: String
        get() {
            val instant = Instant.ofEpochMilli(lastActiveEpochMs)
            val zdt = instant.atZone(ZoneId.of("Africa/Tunis"))
            val formatter = DateTimeFormatter.ofPattern("dd MMM yyyy 'à' HH:mm", Locale.FRENCH)
            return "${zdt.format(formatter)} (Heure de Tunis)"
        }
}
