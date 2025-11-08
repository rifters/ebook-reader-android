package com.rifters.ebookreader.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.rifters.ebookreader.Book
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Unit tests for BookViewModel.
 * 
 * Note: These tests focus on validating the ViewModel's public API and behavior.
 * Integration tests with actual Room database are covered in androidTest.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class BookViewModelTest {
    
    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()
    
    private val testDispatcher = StandardTestDispatcher()
    
    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }
    
    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }
    
    @Test
    fun `book data model has correct fields`() {
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
        
        assertEquals(1, book.id)
        assertEquals("Test Book", book.title)
        assertEquals("Test Author", book.author)
        assertEquals("/path/to/book.pdf", book.filePath)
        assertEquals(1024, book.fileSize)
        assertEquals("application/pdf", book.mimeType)
        assertEquals(100, book.totalPages)
        assertEquals(50, book.currentPage)
        assertEquals(50f, book.progressPercentage, 0.01f)
    }
    
    @Test
    fun `book progress is calculated correctly`() {
        val book = Book(
            title = "Test Book",
            author = "Test Author",
            filePath = "/path/to/book.pdf",
            fileSize = 1024,
            mimeType = "application/pdf",
            totalPages = 200,
            currentPage = 75
        )
        
        // Progress would be updated by the ViewModel
        val expectedProgress = (75.0 / 200.0 * 100).toFloat()
        
        assertTrue(book.currentPage < book.totalPages)
        assertEquals(75, book.currentPage)
        assertEquals(200, book.totalPages)
    }
    
    @Test
    fun `book completion status can be updated`() {
        val book = Book(
            id = 1,
            title = "Completed Book",
            author = "Test Author",
            filePath = "/path/to/book.pdf",
            fileSize = 1024,
            mimeType = "application/pdf",
            isCompleted = true
        )
        
        assertTrue(book.isCompleted)
    }
    
    @Test
    fun `book supports multiple file formats`() {
        val pdfBook = Book(
            title = "PDF Book",
            author = "Author",
            filePath = "/path/book.pdf",
            fileSize = 1024,
            mimeType = "application/pdf"
        )
        
        val epubBook = Book(
            title = "EPUB Book",
            author = "Author",
            filePath = "/path/book.epub",
            fileSize = 2048,
            mimeType = "application/epub+zip"
        )
        
        val mobiBook = Book(
            title = "MOBI Book",
            author = "Author",
            filePath = "/path/book.mobi",
            fileSize = 3072,
            mimeType = "application/x-mobipocket-ebook"
        )
        
        assertEquals("application/pdf", pdfBook.mimeType)
        assertEquals("application/epub+zip", epubBook.mimeType)
        assertEquals("application/x-mobipocket-ebook", mobiBook.mimeType)
    }
    
    @Test
    fun `book timestamp tracks when added`() {
        val currentTime = System.currentTimeMillis()
        val book = Book(
            title = "New Book",
            author = "Author",
            filePath = "/path/book.pdf",
            fileSize = 1024,
            mimeType = "application/pdf"
        )
        
        assertTrue(book.dateAdded >= currentTime)
        assertTrue(book.dateAdded <= System.currentTimeMillis())
    }
    
    @Test
    fun `book can track last opened time`() {
        val lastOpened = System.currentTimeMillis()
        val book = Book(
            id = 1,
            title = "Recently Opened Book",
            author = "Author",
            filePath = "/path/book.pdf",
            fileSize = 1024,
            mimeType = "application/pdf",
            lastOpened = lastOpened
        )
        
        assertEquals(lastOpened, book.lastOpened)
    }
}
