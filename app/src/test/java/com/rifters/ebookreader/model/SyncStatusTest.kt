package com.rifters.ebookreader.model

import org.junit.Assert.*
import org.junit.Test

class SyncStatusTest {
    
    @Test
    fun `sync status creation with valid data`() {
        val syncStatus = SyncStatus(
            id = 1,
            itemType = SyncItemType.BOOK,
            itemId = 100,
            lastSyncedTimestamp = 1000L,
            lastModifiedTimestamp = 2000L,
            needsSync = true,
            syncError = null
        )
        
        assertEquals(1, syncStatus.id)
        assertEquals(SyncItemType.BOOK, syncStatus.itemType)
        assertEquals(100, syncStatus.itemId)
        assertEquals(1000L, syncStatus.lastSyncedTimestamp)
        assertEquals(2000L, syncStatus.lastModifiedTimestamp)
        assertTrue(syncStatus.needsSync)
        assertNull(syncStatus.syncError)
    }
    
    @Test
    fun `sync status with error`() {
        val syncStatus = SyncStatus(
            itemType = SyncItemType.BOOKMARK,
            itemId = 200,
            needsSync = true,
            syncError = "Network error"
        )
        
        assertEquals(SyncItemType.BOOKMARK, syncStatus.itemType)
        assertEquals(200, syncStatus.itemId)
        assertTrue(syncStatus.needsSync)
        assertEquals("Network error", syncStatus.syncError)
    }
    
    @Test
    fun `sync status default values`() {
        val syncStatus = SyncStatus(
            itemType = SyncItemType.BOOK,
            itemId = 300
        )
        
        assertEquals(0, syncStatus.id)
        assertEquals(0L, syncStatus.lastSyncedTimestamp)
        assertTrue(syncStatus.needsSync)
        assertNull(syncStatus.syncError)
    }
    
    @Test
    fun `sync item types`() {
        assertEquals("BOOK", SyncItemType.BOOK.name)
        assertEquals("BOOKMARK", SyncItemType.BOOKMARK.name)
    }
}
