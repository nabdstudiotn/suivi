package com.example.domain.model

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Lifecycle status of a workshop monthly archive.
 */
enum class ArchiveMonthStatus(val labelFr: String) {
    OUVERT("Ouvert"),
    CLOTURE("Clôturé"),
    EN_REVISION("En révision");

    companion object {
        fun fromString(value: String?): ArchiveMonthStatus {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: OUVERT
        }
    }
}

/**
 * Record of monthly archive certification, locking, and revision lifecycle.
 */
data class MonthClosureRecord(
    val yearMonth: String, // e.g. "2026-05"
    val companyId: String,
    val status: ArchiveMonthStatus = ArchiveMonthStatus.OUVERT,
    val closedBy: String = "",
    val closedByRole: MemberRole = MemberRole.DIRECTRICE,
    val closedAtEpochMs: Long = System.currentTimeMillis(),
    val revisionCount: Int = 0,
    val revisionReason: String? = null,
    val notes: String? = null
) {
    val formattedClosedDateTunis: String
        get() {
            if (closedAtEpochMs <= 0) return "-"
            val instant = Instant.ofEpochMilli(closedAtEpochMs)
            val zdt = instant.atZone(ZoneId.of("Africa/Tunis"))
            val formatter = DateTimeFormatter.ofPattern("dd MMMM yyyy 'à' HH:mm", Locale.FRENCH)
            return zdt.format(formatter)
        }
}

/**
 * Daily breakdown for a specific worker within an archived month.
 */
data class DailyAttendanceRecordDetail(
    val dateIso: String,
    val dayNameFrench: String,
    val status: AttendanceStatus,
    val hoursWorked: Int,
    val minutesWorked: Int,
    val note: String? = null
) {
    val formattedHours: String
        get() = "${hoursWorked}h ${minutesWorked.toString().padStart(2, '0')}"
}

/**
 * Detailed archive record of a worker for a given month, derived strictly from confirmed pointage.
 * Preserved even if worker is subsequently archived or removed from active pointage.
 */
data class MonthlyWorkerArchiveItem(
    val workerId: String,
    val workerFullName: String,
    val workerRole: String,
    val workerCategory: String = "Atelier",
    val isActive: Boolean,
    val yearMonth: String,
    val workedDaysCount: Int,
    val absentDaysCount: Int,
    val restDaysCount: Int,
    val totalMinutes: Int,
    val basePayMillimes: Long,
    val appliedRateMillimes: Long,
    val dailyRecords: List<DailyAttendanceRecordDetail> = emptyList()
) {
    val totalHoursFormatted: String
        get() {
            val hours = totalMinutes / 60
            val minutes = totalMinutes % 60
            return "${hours}h ${minutes.toString().padStart(2, '0')}"
        }

    val grossPayFormattedTnd: String
        get() = formatMillimesAsTnd(basePayMillimes)

    val appliedRateFormattedTnd: String
        get() = formatMillimesAsTnd(appliedRateMillimes)
}

/**
 * General monthly summary for the entire workshop, derived from confirmed Pointage entries.
 */
data class WorkshopMonthlyArchiveSummary(
    val yearMonth: String,
    val displayNameFrench: String,
    val status: ArchiveMonthStatus,
    val closure: MonthClosureRecord?,
    val totalWorkersActive: Int,
    val totalMinutesWorked: Int,
    val totalDaysWorked: Int,
    val totalAbsences: Int,
    val totalGrossPayrollMillimes: Long,
    val workers: List<MonthlyWorkerArchiveItem>
) {
    val totalHoursWorkedFormatted: String
        get() {
            val hours = totalMinutesWorked / 60
            val minutes = totalMinutesWorked % 60
            return "${hours}h ${minutes.toString().padStart(2, '0')}"
        }

    val totalGrossPayrollFormattedTnd: String
        get() = formatMillimesAsTnd(totalGrossPayrollMillimes)
}
