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
import com.example.domain.model.AttendanceEntry
import com.example.domain.model.HourlyRateRecord
import com.example.domain.model.PaymentRecord
import com.example.domain.model.PayrollAdjustment
import com.example.domain.model.Worker
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID

class RoomAttendanceRepository(
    private val database: SuiviDatabase
) : AttendanceRepository {

    private val workerDao = database.workerDao()
    private val attendanceDao = database.attendanceDao()
    private val hourlyRateDao = database.hourlyRateDao()
    private val advanceDao = database.advanceDao()
    private val payrollAdjustmentDao = database.payrollAdjustmentDao()
    private val paymentDao = database.paymentDao()
    private val announcementDao = database.announcementDao()

    override fun getActiveWorkers(): Flow<List<Worker>> {
        return workerDao.getActiveWorkers().map { list -> list.map { it.toDomain() } }
    }

    override fun getAllWorkers(): Flow<List<Worker>> {
        return workerDao.getAllWorkers().map { list -> list.map { it.toDomain() } }
    }

    override fun getWorkerById(workerId: String): Flow<Worker?> {
        return workerDao.getWorkerByIdFlow(workerId).map { it?.toDomain() }
    }

    override fun getAttendanceForDate(dateIso: String): Flow<Map<String, AttendanceEntry>> {
        return attendanceDao.getAttendanceForDate(dateIso).map { list ->
            list.map { it.toDomain() }.associateBy { it.workerId }
        }
    }

    override fun getAttendanceForWorker(workerId: String): Flow<List<AttendanceEntry>> {
        return attendanceDao.getAttendanceForWorker(workerId).map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun getAttendanceForMonth(monthPrefix: String): Flow<List<AttendanceEntry>> {
        return attendanceDao.getAttendanceForMonth(monthPrefix).map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun getRatesForWorker(workerId: String): Flow<List<HourlyRateRecord>> {
        return hourlyRateDao.getRatesForWorker(workerId).map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun saveAttendanceEntry(entry: AttendanceEntry) {
        attendanceDao.insertOrUpdate(AttendanceRecordEntity.fromDomain(entry))
    }

    override suspend fun resetAttendanceEntry(workerId: String, dateIso: String) {
        val compositeId = "${workerId}_${dateIso}"
        attendanceDao.deleteAttendance(compositeId)
    }

    override suspend fun markDayAsRestDay(dateIso: String) {
        val allWorkers = workerDao.getAllWorkers().first()
        for (w in allWorkers) {
            val entry = AttendanceEntry(
                workerId = w.id,
                dateIso = dateIso,
                status = com.example.domain.model.AttendanceStatus.REST_DAY
            )
            attendanceDao.insertOrUpdate(AttendanceRecordEntity.fromDomain(entry))
        }
    }

    override suspend fun addWorker(
        firstName: String,
        lastName: String,
        role: String,
        hourlyRateMillimes: Long,
        hireDateIso: String
    ): Worker {
        val id = UUID.randomUUID().toString()
        val initials = "${firstName.take(1)}${lastName.take(1)}".uppercase()
        val worker = Worker(
            id = id,
            firstName = firstName,
            lastName = lastName,
            initials = initials,
            currentRateMillimes = hourlyRateMillimes,
            role = role,
            isActive = true,
            hireDateIso = hireDateIso
        )
        workerDao.insertOrUpdate(WorkerEntity.fromDomain(worker))
        hourlyRateDao.insertRate(
            HourlyRateHistoryEntity(
                workerId = id,
                effectiveDateIso = hireDateIso,
                rateMillimes = hourlyRateMillimes,
                motif = "Embauche"
            )
        )
        return worker
    }

    override suspend fun updateWorker(worker: Worker) {
        workerDao.insertOrUpdate(WorkerEntity.fromDomain(worker))
    }

    override suspend fun setWorkerActiveStatus(workerId: String, isActive: Boolean, dateIso: String) {
        val current = workerDao.getWorkerById(workerId) ?: return
        val updated = if (isActive) {
            current.copy(isActive = true, returnDateIso = dateIso)
        } else {
            current.copy(isActive = false, archivedDateIso = dateIso)
        }
        workerDao.insertOrUpdate(updated)
    }

    override suspend fun updateHourlyRate(
        workerId: String,
        newRateMillimes: Long,
        effectiveDateIso: String,
        motif: String
    ) {
        val worker = workerDao.getWorkerById(workerId)
        if (worker != null) {
            workerDao.insertOrUpdate(worker.copy(currentRateMillimes = newRateMillimes))
        }
        hourlyRateDao.insertRate(
            HourlyRateHistoryEntity(
                workerId = workerId,
                effectiveDateIso = effectiveDateIso,
                rateMillimes = newRateMillimes,
                motif = motif
            )
        )
    }

    override fun getAdvancesForMonth(monthPrefix: String): Flow<List<AdvanceRecord>> {
        return advanceDao.getAdvancesForMonth(monthPrefix).map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun getAdvancesForWorker(workerId: String): Flow<List<AdvanceRecord>> {
        return advanceDao.getAdvancesForWorker(workerId).map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun recordAdvance(
        workerId: String,
        dateIso: String,
        amountMillimes: Long,
        motif: String,
        author: String
    ) {
        val id = UUID.randomUUID().toString()
        advanceDao.insertAdvance(
            AdvanceEntity(
                id = id,
                workerId = workerId,
                dateIso = dateIso,
                amountMillimes = amountMillimes,
                motif = motif,
                author = author
            )
        )
    }

    override fun getAdjustmentsForMonth(yearMonth: String): Flow<List<PayrollAdjustment>> {
        return payrollAdjustmentDao.getAdjustmentsForMonth(yearMonth).map { list ->
            list.map { it.toDomain() }
        }
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
    ) {
        val id = UUID.randomUUID().toString()
        payrollAdjustmentDao.insertAdjustment(
            PayrollAdjustmentEntity(
                id = id,
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
        return paymentDao.getPaymentsForMonth(yearMonth).map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun recordPayment(
        workerId: String,
        yearMonth: String,
        dateIso: String,
        amountMillimes: Long,
        method: String,
        payer: String,
        note: String?
    ) {
        val id = UUID.randomUUID().toString()
        paymentDao.insertPayment(
            PaymentEntity(
                id = id,
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

    override fun getAnnouncements(): Flow<List<Announcement>> {
        return announcementDao.getAnnouncements().map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun createAnnouncement(
        title: String,
        content: String,
        audience: String,
        expirationDateIso: String,
        author: String
    ) {
        val id = UUID.randomUUID().toString()
        announcementDao.insertAnnouncement(
            AnnouncementEntity(
                id = id,
                title = title,
                content = content,
                targetAudience = audience,
                expirationDateIso = expirationDateIso,
                author = author,
                createdAtIso = java.time.Instant.now().toString()
            )
        )
    }

    override suspend fun deleteAnnouncement(id: String) {
        announcementDao.deleteAnnouncement(id)
    }
}
