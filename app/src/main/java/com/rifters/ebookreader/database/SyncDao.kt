package com.rifters.ebookreader.database

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.rifters.ebookreader.model.SyncStatus
import com.rifters.ebookreader.model.SyncItemType

@Dao
interface SyncDao {
    
    @Query("SELECT * FROM sync_status WHERE itemType = :itemType AND itemId = :itemId")
    suspend fun getSyncStatus(itemType: SyncItemType, itemId: Long): SyncStatus?
    
    @Query("SELECT * FROM sync_status WHERE needsSync = 1")
    suspend fun getItemsNeedingSync(): List<SyncStatus>
    
    @Query("SELECT * FROM sync_status WHERE needsSync = 1")
    fun getItemsNeedingSyncLive(): LiveData<List<SyncStatus>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSyncStatus(syncStatus: SyncStatus): Long
    
    @Update
    suspend fun updateSyncStatus(syncStatus: SyncStatus)
    
    @Query("UPDATE sync_status SET needsSync = :needsSync, lastSyncedTimestamp = :timestamp WHERE itemType = :itemType AND itemId = :itemId")
    suspend fun updateSyncNeeded(itemType: SyncItemType, itemId: Long, needsSync: Boolean, timestamp: Long)
    
    @Query("UPDATE sync_status SET syncError = :error WHERE itemType = :itemType AND itemId = :itemId")
    suspend fun updateSyncError(itemType: SyncItemType, itemId: Long, error: String?)
    
    @Query("DELETE FROM sync_status WHERE itemType = :itemType AND itemId = :itemId")
    suspend fun deleteSyncStatus(itemType: SyncItemType, itemId: Long)
    
    @Query("DELETE FROM sync_status")
    suspend fun deleteAllSyncStatus()
    
    @Query("SELECT COUNT(*) FROM sync_status WHERE needsSync = 1")
    suspend fun getPendingSyncCount(): Int
}
