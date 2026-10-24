package com.example.data.model

enum class SyncStatus {
    SYNCED,         // সম্পূর্ণ সিঙ্কড (All local data backed up)
    PENDING_SYNC,   // অফলাইনে সংরক্ষিত, সিঙ্ক অপেক্ষমাণ (Saved offline, awaiting cloud sync)
    SYNCING,        // সিঙ্ক হচ্ছে... (Sync in progress)
    FAILED          // সিঙ্ক ব্যর্থ (Sync error, retryable)
}

data class SyncState(
    val status: SyncStatus = SyncStatus.SYNCED,
    val pendingCount: Int = 0,
    val lastSyncTimestamp: Long = System.currentTimeMillis(),
    val errorMessage: String? = null
)
