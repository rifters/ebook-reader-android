package com.rifters.ebookreader.sync

import org.junit.Assert.*
import org.junit.Test

class SyncResultTest {
    
    @Test
    fun `sync result with all success`() {
        val result = SyncResult(
            successCount = 5,
            failureCount = 0,
            errors = emptyList()
        )
        
        assertEquals(5, result.successCount)
        assertEquals(0, result.failureCount)
        assertEquals(5, result.totalCount)
        assertTrue(result.isFullSuccess)
        assertTrue(result.errors.isEmpty())
    }
    
    @Test
    fun `sync result with partial success`() {
        val errors = listOf("Error 1", "Error 2")
        val result = SyncResult(
            successCount = 3,
            failureCount = 2,
            errors = errors
        )
        
        assertEquals(3, result.successCount)
        assertEquals(2, result.failureCount)
        assertEquals(5, result.totalCount)
        assertFalse(result.isFullSuccess)
        assertEquals(2, result.errors.size)
        assertEquals("Error 1", result.errors[0])
        assertEquals("Error 2", result.errors[1])
    }
    
    @Test
    fun `sync result with all failures`() {
        val errors = listOf("Network error", "Auth error", "Timeout")
        val result = SyncResult(
            successCount = 0,
            failureCount = 3,
            errors = errors
        )
        
        assertEquals(0, result.successCount)
        assertEquals(3, result.failureCount)
        assertEquals(3, result.totalCount)
        assertFalse(result.isFullSuccess)
        assertEquals(3, result.errors.size)
    }
    
    @Test
    fun `sync result with no items`() {
        val result = SyncResult(
            successCount = 0,
            failureCount = 0,
            errors = emptyList()
        )
        
        assertEquals(0, result.successCount)
        assertEquals(0, result.failureCount)
        assertEquals(0, result.totalCount)
        assertFalse(result.isFullSuccess) // No success if nothing was synced
        assertTrue(result.errors.isEmpty())
    }
}
