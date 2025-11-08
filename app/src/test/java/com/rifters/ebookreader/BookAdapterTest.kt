package com.rifters.ebookreader

import org.junit.Assert.*
import org.junit.Test

class BookAdapterTest {
    
    private val diffCallback = BookAdapter.BookDiffCallback()
    
    @Test
    fun `areItemsTheSame returns true for same id`() {
        val book1 = Book(
            id = 1,
            title = "Test Book",
            author = "Author",
            filePath = "/path/to/book.pdf",
            fileSize = 1024,
            mimeType = "application/pdf"
        )
        
        val book2 = Book(
            id = 1,
            title = "Different Title",
            author = "Different Author",
            filePath = "/path/to/book.pdf",
            fileSize = 1024,
            mimeType = "application/pdf"
        )
        
        assertTrue(diffCallback.areItemsTheSame(book1, book2))
    }
    
    @Test
    fun `areItemsTheSame returns false for different id`() {
        val book1 = Book(
            id = 1,
            title = "Test Book",
            author = "Author",
            filePath = "/path/to/book.pdf",
            fileSize = 1024,
            mimeType = "application/pdf"
        )
        
        val book2 = Book(
            id = 2,
            title = "Test Book",
            author = "Author",
            filePath = "/path/to/book.pdf",
            fileSize = 1024,
            mimeType = "application/pdf"
        )
        
        assertFalse(diffCallback.areItemsTheSame(book1, book2))
    }
    
    @Test
    fun `areContentsTheSame returns true for identical books`() {
        val book1 = Book(
            id = 1,
            title = "Test Book",
            author = "Author",
            filePath = "/path/to/book.pdf",
            fileSize = 1024,
            mimeType = "application/pdf",
            currentPage = 10,
            totalPages = 100,
            progressPercentage = 10f
        )
        
        val book2 = Book(
            id = 1,
            title = "Test Book",
            author = "Author",
            filePath = "/path/to/book.pdf",
            fileSize = 1024,
            mimeType = "application/pdf",
            currentPage = 10,
            totalPages = 100,
            progressPercentage = 10f
        )
        
        assertTrue(diffCallback.areContentsTheSame(book1, book2))
    }
    
    @Test
    fun `areContentsTheSame returns false when title changes`() {
        val book1 = Book(
            id = 1,
            title = "Test Book",
            author = "Author",
            filePath = "/path/to/book.pdf",
            fileSize = 1024,
            mimeType = "application/pdf"
        )
        
        val book2 = Book(
            id = 1,
            title = "Different Book",
            author = "Author",
            filePath = "/path/to/book.pdf",
            fileSize = 1024,
            mimeType = "application/pdf"
        )
        
        assertFalse(diffCallback.areContentsTheSame(book1, book2))
    }
    
    @Test
    fun `areContentsTheSame returns false when author changes`() {
        val book1 = Book(
            id = 1,
            title = "Test Book",
            author = "Author1",
            filePath = "/path/to/book.pdf",
            fileSize = 1024,
            mimeType = "application/pdf"
        )
        
        val book2 = Book(
            id = 1,
            title = "Test Book",
            author = "Author2",
            filePath = "/path/to/book.pdf",
            fileSize = 1024,
            mimeType = "application/pdf"
        )
        
        assertFalse(diffCallback.areContentsTheSame(book1, book2))
    }
    
    @Test
    fun `areContentsTheSame returns false when progress changes`() {
        val book1 = Book(
            id = 1,
            title = "Test Book",
            author = "Author",
            filePath = "/path/to/book.pdf",
            fileSize = 1024,
            mimeType = "application/pdf",
            currentPage = 10,
            progressPercentage = 10f
        )
        
        val book2 = Book(
            id = 1,
            title = "Test Book",
            author = "Author",
            filePath = "/path/to/book.pdf",
            fileSize = 1024,
            mimeType = "application/pdf",
            currentPage = 20,
            progressPercentage = 20f
        )
        
        assertFalse(diffCallback.areContentsTheSame(book1, book2))
    }
    
    @Test
    fun `areContentsTheSame returns false when lastOpened changes`() {
        val book1 = Book(
            id = 1,
            title = "Test Book",
            author = "Author",
            filePath = "/path/to/book.pdf",
            fileSize = 1024,
            mimeType = "application/pdf",
            lastOpened = 1000L
        )
        
        val book2 = Book(
            id = 1,
            title = "Test Book",
            author = "Author",
            filePath = "/path/to/book.pdf",
            fileSize = 1024,
            mimeType = "application/pdf",
            lastOpened = 2000L
        )
        
        assertFalse(diffCallback.areContentsTheSame(book1, book2))
    }
    
    @Test
    fun `areContentsTheSame returns false when completion status changes`() {
        val book1 = Book(
            id = 1,
            title = "Test Book",
            author = "Author",
            filePath = "/path/to/book.pdf",
            fileSize = 1024,
            mimeType = "application/pdf",
            isCompleted = false
        )
        
        val book2 = Book(
            id = 1,
            title = "Test Book",
            author = "Author",
            filePath = "/path/to/book.pdf",
            fileSize = 1024,
            mimeType = "application/pdf",
            isCompleted = true
        )
        
        assertFalse(diffCallback.areContentsTheSame(book1, book2))
    }
    
    @Test
    fun `areContentsTheSame detects swapped books with same id`() {
        // This is the specific case from the issue:
        // "When a collection swaps one book for another (keeping the same count)"
        // This test ensures that if a book's content changes (even if ID stays same),
        // the UI will update
        
        val originalBook = Book(
            id = 1,
            title = "Original Book",
            author = "Original Author",
            filePath = "/path/to/original.pdf",
            fileSize = 1024,
            mimeType = "application/pdf"
        )
        
        val swappedBook = Book(
            id = 1,
            title = "Swapped Book",
            author = "Swapped Author",
            filePath = "/path/to/swapped.pdf",
            fileSize = 2048,
            mimeType = "application/pdf"
        )
        
        // areItemsTheSame should return true (same id)
        assertTrue(diffCallback.areItemsTheSame(originalBook, swappedBook))
        
        // areContentsTheSame should return false (different content)
        // This ensures the UI will update to show the swapped book
        assertFalse(diffCallback.areContentsTheSame(originalBook, swappedBook))
    }
}
