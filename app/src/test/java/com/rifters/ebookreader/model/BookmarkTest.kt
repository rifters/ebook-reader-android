package com.rifters.ebookreader.model

import org.junit.Assert.*
import org.junit.Test

class BookmarkTest {
    
    @Test
    fun `bookmark creation with valid data`() {
        val bookmark = Bookmark(
            id = 1,
            bookId = 10,
            page = 5,
            position = 100,
            note = "Important section"
        )
        
        assertEquals(1, bookmark.id)
        assertEquals(10, bookmark.bookId)
        assertEquals(5, bookmark.page)
        assertEquals(100, bookmark.position)
        assertEquals("Important section", bookmark.note)
    }
    
    @Test
    fun `bookmark with null note`() {
        val bookmark = Bookmark(
            bookId = 10,
            page = 5,
            position = 100,
            note = null
        )
        
        assertNull(bookmark.note)
    }
    
    @Test
    fun `bookmark default values`() {
        val bookmark = Bookmark(
            bookId = 10,
            page = 5
        )
        
        assertEquals(0, bookmark.id)
        assertEquals(0, bookmark.position)
        assertNull(bookmark.note)
    }
    
    @Test
    fun `bookmark timestamp is set on creation`() {
        val currentTime = System.currentTimeMillis()
        val bookmark = Bookmark(
            bookId = 10,
            page = 5
        )
        
        assertTrue(bookmark.timestamp >= currentTime)
        assertTrue(bookmark.timestamp <= System.currentTimeMillis())
    }
    
    @Test
    fun `multiple bookmarks for same book`() {
        val bookmark1 = Bookmark(id = 1, bookId = 10, page = 5)
        val bookmark2 = Bookmark(id = 2, bookId = 10, page = 10)
        
        assertEquals(bookmark1.bookId, bookmark2.bookId)
        assertNotEquals(bookmark1.page, bookmark2.page)
    }
    
    @Test
    fun `bookmark with note can be created`() {
        val note = "This is an important section about the main character's motivation"
        val bookmark = Bookmark(
            bookId = 10,
            page = 5,
            note = note
        )
        
        assertEquals(note, bookmark.note)
    }
    
    @Test
    fun `bookmark note can be updated`() {
        val initialNote = "Initial note"
        val updatedNote = "Updated note with more details"
        
        val bookmark = Bookmark(
            id = 1,
            bookId = 10,
            page = 5,
            note = initialNote
        )
        
        val updatedBookmark = bookmark.copy(note = updatedNote)
        
        assertEquals(initialNote, bookmark.note)
        assertEquals(updatedNote, updatedBookmark.note)
    }
    
    @Test
    fun `bookmark note can be removed`() {
        val bookmark = Bookmark(
            id = 1,
            bookId = 10,
            page = 5,
            note = "Some note"
        )
        
        val bookmarkWithoutNote = bookmark.copy(note = null)
        
        assertNotNull(bookmark.note)
        assertNull(bookmarkWithoutNote.note)
    }
    
    @Test
    fun `bookmark with long note`() {
        val longNote = "This is a very long note that might contain a lot of details " +
                "about the content on this page. It could include quotes, thoughts, " +
                "or references to other parts of the book."
        
        val bookmark = Bookmark(
            bookId = 10,
            page = 5,
            note = longNote
        )
        
        assertEquals(longNote, bookmark.note)
        assertTrue(bookmark.note!!.length > 100)
    }
}
