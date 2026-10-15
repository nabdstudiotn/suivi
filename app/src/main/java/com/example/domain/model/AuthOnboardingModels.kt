package com.example.domain.model

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Status of a worker/supervisor join request to an enterprise.
 */
enum class JoinRequestStatus(val labelFr: String) {
    PENDING("En attente de validation"),
    APPROVED("Approuvée"),
    REFUSED("Refusée");

    companion object {
        fun fromString(value: String?): JoinRequestStatus {
            return when (value?.uppercase(Locale.US)?.trim()) {
                "APPROVED" -> APPROVED
                "REFUSED" -> REFUSED
                else -> PENDING
            }
        }
    }
}

/**
 * Join request sent by an Ouvrière or Superviseuse to join a company.
 */
data class JoinRequest(
    val id: String,
    val companyId: String,
    val companyName: String,
    val userId: String,
    val userEmail: String,
    val userFullName: String,
    val userPhone: String,
    val requestedRole: MemberRole,
    val note: String? = null,
    val status: JoinRequestStatus = JoinRequestStatus.PENDING,
    val reviewedByUid: String? = null,
    val reviewedByName: String? = null,
    val assignedWorkerId: String? = null,
    val createdAtEpochMs: Long = System.currentTimeMillis(),
    val reviewedAtEpochMs: Long? = null
) {
    val formattedDateFr: String
        get() {
            val instant = Instant.ofEpochMilli(createdAtEpochMs)
            val zdt = instant.atZone(ZoneId.of("Africa/Tunis"))
            val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy 'à' HH:mm", Locale.FRENCH)
            return zdt.format(formatter)
        }
}

/**
 * User account profile persisted in /users/{uid}.
 */
data class UserAccount(
    val uid: String,
    val email: String,
    val displayName: String,
    val phone: String,
    val createdAtEpochMs: Long = System.currentTimeMillis()
)

/**
 * Active resolution state for the Suivi authentication & membership gate.
 */
sealed interface AuthGateState {
    // Unauthenticated
    data object Unauthenticated : AuthGateState

    // Authenticated in Firebase, resolving memberships in database
    data object ResolvingMembership : AuthGateState

    // Firebase is authenticated but the cloud lookup failed; never crash the launch screen.
    data class CloudUnavailable(val user: UserAccount, val message: String) : AuthGateState

    // Authenticated, but no company and no pending request
    data class NoCompany(val user: UserAccount) : AuthGateState

    // Has a pending join request awaiting Directrice approval
    data class PendingApproval(val user: UserAccount, val request: JoinRequest) : AuthGateState

    // Membership is suspended or removed by Directrice
    data class Blocked(val user: UserAccount, val membership: CompanyMember, val status: MembershipStatus) : AuthGateState

    // Has multiple active companies; user must select which one to open
    data class MultipleCompanies(val user: UserAccount, val memberships: List<CompanyMember>) : AuthGateState

    // Fully resolved active membership! Ready to launch company interface
    data class ActiveWorkspace(val user: UserAccount, val membership: CompanyMember) : AuthGateState
}
