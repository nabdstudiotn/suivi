package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.domain.model.HourlyRateRecord

@Entity(tableName = "hourly_rate_history")
data class HourlyRateHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val workerId: String,
    val effectiveDateIso: String,
    val rateMillimes: Long,
    val motif: String
) {
    fun toDomain(): HourlyRateRecord = HourlyRateRecord(
        id = id,
        workerId = workerId,
        effectiveDateIso = effectiveDateIso,
        rateMillimes = rateMillimes,
        motif = motif
    )

    companion object {
        fun fromDomain(record: HourlyRateRecord): HourlyRateHistoryEntity = HourlyRateHistoryEntity(
            id = record.id,
            workerId = record.workerId,
            effectiveDateIso = record.effectiveDateIso,
            rateMillimes = record.rateMillimes,
            motif = record.motif
        )
    }
}
