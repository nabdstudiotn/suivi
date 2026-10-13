package com.example.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.domain.model.AuditActionType
import com.example.domain.model.AuditEvent
import com.example.domain.model.ChafaPermissions
import com.example.domain.model.CompanyProfile
import com.example.domain.model.DeviceSession
import com.example.domain.model.MemberRole
import com.example.domain.model.MessageSyncStatus

@Entity(
    tableName = "workshop_audit_events",
    indices = [
        Index("companyId"),
        Index("timestampEpochMs")
    ]
)
data class AuditEventEntity(
    @PrimaryKey val id: String,
    val companyId: String,
    val actionType: String,
    val actorId: String,
    val actorName: String,
    val actorRole: String,
    val targetEntityId: String? = null,
    val targetEntityName: String? = null,
    val detailsFr: String,
    val metadataJson: String = "{}",
    val timestampEpochMs: Long,
    val ipOrDeviceId: String = "Appareil Android",
    val syncStatus: String = "SYNCED"
) {
    fun toDomain(): AuditEvent = AuditEvent(
        id = id,
        companyId = companyId,
        actionType = AuditActionType.fromString(actionType),
        actorId = actorId,
        actorName = actorName,
        actorRole = MemberRole.fromString(actorRole),
        targetEntityId = targetEntityId,
        targetEntityName = targetEntityName,
        detailsFr = detailsFr,
        timestampEpochMs = timestampEpochMs,
        ipOrDeviceId = ipOrDeviceId,
        syncStatus = runCatching { MessageSyncStatus.valueOf(syncStatus) }.getOrDefault(MessageSyncStatus.SYNCED)
    )

    companion object {
        fun fromDomain(e: AuditEvent): AuditEventEntity = AuditEventEntity(
            id = e.id,
            companyId = e.companyId,
            actionType = e.actionType.name,
            actorId = e.actorId,
            actorName = e.actorName,
            actorRole = e.actorRole.name,
            targetEntityId = e.targetEntityId,
            targetEntityName = e.targetEntityName,
            detailsFr = e.detailsFr,
            timestampEpochMs = e.timestampEpochMs,
            ipOrDeviceId = e.ipOrDeviceId,
            syncStatus = e.syncStatus.name
        )
    }
}

@Entity(tableName = "company_profiles")
data class CompanyProfileEntity(
    @PrimaryKey val companyId: String,
    val name: String,
    val ownerId: String,
    val ownerName: String,
    val matriculeFiscal: String,
    val address: String,
    val phone: String,
    val standardDailyHours: Int,
    val referenceRateMillimes: Long,
    val currency: String
) {
    fun toDomain(): CompanyProfile = CompanyProfile(
        companyId = companyId,
        name = name,
        ownerId = ownerId,
        ownerName = ownerName,
        matriculeFiscal = matriculeFiscal,
        address = address,
        phone = phone,
        standardDailyHours = standardDailyHours,
        referenceRateMillimes = referenceRateMillimes,
        currency = currency
    )

    companion object {
        fun fromDomain(c: CompanyProfile): CompanyProfileEntity = CompanyProfileEntity(
            companyId = c.companyId,
            name = c.name,
            ownerId = c.ownerId,
            ownerName = c.ownerName,
            matriculeFiscal = c.matriculeFiscal,
            address = c.address,
            phone = c.phone,
            standardDailyHours = c.standardDailyHours,
            referenceRateMillimes = c.referenceRateMillimes,
            currency = c.currency
        )
    }
}

@Entity(tableName = "chafa_permissions")
data class ChafaPermissionsEntity(
    @PrimaryKey val companyId: String,
    val canEditPastAttendance: Boolean,
    val canRecordAdvances: Boolean,
    val canPublishAnnouncements: Boolean,
    val canViewPayrollFinancials: Boolean,
    val canManageWorkers: Boolean,
    val canViewAuditLogs: Boolean,
    val canCloseArchive: Boolean = false,
    val lastUpdatedEpochMs: Long
) {
    fun toDomain(): ChafaPermissions = ChafaPermissions(
        companyId = companyId,
        canEditPastAttendance = canEditPastAttendance,
        canRecordAdvances = canRecordAdvances,
        canPublishAnnouncements = canPublishAnnouncements,
        canViewPayrollFinancials = canViewPayrollFinancials,
        canManageWorkers = canManageWorkers,
        canViewAuditLogs = canViewAuditLogs,
        canCloseArchive = canCloseArchive,
        lastUpdatedEpochMs = lastUpdatedEpochMs
    )

    companion object {
        fun fromDomain(p: ChafaPermissions): ChafaPermissionsEntity = ChafaPermissionsEntity(
            companyId = p.companyId,
            canEditPastAttendance = p.canEditPastAttendance,
            canRecordAdvances = p.canRecordAdvances,
            canPublishAnnouncements = p.canPublishAnnouncements,
            canViewPayrollFinancials = p.canViewPayrollFinancials,
            canManageWorkers = p.canManageWorkers,
            canViewAuditLogs = p.canViewAuditLogs,
            canCloseArchive = p.canCloseArchive,
            lastUpdatedEpochMs = p.lastUpdatedEpochMs
        )
    }
}

@Entity(tableName = "device_sessions")
data class DeviceSessionEntity(
    @PrimaryKey val id: String,
    val companyId: String,
    val userId: String,
    val userName: String,
    val userRole: String,
    val deviceName: String,
    val deviceType: String,
    val ipOrLocation: String,
    val isCurrentDevice: Boolean,
    val isRevoked: Boolean,
    val lastActiveEpochMs: Long
) {
    fun toDomain(): DeviceSession = DeviceSession(
        id = id,
        companyId = companyId,
        userId = userId,
        userName = userName,
        userRole = MemberRole.fromString(userRole),
        deviceName = deviceName,
        deviceType = deviceType,
        ipOrLocation = ipOrLocation,
        isCurrentDevice = isCurrentDevice,
        isRevoked = isRevoked,
        lastActiveEpochMs = lastActiveEpochMs
    )

    companion object {
        fun fromDomain(s: DeviceSession): DeviceSessionEntity = DeviceSessionEntity(
            id = s.id,
            companyId = s.companyId,
            userId = s.userId,
            userName = s.userName,
            userRole = s.userRole.name,
            deviceName = s.deviceName,
            deviceType = s.deviceType,
            ipOrLocation = s.ipOrLocation,
            isCurrentDevice = s.isCurrentDevice,
            isRevoked = s.isRevoked,
            lastActiveEpochMs = s.lastActiveEpochMs
        )
    }
}
