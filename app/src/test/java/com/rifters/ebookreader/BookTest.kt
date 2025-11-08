package com.rifters.ebookreader

import org.junit.Assert.*
import org.junit.Test

class BookTest {
    
    @Test
    fun `book creation with valid data`() {
        val book = Book(
            id = 1,
            title = "Test Book",
            author = "Test Author",
            filePath = "/path/to/book.pdf",
            fileSize = 1024,
            mimeType = "application/pdf"
        )
        
        assertEquals(1, book.id)
        assertEquals("Test Book", book.title)
        assertEquals("Test Author", book.author)
        assertEquals("/path/to/book.pdf", book.filePath)
        assertEquals(1024, book.fileSize)
        assertEquals("application/pdf", book.mimeType)
    }
    
    @Test
    fun `book progress calculation`() {
        val book = Book(
            id = 1,
            title = "Test Book",
            author = "Test Author",
            filePath = "/path/to/book.pdf",
            fileSize = 1024,
            mimeType = "application/pdf",
            totalPages = 100,
            currentPage = 50,
            progressPercentage = 50f
        )
        
        assertEquals(50, book.currentPage)
        assertEquals(50f, book.progressPercentage, 0.01f)
    }
    
    @Test
    fun `book completion status`() {
        val book = Book(
            id = 1,
            title = "Test Book",
            author = "Test Author",
            filePath = "/path/to/book.pdf",
            fileSize = 1024,
            mimeType = "application/pdf",
            isCompleted = true
        )
        
        assertTrue(book.isCompleted)
    }
    
    @Test
    fun `book default values`() {
        val book = Book(
            title = "Test Book",
            author = "Test Author",
            filePath = "/path/to/book.pdf",
            fileSize = 1024,
            mimeType = "application/pdf"
        )
        
        assertEquals(0, book.id)
        assertEquals(0, book.lastOpened)
        assertNull(book.coverImagePath)
        assertEquals(0, book.totalPages)
        assertEquals(0, book.currentPage)
        assertFalse(book.isCompleted)
        assertEquals(0f, book.progressPercentage, 0.01f)
    }
    
    @Test
    fun `book timestamp is set on creation`() {
        val currentTime = System.currentTimeMillis()
        val book = Book(
            title = "Test Book",
            author = "Test Author",
            filePath = "/path/to/book.pdf",
            fileSize = 1024,
            mimeType = "application/pdf"
        )
        
        assertTrue(book.dateAdded >= currentTime)
        assertTrue(book.dateAdded <= System.currentTimeMillis())
    }
    
    @Test
    fun `book with MOBI mime type`() {
        val book = Book(
            title = "Test MOBI Book",
            author = "Test Author",
            filePath = "/path/to/book.mobi",
            fileSize = 2048,
            mimeType = "application/x-mobipocket-ebook"
        )
        
        assertEquals("Test MOBI Book", book.title)
        assertEquals("application/x-mobipocket-ebook", book.mimeType)
        assertEquals("/path/to/book.mobi", book.filePath)
    }
    
    @Test
    fun `book with CBZ mime type`() {
        val book = Book(
            title = "Test Comic",
            author = "Comic Author",
            filePath = "/path/to/comic.cbz",
            fileSize = 5120,
            mimeType = "application/vnd.comicbook+zip"
        )
        
        assertEquals("Test Comic", book.title)
        assertEquals("application/vnd.comicbook+zip", book.mimeType)
        assertEquals("/path/to/comic.cbz", book.filePath)
    }
    
    @Test
    fun `book with CBR mime type`() {
        val book = Book(
            title = "Test Comic RAR",
            author = "Comic Author",
            filePath = "/path/to/comic.cbr",
            fileSize = 6144,
            mimeType = "application/vnd.comicbook-rar"
        )
        
        assertEquals("Test Comic RAR", book.title)
        assertEquals("application/vnd.comicbook-rar", book.mimeType)
        assertEquals("/path/to/comic.cbr", book.filePath)
    }
    
    @Test
    fun `book with EPUB mime type`() {
        val book = Book(
            title = "Test EPUB",
            author = "EPUB Author",
            filePath = "/path/to/book.epub",
            fileSize = 3072,
            mimeType = "application/epub+zip"
        )
        
        assertEquals("Test EPUB", book.title)
        assertEquals("application/epub+zip", book.mimeType)
        assertEquals("/path/to/book.epub", book.filePath)
    }
}
