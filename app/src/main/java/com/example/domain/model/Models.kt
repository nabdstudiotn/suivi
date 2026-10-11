package com.example.domain.model

import java.time.LocalDate

/**
 * Technical worker domain model.
 * ID is technical, permanent and immutable across name changes.
 */
data class Worker(
  val id: String,
  val firstName: String,
  val lastName: String,
  val activeFrom: LocalDate,
  val activeTo: LocalDate? = null,
  val currentHourlyRate: Double = 0.0,
  val status: WorkerProfileStatus = WorkerProfileStatus.ACTIVE,
  val observations: String = ""
) {
  val fullName: String
    get() = if (firstName.isBlank() && lastName.isBlank()) ""
    else if (firstName.isBlank()) lastName
    else if (lastName.isBlank()) firstName
    else "$firstName $lastName"
}

/**
 * Explicit profile lifecycle statuses.
 */
enum class WorkerProfileStatus(val labelFr: String) {
  ACTIVE("Active"),
  ARCHIVED("Archivée"),
  PENDING_SAVE("Brouillon"),
  CONFLICT("Conflit à vérifier"),
  READ_ONLY("Lecture seule")
}

/**
 * Attendance status for a given calendar day.
 */
enum class AttendanceStatus {
  PRESENT,
  ABSENT,
  REST,
  HOLIDAY,
  UNRECORDED
}

/**
 * Daily attendance record.
 */
data class DailyAttendance(
  val workerId: String,
  val date: LocalDate,
  val hoursWorked: Double = 0.0,
  val status: AttendanceStatus = AttendanceStatus.UNRECORDED,
  val note: String = ""
)

/**
 * Clean French search string by removing diacritics, extra spaces and lowering case.
 */
fun normalizeForFrenchSearch(input: String): String {
  val trimmed = input.trim().replace("\\s+".toRegex(), " ")
  val normalized = java.text.Normalizer.normalize(trimmed, java.text.Normalizer.Form.NFD)
  return normalized.replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "").lowercase()
}
