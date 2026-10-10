package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.domain.model.Worker
import com.example.domain.model.WorkerProfileStatus
import java.time.LocalDate

@Entity(tableName = "workers")
data class WorkerEntity(
  @PrimaryKey val id: String,
  val firstName: String,
  val lastName: String,
  val activeFrom: String,
  val activeTo: String?,
  val currentHourlyRate: Double,
  val status: String,
  val observations: String
) {
  fun toDomain(): Worker = Worker(
    id = id,
    firstName = firstName,
    lastName = lastName,
    activeFrom = LocalDate.parse(activeFrom),
    activeTo = activeTo?.let { LocalDate.parse(it) },
    currentHourlyRate = currentHourlyRate,
    status = runCatching { WorkerProfileStatus.valueOf(status) }.getOrDefault(WorkerProfileStatus.ACTIVE),
    observations = observations
  )

  companion object {
    fun fromDomain(worker: Worker): WorkerEntity = WorkerEntity(
      id = worker.id,
      firstName = worker.firstName,
      lastName = worker.lastName,
      activeFrom = worker.activeFrom.toString(),
      activeTo = worker.activeTo?.toString(),
      currentHourlyRate = worker.currentHourlyRate,
      status = worker.status.name,
      observations = worker.observations
    )
  }
}

@Entity(tableName = "daily_attendance", primaryKeys = ["workerId", "date"])
data class AttendanceEntity(
  val workerId: String,
  val date: String,
  val hoursWorked: Double,
  val status: String,
  val note: String
)
