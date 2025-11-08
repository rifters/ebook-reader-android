package com.rifters.ebookreader.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.rifters.ebookreader.database.BookDatabase

/**
 * Background worker for syncing data with cloud storage.
 * Runs periodically to keep local and cloud data in sync.
 */
class SyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    
    override suspend fun doWork(): Result {
        return try {
            val database = BookDatabase.getDatabase(applicationContext)
            val syncService = FirebaseSyncService()
            val syncRepository = SyncRepository(
                cloudSyncService = syncService,
                bookDao = database.bookDao(),
                bookmarkDao = database.bookmarkDao(),
                syncDao = database.syncDao()
            )
            
            // Check if user is authenticated
            if (!syncService.isAuthenticated()) {
                // Skip sync if not authenticated
                return Result.success()
            }
            
            // Perform sync
            val syncResult = syncRepository.syncAll()
            
            if (syncResult.isSuccess) {
                // Pull updates from cloud
                val pullResult = syncRepository.pullFromCloud()
                
                if (pullResult.isSuccess) {
                    Result.success()
                } else {
                    Result.retry()
                }
            } else {
                Result.retry()
            }
        } catch (e: Exception) {
            Result.failure()
        }
    }
}
