package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.domain.model.Worker

@Entity(tableName = "workers")
data class WorkerEntity(
    @PrimaryKey val id: String,
    val firstName: String,
    val lastName: String,
    val initials: String,
    val currentRateMillimes: Long,
    val role: String,
    val isActive: Boolean = true,
    val hireDateIso: String,
    val returnDateIso: String? = null,
    val archivedDateIso: String? = null
) {
    fun toDomain(): Worker = Worker(
        id = id,
        firstName = firstName,
        lastName = lastName,
        initials = initials,
        currentRateMillimes = currentRateMillimes,
        role = role,
        isActive = isActive,
        hireDateIso = hireDateIso,
        returnDateIso = returnDateIso,
        archivedDateIso = archivedDateIso
    )

    companion object {
        fun fromDomain(worker: Worker): WorkerEntity = WorkerEntity(
            id = worker.id,
            firstName = worker.firstName,
            lastName = worker.lastName,
            initials = worker.initials,
            currentRateMillimes = worker.currentRateMillimes,
            role = worker.role,
            isActive = worker.isActive,
            hireDateIso = worker.hireDateIso,
            returnDateIso = worker.returnDateIso,
            archivedDateIso = worker.archivedDateIso
        )
    }
}
