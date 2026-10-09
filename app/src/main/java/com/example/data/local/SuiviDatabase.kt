package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration

@Database(
    entities = [
        WorkerEntity::class,
        HourlyRateHistoryEntity::class,
        AttendanceRecordEntity::class,
        AdvanceEntity::class,
        PayrollAdjustmentEntity::class,
        PaymentEntity::class,
        AnnouncementEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class SuiviDatabase : RoomDatabase() {

    abstract fun workerDao(): WorkerDao
    abstract fun hourlyRateDao(): HourlyRateDao
    abstract fun attendanceDao(): AttendanceDao
    abstract fun advanceDao(): AdvanceDao
    abstract fun payrollAdjustmentDao(): PayrollAdjustmentDao
    abstract fun paymentDao(): PaymentDao
    abstract fun announcementDao(): AnnouncementDao

    companion object {
        @Volatile
        private var INSTANCE: SuiviDatabase? = null

        val MIGRATIONS: Array<Migration> = arrayOf()

        fun getDatabase(context: Context): SuiviDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SuiviDatabase::class.java,
                    "suivi_atelier.db"
                )
                    .addMigrations(*MIGRATIONS)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
