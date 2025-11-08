package com.rifters.ebookreader.model

import org.junit.Assert.*
import org.junit.Test

class SyncDataTest {
    
    @Test
    fun `book sync data creation with valid data`() {
        val bookData = BookSyncData(
            bookId = "123",
            userId = "user1",
            title = "Test Book",
            author = "Test Author",
            totalPages = 100,
            currentPage = 50,
            isCompleted = false,
            progressPercentage = 50f,
            lastOpened = 1000L,
            lastSyncedTimestamp = 2000L
        )
        
        assertEquals("123", bookData.bookId)
        assertEquals("user1", bookData.userId)
        assertEquals("Test Book", bookData.title)
        assertEquals("Test Author", bookData.author)
        assertEquals(100, bookData.totalPages)
        assertEquals(50, bookData.currentPage)
        assertFalse(bookData.isCompleted)
        assertEquals(50f, bookData.progressPercentage, 0.01f)
        assertEquals(1000L, bookData.lastOpened)
        assertEquals(2000L, bookData.lastSyncedTimestamp)
    }
    
    @Test
    fun `book sync data to map`() {
        val bookData = BookSyncData(
            bookId = "123",
            userId = "user1",
            title = "Test Book",
            author = "Test Author",
            totalPages = 100,
            currentPage = 50,
            isCompleted = false,
            progressPercentage = 50f,
            lastOpened = 1000L,
            lastSyncedTimestamp = 2000L
        )
        
        val map = bookData.toMap()
        
        assertEquals("123", map["bookId"])
        assertEquals("user1", map["userId"])
        assertEquals("Test Book", map["title"])
        assertEquals("Test Author", map["author"])
        assertEquals(100, map["totalPages"])
        assertEquals(50, map["currentPage"])
        assertEquals(false, map["isCompleted"])
        assertEquals(50f, map["progressPercentage"])
        assertEquals(1000L, map["lastOpened"])
        assertEquals(2000L, map["lastSyncedTimestamp"])
    }
    
    @Test
    fun `bookmark sync data creation with valid data`() {
        val bookmarkData = BookmarkSyncData(
            bookmarkId = "456",
            userId = "user1",
            bookId = "123",
            page = 10,
            position = 500,
            note = "Test note",
            timestamp = 1000L,
            lastSyncedTimestamp = 2000L
        )
        
        assertEquals("456", bookmarkData.bookmarkId)
        assertEquals("user1", bookmarkData.userId)
        assertEquals("123", bookmarkData.bookId)
        assertEquals(10, bookmarkData.page)
        assertEquals(500, bookmarkData.position)
        assertEquals("Test note", bookmarkData.note)
        assertEquals(1000L, bookmarkData.timestamp)
        assertEquals(2000L, bookmarkData.lastSyncedTimestamp)
    }
    
    @Test
    fun `bookmark sync data to map`() {
        val bookmarkData = BookmarkSyncData(
            bookmarkId = "456",
            userId = "user1",
            bookId = "123",
            page = 10,
            position = 500,
            note = "Test note",
            timestamp = 1000L,
            lastSyncedTimestamp = 2000L
        )
        
        val map = bookmarkData.toMap()
        
        assertEquals("456", map["bookmarkId"])
        assertEquals("user1", map["userId"])
        assertEquals("123", map["bookId"])
        assertEquals(10, map["page"])
        assertEquals(500, map["position"])
        assertEquals("Test note", map["note"])
        assertEquals(1000L, map["timestamp"])
        assertEquals(2000L, map["lastSyncedTimestamp"])
    }
    
    @Test
    fun `bookmark sync data with null note`() {
        val bookmarkData = BookmarkSyncData(
            bookmarkId = "789",
            userId = "user1",
            bookId = "123",
            page = 20,
            position = 1000,
            note = null,
            timestamp = 3000L,
            lastSyncedTimestamp = 4000L
        )
        
        assertNull(bookmarkData.note)
        
        val map = bookmarkData.toMap()
        assertNull(map["note"])
    }
    
    @Test
    fun `book sync data no-arg constructor`() {
        val bookData = BookSyncData()
        
        assertEquals("", bookData.bookId)
        assertEquals("", bookData.userId)
        assertEquals("", bookData.title)
        assertEquals("", bookData.author)
        assertEquals(0, bookData.totalPages)
        assertEquals(0, bookData.currentPage)
        assertFalse(bookData.isCompleted)
        assertEquals(0f, bookData.progressPercentage, 0.01f)
        assertEquals(0L, bookData.lastOpened)
        assertEquals(0L, bookData.lastSyncedTimestamp)
    }
    
    @Test
    fun `bookmark sync data no-arg constructor`() {
        val bookmarkData = BookmarkSyncData()
        
        assertEquals("", bookmarkData.bookmarkId)
        assertEquals("", bookmarkData.userId)
        assertEquals("", bookmarkData.bookId)
        assertEquals(0, bookmarkData.page)
        assertEquals(0, bookmarkData.position)
        assertNull(bookmarkData.note)
        assertEquals(0L, bookmarkData.timestamp)
        assertEquals(0L, bookmarkData.lastSyncedTimestamp)
    }
}
