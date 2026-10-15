package com.example.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.domain.model.Company
import com.example.domain.model.JoinRequest
import com.example.domain.model.JoinRequestStatus
import com.example.domain.model.MemberRole
import com.example.domain.model.UserAccount

@Entity(
    tableName = "join_requests",
    indices = [
        Index("companyId"),
        Index("userId"),
        Index("status")
    ]
)
data class JoinRequestEntity(
    @PrimaryKey val id: String,
    val companyId: String,
    val companyName: String,
    val userId: String,
    val userEmail: String,
    val userFullName: String,
    val userPhone: String,
    val requestedRole: String,
    val note: String?,
    val status: String,
    val reviewedByUid: String? = null,
    val reviewedByName: String? = null,
    val assignedWorkerId: String? = null,
    val createdAtEpochMs: Long,
    val reviewedAtEpochMs: Long? = null
) {
    fun toDomain(): JoinRequest = JoinRequest(
        id = id,
        companyId = companyId,
        companyName = companyName,
        userId = userId,
        userEmail = userEmail,
        userFullName = userFullName,
        userPhone = userPhone,
        requestedRole = MemberRole.fromString(requestedRole),
        note = note,
        status = JoinRequestStatus.fromString(status),
        reviewedByUid = reviewedByUid,
        reviewedByName = reviewedByName,
        assignedWorkerId = assignedWorkerId,
        createdAtEpochMs = createdAtEpochMs,
        reviewedAtEpochMs = reviewedAtEpochMs
    )

    companion object {
        fun fromDomain(r: JoinRequest): JoinRequestEntity = JoinRequestEntity(
            id = r.id,
            companyId = r.companyId,
            companyName = r.companyName,
            userId = r.userId,
            userEmail = r.userEmail,
            userFullName = r.userFullName,
            userPhone = r.userPhone,
            requestedRole = r.requestedRole.name,
            note = r.note,
            status = r.status.name,
            reviewedByUid = r.reviewedByUid,
            reviewedByName = r.reviewedByName,
            assignedWorkerId = r.assignedWorkerId,
            createdAtEpochMs = r.createdAtEpochMs,
            reviewedAtEpochMs = r.reviewedAtEpochMs
        )
    }
}

@Entity(tableName = "companies")
data class CompanyEntity(
    @PrimaryKey val id: String,
    val name: String,
    val ownerId: String,
    val ownerName: String = "",
    val phone: String = "",
    val companyCode: String = "",
    val createdAtIso: String = "",
    val createdAtEpochMs: Long = System.currentTimeMillis()
) {
    fun toDomain(): Company = Company(
        id = id,
        name = name,
        ownerId = ownerId,
        ownerName = ownerName,
        phone = phone,
        companyCode = companyCode,
        createdAtIso = createdAtIso,
        createdAtEpochMs = createdAtEpochMs
    )

    companion object {
        fun fromDomain(c: Company): CompanyEntity = CompanyEntity(
            id = c.id,
            name = c.name,
            ownerId = c.ownerId,
            ownerName = c.ownerName,
            phone = c.phone,
            companyCode = c.companyCode,
            createdAtIso = c.createdAtIso,
            createdAtEpochMs = c.createdAtEpochMs
        )
    }
}

@Entity(tableName = "user_accounts")
data class UserAccountEntity(
    @PrimaryKey val uid: String,
    val email: String,
    val displayName: String,
    val phone: String,
    val createdAtEpochMs: Long
) {
    fun toDomain(): UserAccount = UserAccount(
        uid = uid,
        email = email,
        displayName = displayName,
        phone = phone,
        createdAtEpochMs = createdAtEpochMs
    )

    companion object {
        fun fromDomain(u: UserAccount): UserAccountEntity = UserAccountEntity(
            uid = u.uid,
            email = u.email,
            displayName = u.displayName,
            phone = u.phone,
            createdAtEpochMs = u.createdAtEpochMs
        )
    }
}
