package com.rifters.ebookreader.sync

import android.util.Log
import com.rifters.ebookreader.Book
import com.rifters.ebookreader.database.BookDao
import com.rifters.ebookreader.database.BookmarkDao
import com.rifters.ebookreader.database.SyncDao
import com.rifters.ebookreader.model.Bookmark
import com.rifters.ebookreader.model.BookSyncData
import com.rifters.ebookreader.model.BookmarkSyncData
import com.rifters.ebookreader.model.SyncItemType
import com.rifters.ebookreader.model.SyncStatus

/**
 * Repository for managing sync operations between local database and cloud.
 * Implements conflict resolution using last-write-wins strategy.
 */
class SyncRepository(
    private val cloudSyncService: CloudSyncService,
    private val bookDao: BookDao,
    private val bookmarkDao: BookmarkDao,
    private val syncDao: SyncDao
) {
    
    private companion object {
        const val TAG = "SyncRepository"
    }
    
    /**
     * Sync all pending changes to cloud
     */
    suspend fun syncAll(): Result<SyncResult> {
        var successCount = 0
        var failureCount = 0
        val errors = mutableListOf<String>()
        
        try {
            // Check authentication
            if (!cloudSyncService.isAuthenticated()) {
                val signInResult = cloudSyncService.signInAnonymously()
                if (signInResult.isFailure) {
                    return Result.failure(signInResult.exceptionOrNull() ?: Exception("Authentication failed"))
                }
            }
            
            val userId = cloudSyncService.getCurrentUserId() 
                ?: return Result.failure(Exception("No user ID available"))
            
            // Get all items needing sync
            val itemsToSync = syncDao.getItemsNeedingSync()
            
            for (item in itemsToSync) {
                val result = when (item.itemType) {
                    SyncItemType.BOOK -> syncBook(item.itemId, userId)
                    SyncItemType.BOOKMARK -> syncBookmark(item.itemId, userId)
                }
                
                if (result.isSuccess) {
                    successCount++
                    syncDao.updateSyncNeeded(
                        item.itemType,
                        item.itemId,
                        needsSync = false,
                        timestamp = System.currentTimeMillis()
                    )
                    syncDao.updateSyncError(item.itemType, item.itemId, null)
                } else {
                    failureCount++
                    val error = result.exceptionOrNull()?.message ?: "Unknown error"
                    errors.add(error)
                    syncDao.updateSyncError(item.itemType, item.itemId, error)
                }
            }
            
            return Result.success(SyncResult(successCount, failureCount, errors))
            
        } catch (e: Exception) {
            Log.e(TAG, "Sync failed", e)
            return Result.failure(e)
        }
    }
    
    /**
     * Sync a single book's progress to cloud
     */
    private suspend fun syncBook(bookId: Long, userId: String): Result<Unit> {
        return try {
            val book = bookDao.getBookById(bookId)
                ?: return Result.failure(Exception("Book not found"))
            
            val bookData = BookSyncData(
                bookId = bookId.toString(),
                userId = userId,
                title = book.title,
                author = book.author,
                totalPages = book.totalPages,
                currentPage = book.currentPage,
                isCompleted = book.isCompleted,
                progressPercentage = book.progressPercentage,
                lastOpened = book.lastOpened,
                lastSyncedTimestamp = System.currentTimeMillis()
            )
            
            cloudSyncService.syncBookProgress(bookData)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync book $bookId", e)
            Result.failure(e)
        }
    }
    
    /**
     * Sync a single bookmark to cloud
     */
    private suspend fun syncBookmark(bookmarkId: Long, userId: String): Result<Unit> {
        return try {
            val bookmark = bookmarkDao.getBookmarkById(bookmarkId)
                ?: return Result.failure(Exception("Bookmark not found"))
            
            val bookmarkData = BookmarkSyncData(
                bookmarkId = bookmarkId.toString(),
                userId = userId,
                bookId = bookmark.bookId.toString(),
                page = bookmark.page,
                position = bookmark.position,
                note = bookmark.note,
                timestamp = bookmark.timestamp,
                lastSyncedTimestamp = System.currentTimeMillis()
            )
            
            cloudSyncService.syncBookmark(bookmarkData)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync bookmark $bookmarkId", e)
            Result.failure(e)
        }
    }
    
    /**
     * Pull updates from cloud and merge with local data
     */
    suspend fun pullFromCloud(): Result<SyncResult> {
        var successCount = 0
        var failureCount = 0
        val errors = mutableListOf<String>()
        
        try {
            val userId = cloudSyncService.getCurrentUserId()
                ?: return Result.failure(Exception("No user ID available"))
            
            // Pull book progress
            val booksResult = cloudSyncService.getAllBookProgress(userId)
            if (booksResult.isSuccess) {
                booksResult.getOrNull()?.forEach { bookData ->
                    val mergeResult = mergeBookProgress(bookData)
                    if (mergeResult.isSuccess) {
                        successCount++
                    } else {
                        failureCount++
                        errors.add(mergeResult.exceptionOrNull()?.message ?: "Unknown error")
                    }
                }
            } else {
                failureCount++
                errors.add(booksResult.exceptionOrNull()?.message ?: "Failed to pull books")
            }
            
            return Result.success(SyncResult(successCount, failureCount, errors))
            
        } catch (e: Exception) {
            Log.e(TAG, "Pull from cloud failed", e)
            return Result.failure(e)
        }
    }
    
    /**
     * Merge cloud book progress with local data using last-write-wins strategy
     */
    private suspend fun mergeBookProgress(cloudData: BookSyncData): Result<Unit> {
        return try {
            val bookId = cloudData.bookId.toLongOrNull() 
                ?: return Result.failure(Exception("Invalid book ID"))
            
            val localBook = bookDao.getBookById(bookId)
            
            if (localBook == null) {
                // No local book, skip (book files are local only)
                return Result.success(Unit)
            }
            
            // Last-write-wins: compare timestamps
            if (cloudData.lastOpened > localBook.lastOpened) {
                // Cloud data is newer, update local
                bookDao.updateProgress(
                    bookId = bookId,
                    currentPage = cloudData.currentPage,
                    progressPercentage = cloudData.progressPercentage,
                    lastOpened = cloudData.lastOpened
                )
                bookDao.updateCompletionStatus(bookId, cloudData.isCompleted)
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to merge book progress", e)
            Result.failure(e)
        }
    }
    
    /**
     * Mark a book as needing sync
     */
    suspend fun markBookForSync(bookId: Long) {
        val syncStatus = syncDao.getSyncStatus(SyncItemType.BOOK, bookId)
        if (syncStatus == null) {
            syncDao.insertSyncStatus(
                SyncStatus(
                    itemType = SyncItemType.BOOK,
                    itemId = bookId,
                    needsSync = true
                )
            )
        } else {
            syncDao.updateSyncNeeded(
                SyncItemType.BOOK,
                bookId,
                needsSync = true,
                timestamp = System.currentTimeMillis()
            )
        }
    }
    
    /**
     * Mark a bookmark as needing sync
     */
    suspend fun markBookmarkForSync(bookmarkId: Long) {
        val syncStatus = syncDao.getSyncStatus(SyncItemType.BOOKMARK, bookmarkId)
        if (syncStatus == null) {
            syncDao.insertSyncStatus(
                SyncStatus(
                    itemType = SyncItemType.BOOKMARK,
                    itemId = bookmarkId,
                    needsSync = true
                )
            )
        } else {
            syncDao.updateSyncNeeded(
                SyncItemType.BOOKMARK,
                bookmarkId,
                needsSync = true,
                timestamp = System.currentTimeMillis()
            )
        }
    }
    
    /**
     * Get pending sync count
     */
    suspend fun getPendingSyncCount(): Int {
        return syncDao.getPendingSyncCount()
    }
}

/**
 * Result of a sync operation
 */
data class SyncResult(
    val successCount: Int,
    val failureCount: Int,
    val errors: List<String>
) {
    val totalCount: Int get() = successCount + failureCount
    val isFullSuccess: Boolean get() = failureCount == 0 && successCount > 0
}
