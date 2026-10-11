package com.example.data

import android.content.Context
import com.example.data.local.AttendanceEntity
import com.example.data.local.SuiviDatabase
import com.example.data.local.WorkerEntity
import com.example.data.sync.SyncState
import com.example.domain.model.DailyAttendance
import com.example.domain.model.Worker
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

/**
 * AttendanceRepository:
 * Exposes local Room persistence as primary offline copy for manager phone.
 * Tracks and exposes honest SyncState without false claims.
 */
class AttendanceRepository(context: Context) {
  private val database = SuiviDatabase.getDatabase(context)
  private val workerDao = database.workerDao()
  private val attendanceDao = database.attendanceDao()

  // Real, honest sync status (no fake cloud claims)
  private val _syncState = MutableStateFlow<SyncState>(SyncState.LocalOnly)
  val syncState: Flow<SyncState> = _syncState.asStateFlow()

  fun getAllWorkers(): Flow<List<Worker>> {
    return workerDao.getAllWorkers().map { list ->
      list.map { it.toDomain() }
    }
  }

  suspend fun getWorkerById(id: String): Worker? {
    return workerDao.getWorkerById(id)?.toDomain()
  }

  suspend fun saveWorker(worker: Worker) {
    workerDao.insertOrUpdate(WorkerEntity.fromDomain(worker))
  }

  suspend fun deleteWorker(id: String) {
    workerDao.deleteWorker(id)
  }

  fun getAttendanceForDate(date: LocalDate): Flow<List<DailyAttendance>> {
    return attendanceDao.getAttendanceForDate(date.toString()).map { list ->
      list.map {
        DailyAttendance(
          workerId = it.workerId,
          date = LocalDate.parse(it.date),
          hoursWorked = it.hoursWorked,
          status = runCatching { com.example.domain.model.AttendanceStatus.valueOf(it.status) }
            .getOrDefault(com.example.domain.model.AttendanceStatus.UNRECORDED),
          note = it.note
        )
      }
    }
  }

  suspend fun recordAttendance(attendance: DailyAttendance) {
    attendanceDao.insertOrUpdate(
      AttendanceEntity(
        workerId = attendance.workerId,
        date = attendance.date.toString(),
        hoursWorked = attendance.hoursWorked,
        status = attendance.status.name,
        note = attendance.note
      )
    )
  }
}
