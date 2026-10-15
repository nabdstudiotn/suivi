package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Suivi Room Database:
 * Provides permanent on-device cache and reliable outbox queue.
 */
@Database(
    entities = [
        WorkerEntity::class,
        AttendanceRecordEntity::class,
        HourlyRateHistoryEntity::class,
        AdvanceEntity::class,
        PayrollAdjustmentEntity::class,
        PaymentEntity::class,
        AnnouncementEntity::class,
        ChatMessageEntity::class,
        ChatConversationEntity::class,
        CompanyMemberEntity::class,
        WorkshopAnnouncementEntity::class,
        AnnouncementReceiptEntity::class,
        AnnouncementAuditEntity::class,
        AuditEventEntity::class,
        CompanyProfileEntity::class,
        ChafaPermissionsEntity::class,
        DeviceSessionEntity::class,
        MonthClosureEntity::class,
        CompanyEntity::class,
        JoinRequestEntity::class,
        UserAccountEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class SuiviDatabase : RoomDatabase() {
    abstract fun workerDao(): WorkerDao
    abstract fun attendanceDao(): AttendanceDao
    abstract fun hourlyRateDao(): HourlyRateDao
    abstract fun advanceDao(): AdvanceDao
    abstract fun payrollAdjustmentDao(): PayrollAdjustmentDao
    abstract fun paymentDao(): PaymentDao
    abstract fun announcementDao(): AnnouncementDao
    abstract fun chatMessageDao(): ChatMessageDao
    abstract fun chatConversationDao(): ChatConversationDao
    abstract fun companyMemberDao(): CompanyMemberDao
    abstract fun workshopAnnouncementDao(): WorkshopAnnouncementDao
    abstract fun announcementReceiptDao(): AnnouncementReceiptDao
    abstract fun announcementAuditDao(): AnnouncementAuditDao
    abstract fun auditEventDao(): AuditEventDao
    abstract fun companyProfileDao(): CompanyProfileDao
    abstract fun chafaPermissionsDao(): ChafaPermissionsDao
    abstract fun deviceSessionDao(): DeviceSessionDao
    abstract fun monthClosureDao(): MonthClosureDao
    abstract fun companyDao(): CompanyDao
    abstract fun joinRequestDao(): JoinRequestDao
    abstract fun userAccountDao(): UserAccountDao

    companion object {
        @Volatile
        private var INSTANCE: SuiviDatabase? = null

        fun getDatabase(context: Context): SuiviDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SuiviDatabase::class.java,
                    "suivi_offline.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
