package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.domain.model.ArchiveMonthStatus
import com.example.domain.model.MemberRole
import com.example.domain.model.MonthClosureRecord

@Entity(tableName = "month_closures")
data class MonthClosureEntity(
    @PrimaryKey val yearMonth: String,
    val companyId: String,
    val status: String,
    val closedBy: String,
    val closedByRole: String,
    val closedAtEpochMs: Long,
    val revisionCount: Int,
    val revisionReason: String?,
    val notes: String?
) {
    fun toDomain(): MonthClosureRecord = MonthClosureRecord(
        yearMonth = yearMonth,
        companyId = companyId,
        status = ArchiveMonthStatus.fromString(status),
        closedBy = closedBy,
        closedByRole = MemberRole.fromString(closedByRole),
        closedAtEpochMs = closedAtEpochMs,
        revisionCount = revisionCount,
        revisionReason = revisionReason,
        notes = notes
    )

    companion object {
        fun fromDomain(d: MonthClosureRecord): MonthClosureEntity = MonthClosureEntity(
            yearMonth = d.yearMonth,
            companyId = d.companyId,
            status = d.status.name,
            closedBy = d.closedBy,
            closedByRole = d.closedByRole.name,
            closedAtEpochMs = d.closedAtEpochMs,
            revisionCount = d.revisionCount,
            revisionReason = d.revisionReason,
            notes = d.notes
        )
    }
}
