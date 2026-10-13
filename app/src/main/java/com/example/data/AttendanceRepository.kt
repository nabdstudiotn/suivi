package com.example.data

import com.example.domain.model.AdjustmentType
import com.example.domain.model.AdvanceRecord
import com.example.domain.model.Announcement
import com.example.domain.model.AttendanceEntry
import com.example.domain.model.HourlyRateRecord
import com.example.domain.model.PaymentRecord
import com.example.domain.model.PayrollAdjustment
import com.example.domain.model.Worker
import kotlinx.coroutines.flow.Flow

/**
 * Common data contract for workshop attendance, team roster and payroll persistence.
 * Implemented by both Room local persistence and Firestore cloud repository.
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
    suspend fun updateHourlyRate(
        workerId: String,
        newRateMillimes: Long,
        effectiveDateIso: String,
        motif: String
    )
    fun getAdvancesForMonth(monthPrefix: String): Flow<List<AdvanceRecord>>
    fun getAdvancesForWorker(workerId: String): Flow<List<AdvanceRecord>>
    suspend fun recordAdvance(
        workerId: String,
        dateIso: String,
        amountMillimes: Long,
        motif: String,
        author: String
    )
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
