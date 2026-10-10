package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Suivi Room Database: On-device offline storage.
 * Primary offline copy for manager phone.
 */
@Database(
  entities = [WorkerEntity::class, AttendanceEntity::class],
  version = 1,
  exportSchema = false
)
abstract class SuiviDatabase : RoomDatabase() {
  abstract fun workerDao(): WorkerDao
  abstract fun attendanceDao(): AttendanceDao

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
          .fallbackToDestructiveMigration(false)
          .build()
        INSTANCE = instance
        instance
      }
    }
  }
}
