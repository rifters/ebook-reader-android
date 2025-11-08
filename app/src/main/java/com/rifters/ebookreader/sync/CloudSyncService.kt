package com.rifters.ebookreader.sync

import com.rifters.ebookreader.model.BookSyncData
import com.rifters.ebookreader.model.BookmarkSyncData

/**
 * Interface for cloud sync operations.
 * This abstraction allows for different cloud storage implementations (Firebase, AWS, etc.)
 */
interface CloudSyncService {
    
    /**
     * Initialize the sync service
     */
    suspend fun initialize(): Boolean
    
    /**
     * Check if user is authenticated
     */
    suspend fun isAuthenticated(): Boolean
    
    /**
     * Sign in anonymously for cloud sync
     */
    suspend fun signInAnonymously(): Result<String>
    
    /**
     * Get current user ID
     */
    suspend fun getCurrentUserId(): String?
    
    /**
     * Sync book progress to cloud
     */
    suspend fun syncBookProgress(bookData: BookSyncData): Result<Unit>
    
    /**
     * Sync bookmark to cloud
     */
    suspend fun syncBookmark(bookmarkData: BookmarkSyncData): Result<Unit>
    
    /**
     * Get book progress from cloud
     */
    suspend fun getBookProgress(bookId: String, userId: String): Result<BookSyncData?>
    
    /**
     * Get all book progress for user
     */
    suspend fun getAllBookProgress(userId: String): Result<List<BookSyncData>>
    
    /**
     * Get bookmarks for a book from cloud
     */
    suspend fun getBookmarks(bookId: String, userId: String): Result<List<BookmarkSyncData>>
    
    /**
     * Delete book progress from cloud
     */
    suspend fun deleteBookProgress(bookId: String, userId: String): Result<Unit>
    
    /**
     * Delete bookmark from cloud
     */
    suspend fun deleteBookmark(bookmarkId: String, userId: String): Result<Unit>
}
