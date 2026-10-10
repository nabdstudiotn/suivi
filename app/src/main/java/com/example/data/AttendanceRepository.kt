package com.example.data

import com.example.data.local.AdvanceEntity
import com.example.data.local.AnnouncementEntity
import com.example.data.local.AttendanceRecordEntity
import com.example.data.local.HourlyRateHistoryEntity
import com.example.data.local.PaymentEntity
import com.example.data.local.PayrollAdjustmentEntity
import com.example.data.local.SuiviDatabase
import com.example.data.local.WorkerEntity
import com.example.domain.model.AdjustmentType
import com.example.domain.model.AdvanceRecord
import com.example.domain.model.Announcement
import com.example.domain.model.AtelierDate
import com.example.domain.model.AttendanceEntry
import com.example.domain.model.AttendanceStatus
import com.example.domain.model.HourlyRateRecord
import com.example.domain.model.PaymentRecord
import com.example.domain.model.PayrollAdjustment
import com.example.domain.model.Worker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * Contract for attendance, worker management, archives, rate history, payroll, and communication.
 * Per 04_real_only_policy.md and Approved Corrections:
 * All data comes exclusively from durable Room SQLite storage or explicitly user-entered operations.
 */
interface AttendanceRepository {
    fun getActiveWorkers(): Flow<List<Worker>>
    fun getAllWorkers(): Flow<List<Worker>>
    fun getWorkerById(workerId: String): Flow<Worker?>
    fun getAttendanceForDate(dateIso: String): Flow<Map<String, AttendanceEntry>>
    fun getAttendanceForWorker(workerId: String): Flow<List<AttendanceEntry>>
    fun getAttendanceForMonth(monthPrefix: String): Flow<List<AttendanceEntry>>
    fun getRatesForWorker(workerId: String): Flow<List<HourlyRateRecord>>

    suspend fun saveAttendanceEntry(entry: AttendanceEntry)
    suspend fun resetAttendanceEntry(workerId: String, dateIso: String)
    suspend fun markDayAsRestDay(dateIso: String)

    suspend fun addWorker(
        firstName: String,
        lastName: String,
        role: String,
        hourlyRateMillimes: Long,
        hireDateIso: String
    ): Worker
    suspend fun updateWorker(worker: Worker)
    suspend fun setWorkerActiveStatus(workerId: String, isActive: Boolean, dateIso: String)
    suspend fun updateHourlyRate(workerId: String, newRateMillimes: Long, effectiveDateIso: String, motif: String)

    // Advances & Payroll
    fun getAdvancesForMonth(monthPrefix: String): Flow<List<AdvanceRecord>>
    fun getAdvancesForWorker(workerId: String): Flow<List<AdvanceRecord>>
    suspend fun recordAdvance(workerId: String, dateIso: String, amountMillimes: Long, motif: String, author: String)

    fun getAdjustmentsForMonth(yearMonth: String): Flow<List<PayrollAdjustment>>
    suspend fun recordAdjustment(
        type: AdjustmentType,
        workerId: String,
        yearMonth: String,
        dateIso: String,
        amountMillimes: Long,
        motif: String,
        note: String?,
        author: String
    )

    fun getPaymentsForMonth(yearMonth: String): Flow<List<PaymentRecord>>
    suspend fun recordPayment(
        workerId: String,
        yearMonth: String,
        dateIso: String,
        amountMillimes: Long,
        method: String,
        payer: String,
        note: String?
    )

    // Communication (Annonces)
    fun getAnnouncements(): Flow<List<Announcement>>
    suspend fun createAnnouncement(
        title: String,
        content: String,
        audience: String,
        expirationDateIso: String,
        author: String
    )
    suspend fun deleteAnnouncement(id: String)
}

/**
 * Room-backed persistent repository implementing local SQLite storage.
 * Empty state on initial launch. No invented workers or fake records.
 */
class RoomAttendanceRepository(
    private val database: SuiviDatabase
) : AttendanceRepository {

    private val workerDao = database.workerDao()
    private val hourlyRateDao = database.hourlyRateDao()
    private val attendanceDao = database.attendanceDao()
    private val advanceDao = database.advanceDao()
    private val adjustmentDao = database.payrollAdjustmentDao()
    private val paymentDao = database.paymentDao()
    private val announcementDao = database.announcementDao()

    override fun getActiveWorkers(): Flow<List<Worker>> {
        return workerDao.getActiveWorkers().map { list -> list.map { it.toDomain() } }
    }

    override fun getAllWorkers(): Flow<List<Worker>> {
        return workerDao.getAllWorkers().map { list -> list.map { it.toDomain() } }
    }

    override fun getWorkerById(workerId: String): Flow<Worker?> {
        return workerDao.getWorkerById(workerId).map { it?.toDomain() }
    }

    override fun getAttendanceForDate(dateIso: String): Flow<Map<String, AttendanceEntry>> {
        return attendanceDao.getAttendanceForDate(dateIso).map { list ->
            list.associate { entity ->
                entity.workerId to entity.toDomain()
            }
        }
    }

    override fun getAttendanceForWorker(workerId: String): Flow<List<AttendanceEntry>> {
        return attendanceDao.getAttendanceForWorker(workerId).map { list -> list.map { it.toDomain() } }
    }

    override fun getAttendanceForMonth(monthPrefix: String): Flow<List<AttendanceEntry>> {
        return attendanceDao.getAttendanceForMonth(monthPrefix).map { list -> list.map { it.toDomain() } }
    }

    override fun getRatesForWorker(workerId: String): Flow<List<HourlyRateRecord>> {
        return hourlyRateDao.getRatesForWorker(workerId).map { list -> list.map { it.toDomain() } }
    }

    override suspend fun saveAttendanceEntry(entry: AttendanceEntry) = withContext(Dispatchers.IO) {
        attendanceDao.insertRecord(AttendanceRecordEntity.fromDomain(entry))
    }

    override suspend fun resetAttendanceEntry(workerId: String, dateIso: String) = withContext(Dispatchers.IO) {
        val blankEntry = AttendanceEntry(
            workerId = workerId,
            dateIso = dateIso,
            status = AttendanceStatus.BLANK
        )
        attendanceDao.insertRecord(AttendanceRecordEntity.fromDomain(blankEntry))
    }

    override suspend fun markDayAsRestDay(dateIso: String) = withContext(Dispatchers.IO) {
        val workers = workerDao.getActiveWorkers().first()
        val restRecords = workers.map { w ->
            AttendanceRecordEntity(
                id = "${w.id}_$dateIso",
                workerId = w.id,
                dateIso = dateIso,
                status = AttendanceStatus.REST_DAY.name,
                hours = 0,
                minutes = 0
            )
        }
        attendanceDao.insertRecords(restRecords)
    }

    override suspend fun addWorker(
        firstName: String,
        lastName: String,
        role: String,
        hourlyRateMillimes: Long,
        hireDateIso: String
    ): Worker = withContext(Dispatchers.IO) {
        val initials = buildString {
            if (firstName.isNotBlank()) append(firstName.trim().first().uppercaseChar())
            if (lastName.isNotBlank()) append(lastName.trim().first().uppercaseChar())
        }.ifEmpty { "OU" }

        val workerId = "w_" + UUID.randomUUID().toString().take(8)

        val entity = WorkerEntity(
            id = workerId,
            firstName = firstName.trim(),
            lastName = lastName.trim(),
            initials = initials,
            currentRateMillimes = hourlyRateMillimes,
            role = role.trim(),
            isActive = true,
            hireDateIso = hireDateIso,
            returnDateIso = null,
            archivedDateIso = null
        )
        workerDao.insertWorker(entity)

        hourlyRateDao.insertRate(
            HourlyRateHistoryEntity(
                workerId = workerId,
                effectiveDateIso = hireDateIso,
                rateMillimes = hourlyRateMillimes,
                motif = "Taux initial d'embauche"
            )
        )

        entity.toDomain()
    }

    override suspend fun updateWorker(worker: Worker) = withContext(Dispatchers.IO) {
        workerDao.updateWorker(WorkerEntity.fromDomain(worker))
    }

    override suspend fun setWorkerActiveStatus(
        workerId: String,
        isActive: Boolean,
        dateIso: String
    ) = withContext(Dispatchers.IO) {
        val current = workerDao.getWorkerById(workerId).first() ?: return@withContext
        val updated = if (isActive) {
            current.copy(isActive = true, returnDateIso = dateIso)
        } else {
            current.copy(isActive = false, archivedDateIso = dateIso)
        }
        workerDao.updateWorker(updated)
    }

    override suspend fun updateHourlyRate(
        workerId: String,
        newRateMillimes: Long,
        effectiveDateIso: String,
        motif: String
    ) = withContext(Dispatchers.IO) {
        hourlyRateDao.insertRate(
            HourlyRateHistoryEntity(
                workerId = workerId,
                effectiveDateIso = effectiveDateIso,
                rateMillimes = newRateMillimes,
                motif = motif
            )
        )
        val worker = workerDao.getWorkerById(workerId).first()
        if (worker != null) {
            workerDao.updateWorker(worker.copy(currentRateMillimes = newRateMillimes))
        }
    }

    // Advances & Payroll
    override fun getAdvancesForMonth(monthPrefix: String): Flow<List<AdvanceRecord>> {
        return advanceDao.getAdvancesForMonth(monthPrefix).map { list -> list.map { it.toDomain() } }
    }

    override fun getAdvancesForWorker(workerId: String): Flow<List<AdvanceRecord>> {
        return advanceDao.getAdvancesForWorker(workerId).map { list -> list.map { it.toDomain() } }
    }

    override suspend fun recordAdvance(
        workerId: String,
        dateIso: String,
        amountMillimes: Long,
        motif: String,
        author: String
    ) = withContext(Dispatchers.IO) {
        advanceDao.insertAdvance(
            AdvanceEntity(
                id = "adv_" + UUID.randomUUID().toString().take(8),
                workerId = workerId,
                dateIso = dateIso,
                amountMillimes = amountMillimes,
                motif = motif,
                author = author
            )
        )
    }

    override fun getAdjustmentsForMonth(yearMonth: String): Flow<List<PayrollAdjustment>> {
        return adjustmentDao.getAdjustmentsForMonth(yearMonth).map { list -> list.map { it.toDomain() } }
    }

    override suspend fun recordAdjustment(
        type: AdjustmentType,
        workerId: String,
        yearMonth: String,
        dateIso: String,
        amountMillimes: Long,
        motif: String,
        note: String?,
        author: String
    ) = withContext(Dispatchers.IO) {
        adjustmentDao.insertAdjustment(
            PayrollAdjustmentEntity(
                id = "adj_" + UUID.randomUUID().toString().take(8),
                type = type.name,
                workerId = workerId,
                yearMonth = yearMonth,
                dateIso = dateIso,
                amountMillimes = amountMillimes,
                motif = motif,
                note = note,
                author = author
            )
        )
    }

    override fun getPaymentsForMonth(yearMonth: String): Flow<List<PaymentRecord>> {
        return paymentDao.getPaymentsForMonth(yearMonth).map { list -> list.map { it.toDomain() } }
    }

    override suspend fun recordPayment(
        workerId: String,
        yearMonth: String,
        dateIso: String,
        amountMillimes: Long,
        method: String,
        payer: String,
        note: String?
    ) = withContext(Dispatchers.IO) {
        paymentDao.insertPayment(
            PaymentEntity(
                id = "pay_" + UUID.randomUUID().toString().take(8),
                workerId = workerId,
                yearMonth = yearMonth,
                dateIso = dateIso,
                amountMillimes = amountMillimes,
                paymentMethod = method,
                payerName = payer,
                note = note
            )
        )
    }

    // Communication (Annonces)
    override fun getAnnouncements(): Flow<List<Announcement>> {
        return announcementDao.getAllAnnouncements().map { list -> list.map { it.toDomain() } }
    }

    override suspend fun createAnnouncement(
        title: String,
        content: String,
        audience: String,
        expirationDateIso: String,
        author: String
    ) = withContext(Dispatchers.IO) {
        announcementDao.insertAnnouncement(
            AnnouncementEntity(
                id = "ann_" + UUID.randomUUID().toString().take(8),
                title = title,
                content = content,
                targetAudience = audience,
                expirationDateIso = expirationDateIso,
                author = author,
                createdAtIso = AtelierDate.todayTunis().isoString
            )
        )
    }

    override suspend fun deleteAnnouncement(id: String) = withContext(Dispatchers.IO) {
        announcementDao.deleteAnnouncement(id)
    }
}
