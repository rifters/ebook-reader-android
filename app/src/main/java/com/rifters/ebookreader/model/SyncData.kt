package com.rifters.ebookreader.model

/**
 * Syncable data for a Book that will be stored in Firestore.
 * Contains only the necessary sync data (reading progress, not file content).
 */
data class BookSyncData(
    val bookId: String = "",
    val userId: String = "",
    val title: String = "",
    val author: String = "",
    val totalPages: Int = 0,
    val currentPage: Int = 0,
    val isCompleted: Boolean = false,
    val progressPercentage: Float = 0f,
    val lastOpened: Long = 0,
    val lastSyncedTimestamp: Long = System.currentTimeMillis()
) {
    // No-arg constructor for Firebase
    constructor() : this("", "", "", "", 0, 0, false, 0f, 0, 0)
    
    /**
     * Create a map for Firestore
     */
    fun toMap(): Map<String, Any> {
        return mapOf(
            "bookId" to bookId,
            "userId" to userId,
            "title" to title,
            "author" to author,
            "totalPages" to totalPages,
            "currentPage" to currentPage,
            "isCompleted" to isCompleted,
            "progressPercentage" to progressPercentage,
            "lastOpened" to lastOpened,
            "lastSyncedTimestamp" to lastSyncedTimestamp
        )
    }
}

/**
 * Syncable data for a Bookmark that will be stored in Firestore.
 */
data class BookmarkSyncData(
    val bookmarkId: String = "",
    val userId: String = "",
    val bookId: String = "",
    val page: Int = 0,
    val position: Int = 0,
    val note: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val lastSyncedTimestamp: Long = System.currentTimeMillis()
) {
    // No-arg constructor for Firebase
    constructor() : this("", "", "", 0, 0, null, 0, 0)
    
    /**
     * Create a map for Firestore
     */
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "bookmarkId" to bookmarkId,
            "userId" to userId,
            "bookId" to bookId,
            "page" to page,
            "position" to position,
            "note" to note,
            "timestamp" to timestamp,
            "lastSyncedTimestamp" to lastSyncedTimestamp
        )
    }
}
