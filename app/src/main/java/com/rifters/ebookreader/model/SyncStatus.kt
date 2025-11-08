package com.rifters.ebookreader.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Represents the sync status of a syncable item (book or bookmark).
 * Tracks when items were last synced and whether they need syncing.
 */
@Entity(tableName = "sync_status")
@Parcelize
data class SyncStatus(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val itemType: SyncItemType,
    val itemId: Long,
    val lastSyncedTimestamp: Long = 0,
    val lastModifiedTimestamp: Long = System.currentTimeMillis(),
    val needsSync: Boolean = true,
    val syncError: String? = null
) : Parcelable

enum class SyncItemType {
    BOOK,
    BOOKMARK
}
