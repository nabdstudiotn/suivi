package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkerDao {
    @Query("SELECT * FROM workers ORDER BY lastName ASC, firstName ASC")
    fun getAllWorkers(): Flow<List<WorkerEntity>>

    @Query("SELECT * FROM workers WHERE isActive = 1 ORDER BY lastName ASC, firstName ASC")
    fun getActiveWorkers(): Flow<List<WorkerEntity>>

    @Query("SELECT * FROM workers WHERE id = :id")
    fun getWorkerById(id: String): Flow<WorkerEntity?>

    @Query("SELECT COUNT(*) FROM workers")
    suspend fun countWorkers(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorker(worker: WorkerEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkers(workers: List<WorkerEntity>)

    @Update
    suspend fun updateWorker(worker: WorkerEntity)
}

@Dao
interface HourlyRateDao {
    @Query("SELECT * FROM hourly_rate_history WHERE workerId = :workerId ORDER BY effectiveDateIso DESC")
    fun getRatesForWorker(workerId: String): Flow<List<HourlyRateHistoryEntity>>

    @Query("SELECT * FROM hourly_rate_history WHERE workerId = :workerId ORDER BY effectiveDateIso DESC")
    suspend fun getRatesForWorkerList(workerId: String): List<HourlyRateHistoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRate(rate: HourlyRateHistoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRates(rates: List<HourlyRateHistoryEntity>)
}

@Dao
interface AttendanceDao {
    @Query("SELECT * FROM attendance_records WHERE dateIso = :dateIso")
    fun getAttendanceForDate(dateIso: String): Flow<List<AttendanceRecordEntity>>

    @Query("SELECT * FROM attendance_records WHERE workerId = :workerId ORDER BY dateIso DESC")
    fun getAttendanceForWorker(workerId: String): Flow<List<AttendanceRecordEntity>>

    @Query("SELECT * FROM attendance_records WHERE dateIso LIKE :monthPrefix || '%' ORDER BY dateIso ASC")
    fun getAttendanceForMonth(monthPrefix: String): Flow<List<AttendanceRecordEntity>>

    @Query("SELECT * FROM attendance_records WHERE workerId = :workerId AND dateIso LIKE :monthPrefix || '%' ORDER BY dateIso ASC")
    fun getAttendanceForWorkerInMonth(workerId: String, monthPrefix: String): Flow<List<AttendanceRecordEntity>>

    @Query("SELECT * FROM attendance_records WHERE id = :id")
    suspend fun getRecordById(id: String): AttendanceRecordEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: AttendanceRecordEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecords(records: List<AttendanceRecordEntity>)

    @Query("DELETE FROM attendance_records WHERE id = :id")
    suspend fun deleteRecord(id: String)
}

@Dao
interface AdvanceDao {
    @Query("SELECT * FROM advances WHERE workerId = :workerId ORDER BY dateIso DESC")
    fun getAdvancesForWorker(workerId: String): Flow<List<AdvanceEntity>>

    @Query("SELECT * FROM advances WHERE dateIso LIKE :monthPrefix || '%' ORDER BY dateIso DESC")
    fun getAdvancesForMonth(monthPrefix: String): Flow<List<AdvanceEntity>>

    @Query("SELECT * FROM advances ORDER BY dateIso DESC")
    fun getAllAdvances(): Flow<List<AdvanceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAdvance(advance: AdvanceEntity)
}

@Dao
interface PayrollAdjustmentDao {
    @Query("SELECT * FROM payroll_adjustments WHERE yearMonth = :yearMonth ORDER BY dateIso DESC")
    fun getAdjustmentsForMonth(yearMonth: String): Flow<List<PayrollAdjustmentEntity>>

    @Query("SELECT * FROM payroll_adjustments WHERE workerId = :workerId AND yearMonth = :yearMonth ORDER BY dateIso DESC")
    fun getAdjustmentsForWorkerInMonth(workerId: String, yearMonth: String): Flow<List<PayrollAdjustmentEntity>>

    @Query("SELECT * FROM payroll_adjustments ORDER BY dateIso DESC")
    fun getAllAdjustments(): Flow<List<PayrollAdjustmentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAdjustment(adj: PayrollAdjustmentEntity)
}

@Dao
interface PaymentDao {
    @Query("SELECT * FROM payments WHERE yearMonth = :yearMonth ORDER BY dateIso DESC")
    fun getPaymentsForMonth(yearMonth: String): Flow<List<PaymentEntity>>

    @Query("SELECT * FROM payments WHERE workerId = :workerId AND yearMonth = :yearMonth ORDER BY dateIso DESC")
    fun getPaymentsForWorkerInMonth(workerId: String, yearMonth: String): Flow<List<PaymentEntity>>

    @Query("SELECT * FROM payments ORDER BY dateIso DESC")
    fun getAllPayments(): Flow<List<PaymentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(payment: PaymentEntity)
}

@Dao
interface AnnouncementDao {
    @Query("SELECT * FROM announcements ORDER BY createdAtIso DESC")
    fun getAllAnnouncements(): Flow<List<AnnouncementEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnnouncement(announcement: AnnouncementEntity)

    @Query("DELETE FROM announcements WHERE id = :id")
    suspend fun deleteAnnouncement(id: String)
}
