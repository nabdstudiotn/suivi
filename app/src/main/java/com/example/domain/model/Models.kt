package com.example.domain.model

import java.text.Normalizer
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.TemporalAdjusters
import java.util.Locale

/**
 * Technical worker domain model.
 * ID is technical, permanent and immutable across name changes.
 */
data class Worker(
    val id: String,
    val firstName: String,
    val lastName: String,
    val initials: String = "${firstName.take(1)}${lastName.take(1)}".uppercase(),
    val currentRateMillimes: Long = 3500L,
    val role: String = "Ouvrière",
    val isActive: Boolean = true,
    val hireDateIso: String = "2026-01-01",
    val returnDateIso: String? = null,
    val archivedDateIso: String? = null,
    val cin: String? = null,
    val phone: String? = null,
    val notes: String = ""
) {
    val fullName: String
        get() = "$firstName $lastName".trim()

    val rateFormattedTnd: String
        get() = formatMillimesAsTnd(currentRateMillimes)

    val profileStatus: WorkerProfileStatus
        get() = if (isActive) WorkerProfileStatus.ACTIVE else WorkerProfileStatus.ARCHIVED

    fun isEmployedOn(dateIso: String): Boolean {
        if (dateIso < hireDateIso) return false
        if (!isActive && archivedDateIso != null) {
            if (dateIso > archivedDateIso) {
                if (returnDateIso == null || dateIso < returnDateIso) return false
            }
        }
        return true
    }
}

/**
 * Explicit profile lifecycle statuses.
 */
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
    val role: String = "",
    val rateInput: String = "",
    val cin: String = "",
    val phone: String = "",
    val hireDateIso: String = "",
    val notes: String = ""
) {
    val isNotEmpty: Boolean
        get() = firstName.isNotBlank() || lastName.isNotBlank() || role.isNotBlank() || rateInput.isNotBlank() || notes.isNotBlank()
}

/**
 * Top horizontal section navigation map for Suivi.
 */
enum class AtelierSection(val titleFrench: String, val subtitleFr: String) {
    POINTAGE("Pointage", "Présences"),
    EQUIPE("Équipe", "Équipe d'atelier"),
    PAIE("Paie", "Paie & règlements"),
    COMMUNICATION("Communication", "Échanges d'atelier"),
    ARCHIVES("Archives", "Archives d'atelier"),
    PARAMETRES("Paramètres", "Configuration")
}

/**
 * Calendar date handling in Africa/Tunis zone.
 */
data class AtelierDate(
    val year: Int,
    val month: Int,
    val day: Int
) : Comparable<AtelierDate> {
    val isoString: String
        get() = String.format(Locale.US, "%04d-%02d-%02d", year, month, day)

    val yearMonthIso: String
        get() = String.format(Locale.US, "%04d-%02d", year, month)

    private val localDate: LocalDate
        get() = LocalDate.of(year, month, day)

    val isSunday: Boolean
        get() = localDate.dayOfWeek == DayOfWeek.SUNDAY

    val dayOfWeekNameFrench: String
        get() = when (localDate.dayOfWeek) {
            DayOfWeek.MONDAY -> "Lundi"
            DayOfWeek.TUESDAY -> "Mardi"
            DayOfWeek.WEDNESDAY -> "Mercredi"
            DayOfWeek.THURSDAY -> "Jeudi"
            DayOfWeek.FRIDAY -> "Vendredi"
            DayOfWeek.SATURDAY -> "Samedi"
            DayOfWeek.SUNDAY -> "Dimanche"
        }

    val displayDateFrench: String
        get() = "$dayOfWeekNameFrench $day ${frenchMonthName(month)} $year"

    val formattedFrench: String
        get() = displayDateFrench

    val shortDayOfWeekNameFrench: String
        get() = when (localDate.dayOfWeek) {
            DayOfWeek.MONDAY -> "Lun"
            DayOfWeek.TUESDAY -> "Mar"
            DayOfWeek.WEDNESDAY -> "Mer"
            DayOfWeek.THURSDAY -> "Jeu"
            DayOfWeek.FRIDAY -> "Ven"
            DayOfWeek.SATURDAY -> "Sam"
            DayOfWeek.SUNDAY -> "Dim"
        }

    fun previousDay(): AtelierDate {
        val prev = localDate.minusDays(1)
        return AtelierDate(prev.year, prev.monthValue, prev.dayOfMonth)
    }

    fun nextDay(): AtelierDate {
        val next = localDate.plusDays(1)
        return AtelierDate(next.year, next.monthValue, next.dayOfMonth)
    }

    fun getWeekDays(): List<AtelierDate> {
        val monday = localDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        return (0..6).map { offset ->
            val d = monday.plusDays(offset.toLong())
            AtelierDate(d.year, d.monthValue, d.dayOfMonth)
        }
    }

    fun getMonthDays(): List<AtelierDate> {
        val length = localDate.lengthOfMonth()
        return (1..length).map { d ->
            AtelierDate(year, month, d)
        }
    }

    override fun compareTo(other: AtelierDate): Int {
        return this.localDate.compareTo(other.localDate)
    }

    companion object {
        fun todayTunis(): AtelierDate {
            val now = ZonedDateTime.now(ZoneId.of("Africa/Tunis"))
            return AtelierDate(now.year, now.monthValue, now.dayOfMonth)
        }

        fun parse(iso: String): AtelierDate {
            val parts = iso.split("-")
            return AtelierDate(parts[0].toInt(), parts[1].toInt(), parts[2].toInt())
        }

        fun daysInYearMonth(yearMonth: String): Int {
            val parts = yearMonth.split("-")
            val year = parts[0].toInt()
            val month = parts[1].toInt()
            return YearMonth.of(year, month).lengthOfMonth()
        }

        fun frenchMonthName(month: Int): String = when (month) {
            1 -> "Janvier"; 2 -> "Février"; 3 -> "Mars"; 4 -> "Avril"
            5 -> "Mai"; 6 -> "Juin"; 7 -> "Juillet"; 8 -> "Août"
            9 -> "Septembre"; 10 -> "Octobre"; 11 -> "Novembre"; 12 -> "Décembre"
            else -> ""
        }
    }
}

/**
 * Attendance status for a given calendar day.
 */
enum class AttendanceStatus(val labelFrench: String) {
    BLANK("À renseigner"),
    WORKED("Heures faites"),
    ABSENT("AB"),
    REST_DAY("Jour de repos")
}

enum class AttendanceViewMode(val titleFrench: String) {
    JOUR("Jour"),
    SEMAINE("Semaine"),
    MOIS("Mois")
}

/**
 * Daily attendance record for an operator.
 */
data class AttendanceEntry(
    val workerId: String,
    val dateIso: String,
    val status: AttendanceStatus = AttendanceStatus.BLANK,
    val hours: Int = 0,
    val minutes: Int = 0,
    val note: String? = null
) {
    val workedMinutes: Int
        get() = if (status == AttendanceStatus.WORKED) (hours * 60) + minutes else 0

    val totalMinutes: Int
        get() = workedMinutes

    val durationFormatted: String
        get() = when (status) {
            AttendanceStatus.BLANK -> "À renseigner"
            AttendanceStatus.ABSENT -> "AB"
            AttendanceStatus.REST_DAY -> "Jour de repos"
            AttendanceStatus.WORKED -> {
                if (minutes == 0) "$hours h" else "$hours h ${minutes.toString().padStart(2, '0')}"
            }
        }
}

/**
 * Historical or current hourly rate of a worker in millimes (TND * 1000).
 */
data class HourlyRateRecord(
    val id: Long = 0L,
    val workerId: String,
    val effectiveDateIso: String,
    val rateMillimes: Long,
    val motif: String = "Initial"
) {
    val rateFormattedTnd: String
        get() = formatMillimesAsTnd(rateMillimes)
}

data class AdvanceRecord(
    val id: String,
    val workerId: String,
    val dateIso: String,
    val amountMillimes: Long,
    val motif: String,
    val author: String = "Gérante"
) {
    val amountFormattedTnd: String
        get() = formatMillimesAsTnd(amountMillimes)
}

enum class AdjustmentType(val labelFrench: String) {
    PRIME("Prime de rendement"),
    RETENUE("Retenue sur salaire"),
    PRIME_AID("Prime de l'Aïd")
}

data class PayrollAdjustment(
    val id: String,
    val type: AdjustmentType,
    val workerId: String,
    val yearMonth: String,
    val dateIso: String,
    val amountMillimes: Long,
    val motif: String,
    val note: String? = null,
    val author: String = "Gérante"
) {
    val amountFormattedTnd: String
        get() = formatMillimesAsTnd(amountMillimes)
}

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

data class Announcement(
    val id: String,
    val title: String,
    val content: String,
    val targetAudience: String = "Tout l'atelier",
    val expirationDateIso: String = "",
    val author: String = "Gérante",
    val createdAtIso: String = ""
)

data class WorkedMonthSummary(
    val yearMonth: String,
    val displayNameFrench: String,
    val workedDaysCount: Int,
    val absentDaysCount: Int,
    val totalMinutes: Int,
    val basePayMillimes: Long,
    val advancesMillimes: Long,
    val adjustmentsMillimes: Long,
    val totalDueMillimes: Long,
    val paidAmountMillimes: Long,
    val remainingDueMillimes: Long
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
        get() = basePayFormattedTnd

    val totalDueFormattedTnd: String
        get() = formatMillimesAsTnd(totalDueMillimes)

    val paidAmountFormattedTnd: String
        get() = formatMillimesAsTnd(paidAmountMillimes)

    val remainingDueFormattedTnd: String
        get() = formatMillimesAsTnd(remainingDueMillimes)
}

/**
 * Exact integer formatting of millimes into Tunisian Dinars (e.g. 7000L -> "7.000 TND").
 */
fun formatMillimesAsTnd(millimes: Long): String {
    val sign = if (millimes < 0) "-" else ""
    val abs = kotlin.math.abs(millimes)
    val dinars = abs / 1000
    val remainder = abs % 1000
    return String.format(Locale.US, "%s%d.%03d TND", sign, dinars, remainder)
}

/**
 * Clean French search string by removing diacritics, extra spaces and lowering case.
 */
fun normalizeForFrenchSearch(input: String): String {
    val trimmed = input.trim().replace("\\s+".toRegex(), " ")
    val normalized = Normalizer.normalize(trimmed, Normalizer.Form.NFD)
    return normalized.replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "").lowercase()
}
