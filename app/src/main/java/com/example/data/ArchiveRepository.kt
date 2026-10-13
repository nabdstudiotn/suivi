package com.example.data

import android.util.Log
import com.example.data.local.AttendanceDao
import com.example.data.local.AttendanceRecordEntity
import com.example.data.local.HourlyRateDao
import com.example.data.local.MonthClosureDao
import com.example.data.local.MonthClosureEntity
import com.example.data.local.WorkerDao
import com.example.domain.calculation.AtelierCalculations
import com.example.domain.model.ArchiveMonthStatus
import com.example.domain.model.AttendanceEntry
import com.example.domain.model.AttendanceStatus
import com.example.domain.model.AuditActionType
import com.example.domain.model.AuditEvent
import com.example.domain.model.DailyAttendanceRecordDetail
import com.example.domain.model.HourlyRateRecord
import com.example.domain.model.MemberRole
import com.example.domain.model.MonthClosureRecord
import com.example.domain.model.MonthlyWorkerArchiveItem
import com.example.domain.model.Worker
import com.example.domain.model.WorkshopMonthlyArchiveSummary
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Stage 7: Centralized Archive Repository.
 * Derives monthly workshop and individual worker chronological archives strictly from confirmed Pointage data.
 * Manages month closure, certifications, revisions, and audits with Firestore cloud sync.
 * Guarantees that worker archives are never deleted even if the worker is stopped or archived.
 */
class ArchiveRepository(
    private val monthClosureDao: MonthClosureDao,
    private val workerDao: WorkerDao,
    private val attendanceDao: AttendanceDao,
    private val hourlyRateDao: HourlyRateDao,
    private val settingsRepository: SettingsRepository,
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    companion object {
        private const val TAG = "ArchiveRepository"
    }

    /**
     * Live stream of all month closures for a company.
     */
    fun getMonthClosures(companyId: String = "atelier_nabd_01"): Flow<List<MonthClosureRecord>> {
        return monthClosureDao.getAllClosures(companyId).map { entities ->
            entities.map { it.toDomain() }
        }.flowOn(Dispatchers.IO)
    }

    /**
     * Live stream of closure for a specific month.
     */
    fun getMonthClosure(companyId: String = "atelier_nabd_01", yearMonth: String): Flow<MonthClosureRecord?> {
        return monthClosureDao.getClosureForMonth(companyId, yearMonth).map { it?.toDomain() }.flowOn(Dispatchers.IO)
    }

    /**
     * Discovers all distinct months having confirmed pointage activity across all workers.
     * Always includes the current month so users can navigate and inspect in-progress work.
     */
    fun getAllAvailableMonths(): Flow<List<String>> {
        return attendanceDao.getAllAttendance().map { records ->
            val set = mutableSetOf<String>()
            val currentYearMonth = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"))
            set.add(currentYearMonth)

            for (r in records) {
                if (r.dateIso.length >= 7 && (r.status == AttendanceStatus.WORKED.name || (r.hours > 0 || r.minutes > 0) || r.status == AttendanceStatus.ABSENT.name)) {
                    set.add(r.dateIso.substring(0, 7))
                }
            }
            set.sortedDescending()
        }.flowOn(Dispatchers.IO)
    }

    /**
     * Builds the complete general workshop monthly archive summary for a given month.
     * Incorporates all workers (both active and historical/archived), confirmed hours, and closure status.
     */
    fun getMonthlyWorkshopArchive(
        companyId: String = "atelier_nabd_01",
        yearMonth: String
    ): Flow<WorkshopMonthlyArchiveSummary> {
        val workersFlow = workerDao.getAllWorkers().map { list -> list.map { it.toDomain() } }
        val attendanceFlow = attendanceDao.getAttendanceForMonth(yearMonth).map { list -> list.map { it.toDomain() } }
        val closureFlow = monthClosureDao.getClosureForMonth(companyId, yearMonth).map { it?.toDomain() }

        return combine(workersFlow, attendanceFlow, closureFlow) { workers, attendances, closure ->
            buildMonthlyArchiveSummary(yearMonth, workers, attendances, closure)
        }.flowOn(Dispatchers.IO)
    }

    /**
     * Builds the complete chronological archive for a specific worker across all worked months.
     * Preserved permanently regardless of active/inactive state in pointage.
     */
    fun getWorkerChronologicalArchive(
        workerId: String
    ): Flow<List<MonthlyWorkerArchiveItem>> {
        val workerFlow = workerDao.getWorkerByIdFlow(workerId).map { it?.toDomain() }
        val attendanceFlow = attendanceDao.getAttendanceForWorker(workerId).map { list -> list.map { it.toDomain() } }
        val ratesFlow = hourlyRateDao.getRatesForWorker(workerId).map { list -> list.map { it.toDomain() } }

        return combine(workerFlow, attendanceFlow, ratesFlow) { worker, attendances, rates ->
            if (worker == null) return@combine emptyList()

            val groupedByMonth = attendances.groupBy { it.dateIso.substring(0, 7) }
            val items = mutableListOf<MonthlyWorkerArchiveItem>()

            for ((yearMonth, monthEntries) in groupedByMonth.toSortedMap(Comparator.reverseOrder())) {
                val workedEntries = monthEntries.filter { it.status == AttendanceStatus.WORKED }
                val totalMinutes = AtelierCalculations.calculateTotalMinutes(workedEntries)

                // Include only months where the worker had confirmed presence, worked hours, or recorded absences
                if (totalMinutes > 0 || monthEntries.any { it.status == AttendanceStatus.ABSENT }) {
                    val basePay = AtelierCalculations.calculateGrossPayMillimes(
                        monthEntries,
                        rates,
                        worker.currentRateMillimes
                    )
                    val effectiveRate = rates
                        .filter { it.effectiveDateIso <= "$yearMonth-31" }
                        .maxByOrNull { it.effectiveDateIso }
                        ?.rateMillimes ?: worker.currentRateMillimes

                    val dailyRecords = monthEntries
                        .sortedBy { it.dateIso }
                        .map { entry ->
                            val dateObj = try { LocalDate.parse(entry.dateIso) } catch (e: Exception) { LocalDate.now() }
                            val dayName = dateObj.format(DateTimeFormatter.ofPattern("EEE dd MMM", Locale.FRENCH))
                            DailyAttendanceRecordDetail(
                                dateIso = entry.dateIso,
                                dayNameFrench = dayName.replaceFirstChar { it.uppercase() },
                                status = entry.status,
                                hoursWorked = entry.workedMinutes / 60,
                                minutesWorked = entry.workedMinutes % 60,
                                note = entry.note
                            )
                        }

                    items.add(
                        MonthlyWorkerArchiveItem(
                            workerId = worker.id,
                            workerFullName = worker.fullName,
                            workerRole = worker.role,
                            workerCategory = "Atelier",
                            isActive = worker.isActive,
                            yearMonth = yearMonth,
                            workedDaysCount = workedEntries.size,
                            absentDaysCount = monthEntries.count { it.status == AttendanceStatus.ABSENT },
                            restDaysCount = monthEntries.count { it.status == AttendanceStatus.REST_DAY },
                            totalMinutes = totalMinutes,
                            basePayMillimes = basePay,
                            appliedRateMillimes = effectiveRate,
                            dailyRecords = dailyRecords
                        )
                    )
                }
            }
            items
        }.flowOn(Dispatchers.IO)
    }

    /**
     * Clôture et certification officielle d'un mois d'archive.
     * Updates Room + Firestore and emits AuditEvent ARCHIVE_MONTH_CLOSED.
     */
    suspend fun closeMonth(
        companyId: String,
        yearMonth: String,
        actorName: String,
        actorRole: MemberRole,
        notes: String = ""
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val record = MonthClosureRecord(
                yearMonth = yearMonth,
                companyId = companyId,
                status = ArchiveMonthStatus.CLOTURE,
                closedBy = actorName,
                closedByRole = actorRole,
                closedAtEpochMs = System.currentTimeMillis(),
                revisionCount = 0,
                revisionReason = null,
                notes = notes.ifBlank { "Clôture certifiée de fin de mois d'atelier" }
            )

            // Save in Room
            monthClosureDao.insertOrUpdate(MonthClosureEntity.fromDomain(record))

            // Sync with Firestore
            try {
                val payload = mapOf(
                    "companyId" to companyId,
                    "yearMonth" to yearMonth,
                    "status" to ArchiveMonthStatus.CLOTURE.name,
                    "closedBy" to actorName,
                    "closedByRole" to actorRole.name,
                    "closedAtEpochMs" to record.closedAtEpochMs,
                    "revisionCount" to 0,
                    "notes" to record.notes,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                firestore.collection("companies").document(companyId)
                    .collection("month_closures").document(yearMonth)
                    .set(payload).await()
            } catch (e: Exception) {
                Log.w(TAG, "Firestore month closure warning: ${e.message}")
            }

            // Log AuditEvent
            settingsRepository.logAuditEvent(
                companyId = companyId,
                actionType = AuditActionType.ARCHIVE_MONTH_CLOSED,
                detailsFr = "Clôture officielle et certification du mois $yearMonth par $actorName ($actorRole). Note: ${record.notes}",
                targetEntityId = yearMonth,
                targetEntityName = "Archive $yearMonth",
                metadata = mapOf("yearMonth" to yearMonth, "status" to "CLOTURE")
            )

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "closeMonth failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Réouverture d'un mois clôturé pour révision / correction justifiée.
     * Updates Room + Firestore and emits AuditEvent ARCHIVE_MONTH_REOPENED.
     */
    suspend fun reopenForRevision(
        companyId: String,
        yearMonth: String,
        actorName: String,
        actorRole: MemberRole,
        reason: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val existing = monthClosureDao.getClosureForMonth(companyId, yearMonth).map { it?.toDomain() }
            val currentRev = 1 // default revision count

            val record = MonthClosureRecord(
                yearMonth = yearMonth,
                companyId = companyId,
                status = ArchiveMonthStatus.EN_REVISION,
                closedBy = actorName,
                closedByRole = actorRole,
                closedAtEpochMs = System.currentTimeMillis(),
                revisionCount = currentRev,
                revisionReason = reason.ifBlank { "Révision demandée pour rectification d'heures" },
                notes = "Mois déverrouillé pour révision"
            )

            monthClosureDao.insertOrUpdate(MonthClosureEntity.fromDomain(record))

            try {
                val payload = mapOf(
                    "companyId" to companyId,
                    "yearMonth" to yearMonth,
                    "status" to ArchiveMonthStatus.EN_REVISION.name,
                    "closedBy" to actorName,
                    "closedByRole" to actorRole.name,
                    "closedAtEpochMs" to record.closedAtEpochMs,
                    "revisionCount" to currentRev,
                    "revisionReason" to record.revisionReason,
                    "notes" to record.notes,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                firestore.collection("companies").document(companyId)
                    .collection("month_closures").document(yearMonth)
                    .set(payload).await()
            } catch (e: Exception) {
                Log.w(TAG, "Firestore month revision warning: ${e.message}")
            }

            // Log AuditEvent
            settingsRepository.logAuditEvent(
                companyId = companyId,
                actionType = AuditActionType.ARCHIVE_MONTH_REOPENED,
                detailsFr = "Réouverture du mois d'archive $yearMonth pour révision par $actorName ($actorRole). Motif: $reason",
                targetEntityId = yearMonth,
                targetEntityName = "Archive $yearMonth",
                metadata = mapOf("yearMonth" to yearMonth, "revisionReason" to reason)
            )

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "reopenForRevision failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Validation et re-clôture après révision.
     */
    suspend fun finalizeRevision(
        companyId: String,
        yearMonth: String,
        actorName: String,
        actorRole: MemberRole,
        notes: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val record = MonthClosureRecord(
                yearMonth = yearMonth,
                companyId = companyId,
                status = ArchiveMonthStatus.CLOTURE,
                closedBy = actorName,
                closedByRole = actorRole,
                closedAtEpochMs = System.currentTimeMillis(),
                revisionCount = 1,
                revisionReason = "Révision validée",
                notes = notes.ifBlank { "Re-clôture suite à révision approuvée" }
            )

            monthClosureDao.insertOrUpdate(MonthClosureEntity.fromDomain(record))

            try {
                val payload = mapOf(
                    "companyId" to companyId,
                    "yearMonth" to yearMonth,
                    "status" to ArchiveMonthStatus.CLOTURE.name,
                    "closedBy" to actorName,
                    "closedByRole" to actorRole.name,
                    "closedAtEpochMs" to record.closedAtEpochMs,
                    "revisionCount" to 1,
                    "revisionReason" to record.revisionReason,
                    "notes" to record.notes,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                firestore.collection("companies").document(companyId)
                    .collection("month_closures").document(yearMonth)
                    .set(payload).await()
            } catch (e: Exception) {
                Log.w(TAG, "Firestore finalize revision warning: ${e.message}")
            }

            settingsRepository.logAuditEvent(
                companyId = companyId,
                actionType = AuditActionType.ARCHIVE_MONTH_REVISED,
                detailsFr = "Validation de la révision et re-clôture du mois $yearMonth par $actorName ($actorRole). Notes: $notes",
                targetEntityId = yearMonth,
                targetEntityName = "Archive $yearMonth"
            )

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "finalizeRevision failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Returns the audit log trail specifically tied to archives or this month.
     */
    fun getAuditTrailForMonth(
        companyId: String = "atelier_nabd_01",
        yearMonth: String
    ): Flow<List<AuditEvent>> {
        return settingsRepository.getAuditEvents(companyId).map { list ->
            list.filter { event ->
                event.targetEntityId == yearMonth ||
                event.actionType == AuditActionType.ARCHIVE_MONTH_CLOSED ||
                event.actionType == AuditActionType.ARCHIVE_MONTH_REOPENED ||
                event.actionType == AuditActionType.ARCHIVE_MONTH_REVISED ||
                event.detailsFr.contains(yearMonth)
            }
        }.flowOn(Dispatchers.IO)
    }

    private fun buildMonthlyArchiveSummary(
        yearMonth: String,
        workers: List<Worker>,
        attendances: List<AttendanceEntry>,
        closure: MonthClosureRecord?
    ): WorkshopMonthlyArchiveSummary {
        val parts = yearMonth.split("-")
        val year = parts.getOrNull(0)?.toIntOrNull() ?: 2026
        val month = parts.getOrNull(1)?.toIntOrNull() ?: 5
        val monthName = when (month) {
            1 -> "Janvier"; 2 -> "Février"; 3 -> "Mars"; 4 -> "Avril"
            5 -> "Mai"; 6 -> "Juin"; 7 -> "Juillet"; 8 -> "Août"
            9 -> "Septembre"; 10 -> "Octobre"; 11 -> "Novembre"; 12 -> "Décembre"
            else -> "Mois"
        }
        val displayMonthFrench = "$monthName $year"

        val workerItems = mutableListOf<MonthlyWorkerArchiveItem>()
        var totalMinutes = 0
        var totalDays = 0
        var totalAbsences = 0
        var totalGross = 0L

        // Group attendance by workerId
        val byWorker = attendances.groupBy { it.workerId }

        for (worker in workers) {
            val entries = byWorker[worker.id] ?: emptyList()
            val workedEntries = entries.filter { it.status == AttendanceStatus.WORKED }
            val minutes = AtelierCalculations.calculateTotalMinutes(workedEntries)

            if (minutes > 0 || entries.any { it.status == AttendanceStatus.ABSENT }) {
                val absentCount = entries.count { it.status == AttendanceStatus.ABSENT }
                val restCount = entries.count { it.status == AttendanceStatus.REST_DAY }
                val basePay = (minutes * worker.currentRateMillimes) / 60

                totalMinutes += minutes
                totalDays += workedEntries.size
                totalAbsences += absentCount
                totalGross += basePay

                val dailyRecords = entries.sortedBy { it.dateIso }.map { entry ->
                    val dateObj = try { LocalDate.parse(entry.dateIso) } catch (e: Exception) { LocalDate.now() }
                    val dayName = dateObj.format(DateTimeFormatter.ofPattern("EEE dd MMM", Locale.FRENCH))
                    DailyAttendanceRecordDetail(
                        dateIso = entry.dateIso,
                        dayNameFrench = dayName.replaceFirstChar { it.uppercase() },
                        status = entry.status,
                        hoursWorked = entry.workedMinutes / 60,
                        minutesWorked = entry.workedMinutes % 60,
                        note = entry.note
                    )
                }

                workerItems.add(
                    MonthlyWorkerArchiveItem(
                        workerId = worker.id,
                        workerFullName = worker.fullName,
                        workerRole = worker.role,
                        workerCategory = "Atelier",
                        isActive = worker.isActive,
                        yearMonth = yearMonth,
                        workedDaysCount = workedEntries.size,
                        absentDaysCount = absentCount,
                        restDaysCount = restCount,
                        totalMinutes = minutes,
                        basePayMillimes = basePay,
                        appliedRateMillimes = worker.currentRateMillimes,
                        dailyRecords = dailyRecords
                    )
                )
            }
        }

        return WorkshopMonthlyArchiveSummary(
            yearMonth = yearMonth,
            displayNameFrench = displayMonthFrench,
            status = closure?.status ?: ArchiveMonthStatus.OUVERT,
            closure = closure,
            totalWorkersActive = workerItems.size,
            totalMinutesWorked = totalMinutes,
            totalDaysWorked = totalDays,
            totalAbsences = totalAbsences,
            totalGrossPayrollMillimes = totalGross,
            workers = workerItems.sortedBy { it.workerFullName }
        )
    }
}
