package com.example.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkerDao {
  @Query("SELECT * FROM workers ORDER BY lastName ASC, firstName ASC")
  fun getAllWorkers(): Flow<List<WorkerEntity>>

  @Query("SELECT * FROM workers WHERE id = :id")
  suspend fun getWorkerById(id: String): WorkerEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertOrUpdate(worker: WorkerEntity)

  @Query("DELETE FROM workers WHERE id = :id")
  suspend fun deleteWorker(id: String)
}

@Dao
interface AttendanceDao {
  @Query("SELECT * FROM daily_attendance WHERE date = :date")
  fun getAttendanceForDate(date: String): Flow<List<AttendanceEntity>>

  @Query("SELECT * FROM daily_attendance WHERE workerId = :workerId ORDER BY date DESC")
  fun getAttendanceForWorker(workerId: String): Flow<List<AttendanceEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertOrUpdate(attendance: AttendanceEntity)
}
