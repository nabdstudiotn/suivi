package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.domain.model.AdjustmentType
import com.example.domain.model.AdvanceRecord
import com.example.domain.model.Announcement
import com.example.domain.model.AttendanceEntry
import com.example.domain.model.AttendanceStatus
import com.example.domain.model.PaymentRecord
import com.example.domain.model.PayrollAdjustment

@Entity(tableName = "attendance_records")
data class AttendanceRecordEntity(
    @PrimaryKey val id: String, // composite key: "${workerId}_${dateIso}"
    val workerId: String,
    val dateIso: String,
    val status: String, // "BLANK", "WORKED", "ABSENT", "REST_DAY"
    val hours: Int,
    val minutes: Int,
    val note: String? = null
) {
    fun toDomain(): AttendanceEntry = AttendanceEntry(
        workerId = workerId,
        dateIso = dateIso,
        status = runCatching { AttendanceStatus.valueOf(status) }.getOrDefault(AttendanceStatus.BLANK),
        hours = hours,
        minutes = minutes,
        note = note
    )

    companion object {
        fun fromDomain(entry: AttendanceEntry): AttendanceRecordEntity = AttendanceRecordEntity(
            id = "${entry.workerId}_${entry.dateIso}",
            workerId = entry.workerId,
            dateIso = entry.dateIso,
            status = entry.status.name,
            hours = entry.hours,
            minutes = entry.minutes,
            note = entry.note
        )
    }
}

@Entity(tableName = "advances")
data class AdvanceEntity(
    @PrimaryKey val id: String,
    val workerId: String,
    val dateIso: String,
    val amountMillimes: Long,
    val motif: String,
    val author: String = "Gérante"
) {
    fun toDomain(): AdvanceRecord = AdvanceRecord(
        id = id,
        workerId = workerId,
        dateIso = dateIso,
        amountMillimes = amountMillimes,
        motif = motif,
        author = author
    )

    companion object {
        fun fromDomain(record: AdvanceRecord): AdvanceEntity = AdvanceEntity(
            id = record.id,
            workerId = record.workerId,
            dateIso = record.dateIso,
            amountMillimes = record.amountMillimes,
            motif = record.motif,
            author = record.author
        )
    }
}

@Entity(tableName = "payroll_adjustments")
data class PayrollAdjustmentEntity(
    @PrimaryKey val id: String,
    val type: String, // "PRIME", "RETENUE", "PRIME_AID"
    val workerId: String,
    val yearMonth: String,
    val dateIso: String,
    val amountMillimes: Long,
    val motif: String,
    val note: String? = null,
    val author: String = "Gérante"
) {
    fun toDomain(): PayrollAdjustment = PayrollAdjustment(
        id = id,
        type = runCatching { AdjustmentType.valueOf(type) }.getOrDefault(AdjustmentType.PRIME),
        workerId = workerId,
        yearMonth = yearMonth,
        dateIso = dateIso,
        amountMillimes = amountMillimes,
        motif = motif,
        note = note,
        author = author
    )

    companion object {
        fun fromDomain(adj: PayrollAdjustment): PayrollAdjustmentEntity = PayrollAdjustmentEntity(
            id = adj.id,
            type = adj.type.name,
            workerId = adj.workerId,
            yearMonth = adj.yearMonth,
            dateIso = adj.dateIso,
            amountMillimes = adj.amountMillimes,
            motif = adj.motif,
            note = adj.note,
            author = adj.author
        )
    }
}

@Entity(tableName = "payments")
data class PaymentEntity(
    @PrimaryKey val id: String,
    val workerId: String,
    val yearMonth: String,
    val dateIso: String,
    val amountMillimes: Long,
    val paymentMethod: String = "Espèces",
    val payerName: String = "Gérante",
    val note: String? = null
) {
    fun toDomain(): PaymentRecord = PaymentRecord(
        id = id,
        workerId = workerId,
        yearMonth = yearMonth,
        dateIso = dateIso,
        amountMillimes = amountMillimes,
        paymentMethod = paymentMethod,
        payerName = payerName,
        note = note
    )

    companion object {
        fun fromDomain(p: PaymentRecord): PaymentEntity = PaymentEntity(
            id = p.id,
            workerId = p.workerId,
            yearMonth = p.yearMonth,
            dateIso = p.dateIso,
            amountMillimes = p.amountMillimes,
            paymentMethod = p.paymentMethod,
            payerName = p.payerName,
            note = p.note
        )
    }
}

@Entity(tableName = "announcements")
data class AnnouncementEntity(
    @PrimaryKey val id: String,
    val title: String,
    val content: String,
    val targetAudience: String,
    val expirationDateIso: String,
    val author: String,
    val createdAtIso: String
) {
    fun toDomain(): Announcement = Announcement(
        id = id,
        title = title,
        content = content,
        targetAudience = targetAudience,
        expirationDateIso = expirationDateIso,
        author = author,
        createdAtIso = createdAtIso
    )

    companion object {
        fun fromDomain(a: Announcement): AnnouncementEntity = AnnouncementEntity(
            id = a.id,
            title = a.title,
            content = a.content,
            targetAudience = a.targetAudience,
            expirationDateIso = a.expirationDateIso,
            author = a.author,
            createdAtIso = a.createdAtIso
        )
    }
}
