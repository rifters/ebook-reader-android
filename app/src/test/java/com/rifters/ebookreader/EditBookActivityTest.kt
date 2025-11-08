package com.rifters.ebookreader

import org.junit.Assert.*
import org.junit.Test

class EditBookActivityTest {
    
    @Test
    fun `book metadata can be updated`() {
        val originalBook = Book(
            id = 1,
            title = "Original Title",
            author = "Original Author",
            filePath = "/path/to/book.pdf",
            fileSize = 1024,
            mimeType = "application/pdf"
        )
        
        val updatedBook = originalBook.copy(
            title = "Updated Title",
            author = "Updated Author",
            coverImagePath = "/path/to/cover.jpg"
        )
        
        assertEquals(1, updatedBook.id)
        assertEquals("Updated Title", updatedBook.title)
        assertEquals("Updated Author", updatedBook.author)
        assertEquals("/path/to/cover.jpg", updatedBook.coverImagePath)
        assertEquals("/path/to/book.pdf", updatedBook.filePath) // unchanged
    }
    
    @Test
    fun `cover image path can be set`() {
        val book = Book(
            id = 1,
            title = "Test Book",
            author = "Test Author",
            filePath = "/path/to/book.pdf",
            fileSize = 1024,
            mimeType = "application/pdf"
        )
        
        assertNull(book.coverImagePath)
        
        val bookWithCover = book.copy(coverImagePath = "/path/to/cover.jpg")
        
        assertNotNull(bookWithCover.coverImagePath)
        assertEquals("/path/to/cover.jpg", bookWithCover.coverImagePath)
    }
    
    @Test
    fun `cover image path can be updated`() {
        val book = Book(
            id = 1,
            title = "Test Book",
            author = "Test Author",
            filePath = "/path/to/book.pdf",
            fileSize = 1024,
            mimeType = "application/pdf",
            coverImagePath = "/path/to/old_cover.jpg"
        )
        
        val bookWithNewCover = book.copy(coverImagePath = "/path/to/new_cover.jpg")
        
        assertEquals("/path/to/new_cover.jpg", bookWithNewCover.coverImagePath)
    }
    
    @Test
    fun `book title and author validation`() {
        // Title cannot be empty after update
        val titleToValidate = "   "
        assertTrue(titleToValidate.trim().isEmpty())
        
        // Author cannot be empty after update
        val authorToValidate = "   "
        assertTrue(authorToValidate.trim().isEmpty())
        
        // Valid title and author
        val validTitle = "Valid Title"
        val validAuthor = "Valid Author"
        assertTrue(validTitle.trim().isNotEmpty())
        assertTrue(validAuthor.trim().isNotEmpty())
    }
    
    @Test
    fun `book metadata preserves other fields on update`() {
        val book = Book(
            id = 1,
            title = "Original Title",
            author = "Original Author",
            filePath = "/path/to/book.pdf",
            fileSize = 1024,
            mimeType = "application/pdf",
            dateAdded = 1234567890L,
            lastOpened = 1234567899L,
            totalPages = 100,
            currentPage = 50,
            progressPercentage = 50f,
            isCompleted = false
        )
        
        val updatedBook = book.copy(
            title = "New Title",
            author = "New Author"
        )
        
        // Metadata fields updated
        assertEquals("New Title", updatedBook.title)
        assertEquals("New Author", updatedBook.author)
        
        // Other fields preserved
        assertEquals(1024, updatedBook.fileSize)
        assertEquals("application/pdf", updatedBook.mimeType)
        assertEquals(1234567890L, updatedBook.dateAdded)
        assertEquals(1234567899L, updatedBook.lastOpened)
        assertEquals(100, updatedBook.totalPages)
        assertEquals(50, updatedBook.currentPage)
        assertEquals(50f, updatedBook.progressPercentage, 0.01f)
        assertFalse(updatedBook.isCompleted)
    }
}
