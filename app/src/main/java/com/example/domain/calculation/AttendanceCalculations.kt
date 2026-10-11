package com.example.domain.calculation

import com.example.domain.model.AdjustmentType
import com.example.domain.model.AdvanceRecord
import com.example.domain.model.AttendanceEntry
import com.example.domain.model.AttendanceStatus
import com.example.domain.model.HourlyRateRecord
import com.example.domain.model.PaymentRecord
import com.example.domain.model.PayrollAdjustment
import com.example.domain.model.WorkedMonthSummary
import com.example.domain.model.formatMillimesAsTnd
import java.math.BigDecimal
import java.math.RoundingMode

enum class PayrollAuditGrade(val labelFrench: String) {
    GRADE_1_APPROUVE("Grade 1 — Conforme"),
    GRADE_2_AVERTISSEMENT("Grade 2 — Vérification recommandée"),
    GRADE_3_BLOQUE("Grade 3 — À revoir")
}

enum class PayrollEligibilityStatus(val labelFrench: String) {
    ELIGIBLE_FINAL("Calcul final approuvable"),
    ELIGIBLE_ARRET_ACTIVITE("Arrêt d'activité — Calcul final"),
    EN_COURS_MOIS_OUVERT("Calcul en cours"),
    INCOMPLET("Incomplet"),
    A_VERIFIER("À vérifier")
}

/**
 * Deterministic payroll calculation result for a single worker in a month.
 * All financial amounts use exact integer minor units (millimes: 1 TND = 1000 millimes)
 * without any binary floating point arithmetic.
 */
data class WorkerPayrollBreakdown(
    val workerId: String,
    val yearMonth: String,
    val totalMinutes: Int,
    val workedDaysCount: Int,
    val absentDaysCount: Int,
    val restDaysCount: Int,
    val blankDaysCount: Int,
    val basePayMillimes: Long,
    val advancesMillimes: Long,
    val primesMillimes: Long,
    val primeAidMillimes: Long,
    val retenuesMillimes: Long,
    val oldUnpaidBalanceMillimes: Long,
    val totalDueMillimes: Long, // Base + Primes + PrimeAid - Retenues - Advances + OldBalance
    val paidAmountMillimes: Long,
    val remainingDueMillimes: Long, // TotalDue - PaidAmount
    val auditGrade: PayrollAuditGrade = PayrollAuditGrade.GRADE_1_APPROUVE,
    val eligibilityStatus: PayrollEligibilityStatus = PayrollEligibilityStatus.ELIGIBLE_FINAL,
    val isEligibleForFinal: Boolean = true,
    val statusLabel: String = "Calcul en cours"
) {
    val isGrade3Blocked: Boolean
        get() = auditGrade == PayrollAuditGrade.GRADE_3_BLOQUE || blankDaysCount > 0

    val totalHoursFormatted: String
        get() {
            val h = totalMinutes / 60
            val m = totalMinutes % 60
            return if (m == 0) "$h h" else "$h h ${m.toString().padStart(2, '0')}"
        }

    val basePayFormattedTnd: String
        get() = formatMillimesAsTnd(basePayMillimes)

    val advancesFormattedTnd: String
        get() = formatMillimesAsTnd(advancesMillimes)

    val primesFormattedTnd: String
        get() = formatMillimesAsTnd(primesMillimes + primeAidMillimes)

    val retenuesFormattedTnd: String
        get() = formatMillimesAsTnd(retenuesMillimes)

    val totalDueFormattedTnd: String
        get() = formatMillimesAsTnd(totalDueMillimes)

    val paidAmountFormattedTnd: String
        get() = formatMillimesAsTnd(paidAmountMillimes)

    val remainingDueFormattedTnd: String
        get() = formatMillimesAsTnd(remainingDueMillimes)
}

/**
 * Result of independent calculation cross-checking.
 */
data class CalculationResult(
    val totalMinutes: Int,
    val decimalHours: Double,
    val grossPayMillimes: Long,
    val grossPayFormattedTnd: String,
    val workedDaysCount: Int,
    val absentDaysCount: Int,
    val restDaysCount: Int,
    val blankDaysCount: Int,
    val auditDiscrepancyDetected: Boolean
)

/**
 * Deterministic calculation engine for Suivi.
 * Rule: Pure Kotlin logic, independent of UI and strictly NO AI scoring.
 */
object AtelierCalculations {

    /**
     * Finds the effective hourly rate (in millimes) for a given date from the worker's history.
     * Selects the rate where effectiveDateIso <= dateIso, ordered by effectiveDateIso descending.
     */
    fun findEffectiveRateForDate(
        dateIso: String,
        rateHistory: List<HourlyRateRecord>,
        fallbackRateMillimes: Long
    ): Long {
        if (rateHistory.isEmpty()) return fallbackRateMillimes
        val applicable = rateHistory
            .filter { it.effectiveDateIso <= dateIso }
            .maxByOrNull { it.effectiveDateIso }
        return applicable?.rateMillimes ?: fallbackRateMillimes
    }

    /**
     * Method 1: Exact integer millimes daily summation.
     * For each day: (minutes * rateMillimes + 30) / 60
     */
    fun calculateGrossPayMillimes(
        entries: List<AttendanceEntry>,
        rateHistory: List<HourlyRateRecord>,
        fallbackRateMillimes: Long
    ): Long {
        var totalMillimes: Long = 0L
        for (entry in entries) {
            if (entry.status == AttendanceStatus.WORKED) {
                val effectiveRate = findEffectiveRateForDate(entry.dateIso, rateHistory, fallbackRateMillimes)
                val dayMinutes = (entry.hours * 60) + entry.minutes
                // Exact integer division with standard round-half-up (+ 30 before integer div by 60)
                val dayPay = (dayMinutes.toLong() * effectiveRate + 30L) / 60L
                totalMillimes += dayPay
            }
        }
        return totalMillimes
    }

    /**
     * Method 2: BigDecimal arithmetic with high intermediate precision.
     */
    fun calculateGrossPayBigDecimal(
        entries: List<AttendanceEntry>,
        rateHistory: List<HourlyRateRecord>,
        fallbackRateMillimes: Long
    ): BigDecimal {
        var total = BigDecimal.ZERO
        for (entry in entries) {
            if (entry.status == AttendanceStatus.WORKED) {
                val effectiveRate = findEffectiveRateForDate(entry.dateIso, rateHistory, fallbackRateMillimes)
                val dayMinutes = (entry.hours * 60) + entry.minutes
                val rateTnd = BigDecimal.valueOf(effectiveRate).divide(BigDecimal.valueOf(1000), 6, RoundingMode.HALF_UP)
                val hours = BigDecimal.valueOf(dayMinutes.toLong()).divide(BigDecimal.valueOf(60), 6, RoundingMode.HALF_UP)
                total = total.add(hours.multiply(rateTnd))
            }
        }
        return total.setScale(3, RoundingMode.HALF_UP)
    }

    /**
     * Total minutes worked count.
     */
    fun calculateTotalMinutes(entries: List<AttendanceEntry>): Int {
        return entries
            .filter { it.status == AttendanceStatus.WORKED }
            .sumOf { (it.hours * 60) + it.minutes }
    }

    /**
     * Decimal hours helper.
     */
    fun calculateDecimalHours(entries: List<AttendanceEntry>): Double {
        return entries
            .filter { it.status == AttendanceStatus.WORKED }
            .sumOf { it.hours + (it.minutes / 60.0) }
    }

    /**
     * Comprehensive deterministic payroll calculation according to Section 5:
     * Base = sum(worked hours * effective rate)
     * Total Due = Base + Prime + PrimeAid - Retenue - Advances + OldBalance
     * Remaining = Total Due - Paid
     */
    fun calculateWorkerMonthPayroll(
        workerId: String,
        yearMonth: String,
        monthEntries: List<AttendanceEntry>,
        rateHistory: List<HourlyRateRecord>,
        fallbackRateMillimes: Long,
        advances: List<AdvanceRecord>,
        adjustments: List<PayrollAdjustment>,
        payments: List<PaymentRecord>,
        oldUnpaidBalanceMillimes: Long = 0L,
        worker: com.example.domain.model.Worker? = null,
        referenceDate: com.example.domain.model.AtelierDate = com.example.domain.model.AtelierDate.todayTunis()
    ): WorkerPayrollBreakdown {
        val totalMinutes = calculateTotalMinutes(monthEntries)
        val basePay = calculateGrossPayMillimes(monthEntries, rateHistory, fallbackRateMillimes)

        val advancesTotal = advances
            .filter { it.workerId == workerId && it.dateIso.startsWith(yearMonth) }
            .sumOf { it.amountMillimes }

        val workerAdjustments = adjustments.filter {
            (it.workerId == workerId || it.workerId == "ALL") && it.yearMonth == yearMonth
        }

        val primesTotal = workerAdjustments
            .filter { it.type == AdjustmentType.PRIME }
            .sumOf { it.amountMillimes }

        val primeAidTotal = workerAdjustments
            .filter { it.type == AdjustmentType.PRIME_AID }
            .sumOf { it.amountMillimes }

        val retenuesTotal = workerAdjustments
            .filter { it.type == AdjustmentType.RETENUE }
            .sumOf { it.amountMillimes }

        val totalDue = basePay + primesTotal + primeAidTotal - retenuesTotal - advancesTotal + oldUnpaidBalanceMillimes

        val paidTotal = payments
            .filter { it.workerId == workerId && it.yearMonth == yearMonth }
            .sumOf { it.amountMillimes }

        val remainingDue = totalDue - paidTotal

        val workedDays = monthEntries.count { it.status == AttendanceStatus.WORKED }
        val absentDays = monthEntries.count { it.status == AttendanceStatus.ABSENT }
        val restDays = monthEntries.count { it.status == AttendanceStatus.REST_DAY }
        val blankDays = monthEntries.count { it.status == AttendanceStatus.BLANK }

        // Eligibility determination:
        // Rule: Calculation becomes eligible ONLY when month has ended OR worker has a real persisted employment stop before month end.
        val daysInMonth = com.example.domain.model.AtelierDate.daysInYearMonth(yearMonth)
        val isMonthPast = referenceDate.yearMonthIso > yearMonth ||
                (referenceDate.yearMonthIso == yearMonth && referenceDate.day >= daysInMonth)

        val hasRealWorkerStop = worker != null && (!worker.isActive || worker.archivedDateIso != null) &&
                run {
                    val stopDate = worker.archivedDateIso
                    stopDate != null && stopDate.startsWith(yearMonth) && (worker.returnDateIso == null || worker.returnDateIso > stopDate)
                }

        val isEligible = isMonthPast || hasRealWorkerStop

        // Audit Grade determination:
        // Grade 3 (Blocked): UNRECORDED days remaining in employment period, or negative total due, or missing valid rates
        val hasMissingRate = rateHistory.isEmpty() && fallbackRateMillimes <= 0L
        val auditGrade = when {
            blankDays > 0 || hasMissingRate -> PayrollAuditGrade.GRADE_3_BLOQUE
            absentDays > 10 || (advancesTotal > basePay && basePay > 0) -> PayrollAuditGrade.GRADE_2_AVERTISSEMENT
            else -> PayrollAuditGrade.GRADE_1_APPROUVE
        }

        val eligibilityStatus = when {
            blankDays > 0 -> PayrollEligibilityStatus.INCOMPLET
            auditGrade == PayrollAuditGrade.GRADE_3_BLOQUE -> PayrollEligibilityStatus.A_VERIFIER
            hasRealWorkerStop -> PayrollEligibilityStatus.ELIGIBLE_ARRET_ACTIVITE
            isMonthPast -> PayrollEligibilityStatus.ELIGIBLE_FINAL
            else -> PayrollEligibilityStatus.EN_COURS_MOIS_OUVERT
        }

        val statusLabel = when (eligibilityStatus) {
            PayrollEligibilityStatus.INCOMPLET -> "Incomplet"
            PayrollEligibilityStatus.A_VERIFIER -> "À vérifier"
            PayrollEligibilityStatus.ELIGIBLE_ARRET_ACTIVITE -> "Arrêt d'activité — Calcul final"
            PayrollEligibilityStatus.ELIGIBLE_FINAL -> "Calcul final approuvable"
            PayrollEligibilityStatus.EN_COURS_MOIS_OUVERT -> "Calcul en cours"
        }

        return WorkerPayrollBreakdown(
            workerId = workerId,
            yearMonth = yearMonth,
            totalMinutes = totalMinutes,
            workedDaysCount = workedDays,
            absentDaysCount = absentDays,
            restDaysCount = restDays,
            blankDaysCount = blankDays,
            basePayMillimes = basePay,
            advancesMillimes = advancesTotal,
            primesMillimes = primesTotal,
            primeAidMillimes = primeAidTotal,
            retenuesMillimes = retenuesTotal,
            oldUnpaidBalanceMillimes = oldUnpaidBalanceMillimes,
            totalDueMillimes = totalDue,
            paidAmountMillimes = paidTotal,
            remainingDueMillimes = remainingDue,
            auditGrade = auditGrade,
            eligibilityStatus = eligibilityStatus,
            isEligibleForFinal = isEligible,
            statusLabel = statusLabel
        )
    }

    /**
     * Cross-verifies calculations and counts attendance statuses.
     */
    fun auditCalculation(
        entries: List<AttendanceEntry>,
        rateHistory: List<HourlyRateRecord> = emptyList(),
        fallbackRateMillimes: Long
    ): CalculationResult {
        val totalMinutes = calculateTotalMinutes(entries)
        val decimalHours = calculateDecimalHours(entries)
        val grossMillimes = calculateGrossPayMillimes(entries, rateHistory, fallbackRateMillimes)
        val grossBd = calculateGrossPayBigDecimal(entries, rateHistory, fallbackRateMillimes)

        val bdAsMillimes = grossBd.multiply(BigDecimal.valueOf(1000)).toLong()
        val discrepancy = kotlin.math.abs(grossMillimes - bdAsMillimes)
        val isDiscrepancy = discrepancy > 50L

        var worked = 0
        var absent = 0
        var rest = 0
        var blank = 0

        for (e in entries) {
            when (e.status) {
                AttendanceStatus.WORKED -> worked++
                AttendanceStatus.ABSENT -> absent++
                AttendanceStatus.REST_DAY -> rest++
                AttendanceStatus.BLANK -> blank++
            }
        }

        return CalculationResult(
            totalMinutes = totalMinutes,
            decimalHours = decimalHours,
            grossPayMillimes = grossMillimes,
            grossPayFormattedTnd = formatMillimesAsTnd(grossMillimes),
            workedDaysCount = worked,
            absentDaysCount = absent,
            restDaysCount = rest,
            blankDaysCount = blank,
            auditDiscrepancyDetected = isDiscrepancy
        )
    }

    /**
     * Filters and returns month summaries for ONLY the months where the worker actually worked.
     * Rule: An archive route that shows only months in which the worker actually worked.
     */
    fun calculateWorkedMonthsSummaries(
        allEntriesForWorker: List<AttendanceEntry>,
        rateHistory: List<HourlyRateRecord>,
        fallbackRateMillimes: Long,
        advances: List<AdvanceRecord> = emptyList(),
        adjustments: List<PayrollAdjustment> = emptyList(),
        payments: List<PaymentRecord> = emptyList()
    ): List<WorkedMonthSummary> {
        val groupedByMonth = allEntriesForWorker.groupBy { it.dateIso.substring(0, 7) }

        val result = mutableListOf<WorkedMonthSummary>()

        for ((yearMonth, entries) in groupedByMonth.toSortedMap(Comparator.reverseOrder())) {
            val workedEntries = entries.filter { it.status == AttendanceStatus.WORKED }
            val workedMinutes = calculateTotalMinutes(workedEntries)

            // Include ONLY if there are actually worked hours (> 0 minutes) or explicit recorded activity
            if (workedMinutes > 0) {
                val absentCount = entries.count { it.status == AttendanceStatus.ABSENT }
                val basePay = calculateGrossPayMillimes(entries, rateHistory, fallbackRateMillimes)

                val monthAdvances = advances
                    .filter { it.dateIso.startsWith(yearMonth) }
                    .sumOf { it.amountMillimes }

                val monthAdjustments = adjustments
                    .filter { it.yearMonth == yearMonth }
                    .sumOf {
                        if (it.type == AdjustmentType.RETENUE) -it.amountMillimes else it.amountMillimes
                    }

                val totalDue = basePay + monthAdjustments - monthAdvances

                val monthPayments = payments
                    .filter { it.yearMonth == yearMonth }
                    .sumOf { it.amountMillimes }

                val remaining = totalDue - monthPayments

                val parts = yearMonth.split("-")
                val year = parts[0].toIntOrNull() ?: 2026
                val month = parts[1].toIntOrNull() ?: 1
                val monthName = when (month) {
                    1 -> "Janvier"; 2 -> "Février"; 3 -> "Mars"; 4 -> "Avril"
                    5 -> "Mai"; 6 -> "Juin"; 7 -> "Juillet"; 8 -> "Août"
                    9 -> "Septembre"; 10 -> "Octobre"; 11 -> "Novembre"; 12 -> "Décembre"
                    else -> ""
                }

                result.add(
                    WorkedMonthSummary(
                        yearMonth = yearMonth,
                        displayNameFrench = "$monthName $year",
                        workedDaysCount = workedEntries.size,
                        absentDaysCount = absentCount,
                        totalMinutes = workedMinutes,
                        basePayMillimes = basePay,
                        advancesMillimes = monthAdvances,
                        adjustmentsMillimes = monthAdjustments,
                        totalDueMillimes = totalDue,
                        paidAmountMillimes = monthPayments,
                        remainingDueMillimes = remaining
                    )
                )
            }
        }

        return result
    }
}
