package com.example.domain.model

import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

enum class WorkerProfileStatus(val labelFrench: String) {
    ACTIVE("Active"),
    ARCHIVED("Archivée"),
    PENDING_SAVE("Brouillon"),
    CONFLICT("Conflit à vérifier"),
    READ_ONLY("Lecture seule")
}

data class WorkerDraft(
    val firstName: String = "",
    val lastName: String = "",
    val role: String = "Piqueuse",
    val rateInput: String = "6.500",
    val hireDateIso: String = "",
    val notes: String = ""
) {
    val isNotEmpty: Boolean
        get() = firstName.isNotBlank() || lastName.isNotBlank() || notes.isNotBlank() || rateInput.isNotBlank() && rateInput != "6.500"
}

fun normalizeForFrenchSearch(text: String): String {
    val normalized = java.text.Normalizer.normalize(text, java.text.Normalizer.Form.NFD)
    return "\\p{InCombiningDiacriticalMarks}+".toRegex().replace(normalized, "").lowercase(Locale.ROOT).trim()
}

/**
 * Worker profile representation.
 * Money is stored in exact integer minor units (millimes: 1 TND = 1000 millimes)
 * to eliminate binary floating point imprecision.
 */
data class Worker(
    val id: String,
    val firstName: String,
    val lastName: String,
    val initials: String,
    val currentRateMillimes: Long,
    val role: String,
    val isActive: Boolean = true,
    val profileStatus: WorkerProfileStatus = if (isActive) WorkerProfileStatus.ACTIVE else WorkerProfileStatus.ARCHIVED,
    val hireDateIso: String = "2026-01-15",
    val returnDateIso: String? = null,
    val archivedDateIso: String? = null,
    val notes: String? = null
) {
    val fullName: String
        get() = "$firstName $lastName"

    val hourlyRateTnd: Double
        get() = currentRateMillimes / 1000.0

    val rateFormattedTnd: String
        get() = formatMillimesAsTnd(currentRateMillimes)

    /**
     * Determines whether the worker has an effective employment period on the given date.
     * Rules from Section 1 (Pointage):
     * - Do not show worker before hire date.
     * - Do not show worker after cessation / archive date (unless returned).
     */
    fun isEmployedOn(dateIso: String): Boolean {
        if (dateIso < hireDateIso) return false
        if (!isActive) {
            val stopDate = archivedDateIso ?: hireDateIso
            if (dateIso >= stopDate) {
                if (returnDateIso != null && dateIso >= returnDateIso) {
                    return true
                }
                return false
            }
        } else if (archivedDateIso != null && dateIso > archivedDateIso) {
            if (returnDateIso == null || dateIso < returnDateIso) {
                return false
            }
        }
        return true
    }
}

/**
 * Historical effective-dated hourly rate.
 * Rule: When changing hourly rate, save effective date and reason.
 */
data class HourlyRateRecord(
    val id: Long = 0,
    val workerId: String,
    val effectiveDateIso: String,
    val rateMillimes: Long,
    val motif: String = "Taux initial"
) {
    val rateFormattedTnd: String
        get() = formatMillimesAsTnd(rateMillimes)
}

/**
 * Advance on salary (Avance sur salaire).
 * Deducted at full recorded value.
 */
data class AdvanceRecord(
    val id: String,
    val workerId: String,
    val dateIso: String,
    val amountMillimes: Long,
    val motif: String = "Avance sur salaire",
    val author: String = "Gérante"
) {
    val amountFormattedTnd: String
        get() = formatMillimesAsTnd(amountMillimes)
}

/**
 * Payroll adjustments: Prime, Retenue, Prime de l’Aïd.
 * Requires type, amount, affected worker, motif, author, and date.
 */
enum class AdjustmentType(val labelFrench: String) {
    PRIME("Prime"),
    RETENUE("Retenue"),
    PRIME_AID("Prime de l’Aïd")
}

data class PayrollAdjustment(
    val id: String,
    val type: AdjustmentType,
    val workerId: String, // worker id or "ALL"
    val yearMonth: String, // e.g. "2026-10"
    val dateIso: String,
    val amountMillimes: Long,
    val motif: String,
    val note: String? = null,
    val author: String = "Gérante"
) {
    val amountFormattedTnd: String
        get() = formatMillimesAsTnd(amountMillimes)
}

/**
 * Payment record for payroll settlements.
 * Distinct from calculation. Records date, amount, payer, method (default "Espèces").
 */
data class PaymentRecord(
    val id: String,
    val workerId: String,
    val yearMonth: String,
    val dateIso: String,
    val amountMillimes: Long,
    val paymentMethod: String = "Espèces",
    val payerName: String = "Gérante",
    val note: String? = null
) {
    val amountFormattedTnd: String
        get() = formatMillimesAsTnd(amountMillimes)
}

/**
 * Announcement entity for the Communication section.
 */
data class Announcement(
    val id: String,
    val title: String,
    val content: String,
    val targetAudience: String = "Tout l'atelier", // "Tout l'atelier", "Piqueuses", "Coupeuses", "Responsables"
    val expirationDateIso: String,
    val author: String = "Gérante",
    val createdAtIso: String = AtelierDate.todayTunis().isoString
)

/**
 * Monthly attendance summary for archive filtering.
 * An archived month only appears if the worker actually worked or has recorded activity.
 */
data class WorkedMonthSummary(
    val yearMonth: String, // e.g. "2026-10"
    val displayNameFrench: String, // e.g. "Octobre 2026"
    val workedDaysCount: Int,
    val absentDaysCount: Int,
    val totalMinutes: Int,
    val basePayMillimes: Long,
    val advancesMillimes: Long = 0L,
    val adjustmentsMillimes: Long = 0L,
    val oldBalanceMillimes: Long = 0L,
    val totalDueMillimes: Long = basePayMillimes,
    val paidAmountMillimes: Long = 0L,
    val remainingDueMillimes: Long = basePayMillimes,
    val isClosed: Boolean = false
) {
    val totalHoursFormatted: String
        get() {
            val h = totalMinutes / 60
            val m = totalMinutes % 60
            return if (m == 0) "$h h" else "$h h ${m.toString().padStart(2, '0')}"
        }

    val basePayFormattedTnd: String
        get() = formatMillimesAsTnd(basePayMillimes)

    val grossPayFormattedTnd: String
        get() = formatMillimesAsTnd(basePayMillimes)

    val totalDueFormattedTnd: String
        get() = formatMillimesAsTnd(totalDueMillimes)

    val remainingDueFormattedTnd: String
        get() = formatMillimesAsTnd(remainingDueMillimes)
}

/**
 * Helper function to format exact millimes to Tunisian Dinar (e.g. 6,500 TND).
 */
fun formatMillimesAsTnd(millimes: Long): String {
    val dinars = millimes / 1000
    val remainder = kotlin.math.abs(millimes % 1000)
    return String.format(Locale.FRENCH, "%d,%03d TND", dinars, remainder)
}

/**
 * View selector options for Pointage:
 * - Jour: Single day detailed view (default).
 * - Semaine: 7-day week view reading same data source.
 * - Mois: Full month overview reading same data source.
 */
enum class AttendanceViewMode(val titleFrench: String) {
    JOUR("Jour"),
    SEMAINE("Semaine"),
    MOIS("Mois")
}

/**
 * Attendance status enum representing the 4 explicit states:
 * - BLANK: UNRECORDED / À renseigner (enregistrement incomplet, ni absence ni zéro)
 * - WORKED: Heures et minutes saisies (ex: 7 h 30, stocké en minutes entières)
 * - ABSENT: AB (Absence constatée)
 * - REST_DAY: Jour de repos (repos hebdomadaire automatique le dimanche, ou congé convenu)
 */
enum class AttendanceStatus {
    BLANK,
    WORKED,
    ABSENT,
    REST_DAY;

    val isUnrecorded: Boolean
        get() = this == BLANK

    val labelFrench: String
        get() = when (this) {
            BLANK -> "À renseigner"
            WORKED -> "Heures travaillées"
            ABSENT -> "AB"
            REST_DAY -> "Jour de repos"
        }
}

/**
 * Daily attendance record for an individual worker.
 * Source of truth: workedMinutes integer (hours * 60 + minutes).
 * No Float or Double used in domain calculations.
 */
data class AttendanceEntry(
    val workerId: String,
    val dateIso: String,
    val status: AttendanceStatus,
    val hours: Int = 0,
    val minutes: Int = 0,
    val note: String? = null
) {
    val workedMinutes: Int
        get() = if (status == AttendanceStatus.WORKED) (hours * 60) + minutes else 0

    val durationFormatted: String
        get() = when (status) {
            AttendanceStatus.BLANK -> "À renseigner"
            AttendanceStatus.WORKED -> {
                if (minutes == 0) "${hours} h" else "$hours h ${minutes.toString().padStart(2, '0')}"
            }
            AttendanceStatus.ABSENT -> "AB"
            AttendanceStatus.REST_DAY -> "Jour de repos"
        }

    val totalMinutes: Int
        get() = workedMinutes

    val totalDecimalHours: Double
        get() = if (status == AttendanceStatus.WORKED) {
            hours + (minutes / 60.0)
        } else {
            0.0
        }
}

/**
 * Section navigation tabs per Section 2 of approved corrections:
 * Exact Order:
 * 1. Pointage
 * 2. Équipe
 * 3. Paie
 * 4. Communication (Annonces & Discussions)
 * 5. Archives
 * 6. Paramètres
 */
enum class AtelierSection(val titleFrench: String) {
    POINTAGE("Pointage"),
    EQUIPE("Équipe"),
    PAIE("Paie"),
    COMMUNICATION("Communication"),
    ARCHIVES("Archives"),
    PARAMETRES("Paramètres")
}

/**
 * Robust Gregorian calendar date with French formatting and Africa/Tunis timezone.
 */
data class AtelierDate(
    val year: Int,
    val month: Int, // 1-12
    val day: Int
) : Comparable<AtelierDate> {

    val isoString: String
        get() = String.format(Locale.US, "%04d-%02d-%02d", year, month, day)

    val yearMonthIso: String
        get() = String.format(Locale.US, "%04d-%02d", year, month)

    private fun toCalendar(): Calendar {
        val cal = Calendar.getInstance(TimeZone.getTimeZone("Africa/Tunis"), Locale.FRENCH)
        cal.set(Calendar.YEAR, year)
        cal.set(Calendar.MONTH, month - 1)
        cal.set(Calendar.DAY_OF_MONTH, day)
        cal.set(Calendar.HOUR_OF_DAY, 12)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal
    }

    val isSunday: Boolean
        get() {
            val cal = toCalendar()
            return cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY
        }

    val dayOfWeekNameFrench: String
        get() {
            val cal = toCalendar()
            return when (cal.get(Calendar.DAY_OF_WEEK)) {
                Calendar.MONDAY -> "Lundi"
                Calendar.TUESDAY -> "Mardi"
                Calendar.WEDNESDAY -> "Mercredi"
                Calendar.THURSDAY -> "Jeudi"
                Calendar.FRIDAY -> "Vendredi"
                Calendar.SATURDAY -> "Samedi"
                Calendar.SUNDAY -> "Dimanche"
                else -> ""
            }
        }

    val monthNameFrench: String
        get() = when (month) {
            1 -> "janvier"
            2 -> "février"
            3 -> "mars"
            4 -> "avril"
            5 -> "mai"
            6 -> "juin"
            7 -> "juillet"
            8 -> "août"
            9 -> "septembre"
            10 -> "octobre"
            11 -> "novembre"
            12 -> "décembre"
            else -> ""
        }

    val shortDayOfWeekNameFrench: String
        get() = when (toCalendar().get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> "Lun"
            Calendar.TUESDAY -> "Mar"
            Calendar.WEDNESDAY -> "Mer"
            Calendar.THURSDAY -> "Jeu"
            Calendar.FRIDAY -> "Ven"
            Calendar.SATURDAY -> "Sam"
            Calendar.SUNDAY -> "Dim"
            else -> ""
        }

    val formattedFrench: String
        get() = "$dayOfWeekNameFrench $day $monthNameFrench $year"

    val shortFrench: String
        get() = "$day $monthNameFrench"

    fun daysInMonth(): Int {
        val cal = toCalendar()
        return cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    }

    /**
     * Returns the 7 days of the week (Lundi to Dimanche) containing this date.
     */
    fun getWeekDays(): List<AtelierDate> {
        val cal = toCalendar()
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        val daysFromMonday = when (dayOfWeek) {
            Calendar.MONDAY -> 0
            Calendar.TUESDAY -> 1
            Calendar.WEDNESDAY -> 2
            Calendar.THURSDAY -> 3
            Calendar.FRIDAY -> 4
            Calendar.SATURDAY -> 5
            Calendar.SUNDAY -> 6
            else -> 0
        }
        cal.add(Calendar.DAY_OF_MONTH, -daysFromMonday)
        val result = mutableListOf<AtelierDate>()
        for (i in 0 until 7) {
            result.add(fromCalendar(cal))
            cal.add(Calendar.DAY_OF_MONTH, 1)
        }
        return result
    }

    /**
     * Returns all days of this month (1 to daysInMonth).
     */
    fun getMonthDays(): List<AtelierDate> {
        val total = daysInMonth()
        return (1..total).map { d -> AtelierDate(year, month, d) }
    }

    fun previousDay(): AtelierDate {
        val cal = toCalendar()
        cal.add(Calendar.DAY_OF_MONTH, -1)
        return fromCalendar(cal)
    }

    fun nextDay(): AtelierDate {
        val cal = toCalendar()
        cal.add(Calendar.DAY_OF_MONTH, 1)
        return fromCalendar(cal)
    }

    override operator fun compareTo(other: AtelierDate): Int {
        if (year != other.year) return year.compareTo(other.year)
        if (month != other.month) return month.compareTo(other.month)
        return day.compareTo(other.day)
    }

    companion object {
        fun fromCalendar(cal: Calendar): AtelierDate {
            return AtelierDate(
                year = cal.get(Calendar.YEAR),
                month = cal.get(Calendar.MONTH) + 1,
                day = cal.get(Calendar.DAY_OF_MONTH)
            )
        }

        fun todayTunis(): AtelierDate {
            val cal = Calendar.getInstance(TimeZone.getTimeZone("Africa/Tunis"), Locale.FRENCH)
            return fromCalendar(cal)
        }

        fun parseIso(iso: String): AtelierDate {
            val parts = iso.split("-").map { it.toInt() }
            return AtelierDate(parts[0], parts[1], parts[2])
        }

        fun daysInYearMonth(yearMonth: String): Int {
            val parts = yearMonth.split("-")
            val y = parts[0].toInt()
            val m = parts[1].toInt()
            val cal = Calendar.getInstance(TimeZone.getTimeZone("Africa/Tunis"), Locale.FRENCH)
            cal.set(Calendar.YEAR, y)
            cal.set(Calendar.MONTH, m - 1)
            cal.set(Calendar.DAY_OF_MONTH, 1)
            return cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        }
    }
}
