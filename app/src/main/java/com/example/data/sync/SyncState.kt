package com.example.data.sync

/**
 * Honest synchronization state.
 * Never claims cloud synchronization unless a real remote result is confirmed.
 */
sealed class SyncState {
  data object LocalOnly : SyncState()
  data object Synchronizing : SyncState()
  data class Synchronized(val lastSyncTimestampMillis: Long) : SyncState()
  data class SyncUnavailable(val reason: String) : SyncState()
  data class Conflict(val message: String) : SyncState()
}

enum class StorageBackend {
  ROOM_LOCAL_DEVICE,
  FIRESTORE_CLOUD,
  GOOGLE_DRIVE_BACKUP
}
