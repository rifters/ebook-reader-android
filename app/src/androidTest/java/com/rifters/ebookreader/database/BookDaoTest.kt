package com.rifters.ebookreader.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rifters.ebookreader.Book
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

/**
 * Integration tests for BookDao and Room database.
 * 
 * These tests verify database operations with an in-memory database.
 */
@RunWith(AndroidJUnit4::class)
class BookDaoTest {
    
    private lateinit var bookDao: BookDao
    private lateinit var database: BookDatabase
    
    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(
            context,
            BookDatabase::class.java
        ).build()
        bookDao = database.bookDao()
    }
    
    @After
    @Throws(IOException::class)
    fun closeDb() {
        database.close()
    }
    
    @Test
    @Throws(Exception::class)
    fun insertAndGetBook() = runBlocking {
        val book = Book(
            title = "Test Book",
            author = "Test Author",
            filePath = "/path/to/book.pdf",
            fileSize = 1024,
            mimeType = "application/pdf"
        )
        
        val bookId = bookDao.insertBook(book)
        assertTrue(bookId > 0)
        
        val retrievedBook = bookDao.getBookById(bookId)
        assertNotNull(retrievedBook)
        assertEquals("Test Book", retrievedBook?.title)
        assertEquals("Test Author", retrievedBook?.author)
    }
    
    @Test
    @Throws(Exception::class)
    fun getBookByPath() = runBlocking {
        val book = Book(
            title = "Test Book",
            author = "Test Author",
            filePath = "/unique/path/book.pdf",
            fileSize = 1024,
            mimeType = "application/pdf"
        )
        
        bookDao.insertBook(book)
        
        val retrievedBook = bookDao.getBookByPath("/unique/path/book.pdf")
        assertNotNull(retrievedBook)
        assertEquals("Test Book", retrievedBook?.title)
    }
    
    @Test
    @Throws(Exception::class)
    fun updateBook() = runBlocking {
        val book = Book(
            title = "Original Title",
            author = "Original Author",
            filePath = "/path/book.pdf",
            fileSize = 1024,
            mimeType = "application/pdf"
        )
        
        val bookId = bookDao.insertBook(book)
        var retrievedBook = bookDao.getBookById(bookId)
        assertNotNull(retrievedBook)
        
        val updatedBook = retrievedBook!!.copy(
            title = "Updated Title",
            author = "Updated Author"
        )
        bookDao.updateBook(updatedBook)
        
        retrievedBook = bookDao.getBookById(bookId)
        assertEquals("Updated Title", retrievedBook?.title)
        assertEquals("Updated Author", retrievedBook?.author)
    }
    
    @Test
    @Throws(Exception::class)
    fun deleteBook() = runBlocking {
        val book = Book(
            title = "Book to Delete",
            author = "Author",
            filePath = "/path/delete.pdf",
            fileSize = 1024,
            mimeType = "application/pdf"
        )
        
        val bookId = bookDao.insertBook(book)
        var retrievedBook = bookDao.getBookById(bookId)
        assertNotNull(retrievedBook)
        
        bookDao.deleteBook(retrievedBook!!)
        
        retrievedBook = bookDao.getBookById(bookId)
        assertNull(retrievedBook)
    }
    
    @Test
    @Throws(Exception::class)
    fun updateProgress() = runBlocking {
        val book = Book(
            title = "Progress Book",
            author = "Author",
            filePath = "/path/progress.pdf",
            fileSize = 1024,
            mimeType = "application/pdf",
            totalPages = 100
        )
        
        val bookId = bookDao.insertBook(book)
        
        val currentPage = 50
        val progressPercentage = 50f
        val lastOpened = System.currentTimeMillis()
        
        bookDao.updateProgress(bookId, currentPage, progressPercentage, lastOpened)
        
        val retrievedBook = bookDao.getBookById(bookId)
        assertNotNull(retrievedBook)
        assertEquals(50, retrievedBook?.currentPage)
        assertEquals(50f, retrievedBook?.progressPercentage ?: 0f, 0.01f)
        assertEquals(lastOpened, retrievedBook?.lastOpened)
    }
    
    @Test
    @Throws(Exception::class)
    fun updateCompletionStatus() = runBlocking {
        val book = Book(
            title = "Book to Complete",
            author = "Author",
            filePath = "/path/complete.pdf",
            fileSize = 1024,
            mimeType = "application/pdf",
            isCompleted = false
        )
        
        val bookId = bookDao.insertBook(book)
        
        bookDao.updateCompletionStatus(bookId, true)
        
        val retrievedBook = bookDao.getBookById(bookId)
        assertNotNull(retrievedBook)
        assertTrue(retrievedBook?.isCompleted ?: false)
    }
    
    @Test
    @Throws(Exception::class)
    fun getBookCount() = runBlocking {
        val initialCount = bookDao.getBookCount()
        
        val book1 = Book(
            title = "Book 1",
            author = "Author 1",
            filePath = "/path/book1.pdf",
            fileSize = 1024,
            mimeType = "application/pdf"
        )
        val book2 = Book(
            title = "Book 2",
            author = "Author 2",
            filePath = "/path/book2.pdf",
            fileSize = 2048,
            mimeType = "application/pdf"
        )
        
        bookDao.insertBook(book1)
        bookDao.insertBook(book2)
        
        val finalCount = bookDao.getBookCount()
        assertEquals(initialCount + 2, finalCount)
    }
    
    @Test
    @Throws(Exception::class)
    fun deleteAllBooks() = runBlocking {
        val book1 = Book(
            title = "Book 1",
            author = "Author 1",
            filePath = "/path/book1.pdf",
            fileSize = 1024,
            mimeType = "application/pdf"
        )
        val book2 = Book(
            title = "Book 2",
            author = "Author 2",
            filePath = "/path/book2.pdf",
            fileSize = 2048,
            mimeType = "application/pdf"
        )
        
        bookDao.insertBook(book1)
        bookDao.insertBook(book2)
        
        var count = bookDao.getBookCount()
        assertTrue(count >= 2)
        
        bookDao.deleteAllBooks()
        
        count = bookDao.getBookCount()
        assertEquals(0, count)
    }
    
    @Test
    @Throws(Exception::class)
    fun insertMultipleBookFormats() = runBlocking {
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
        
        val pdfId = bookDao.insertBook(pdfBook)
        val epubId = bookDao.insertBook(epubBook)
        val mobiId = bookDao.insertBook(mobiBook)
        
        assertTrue(pdfId > 0)
        assertTrue(epubId > 0)
        assertTrue(mobiId > 0)
        
        val retrievedPdf = bookDao.getBookById(pdfId)
        val retrievedEpub = bookDao.getBookById(epubId)
        val retrievedMobi = bookDao.getBookById(mobiId)
        
        assertEquals("application/pdf", retrievedPdf?.mimeType)
        assertEquals("application/epub+zip", retrievedEpub?.mimeType)
        assertEquals("application/x-mobipocket-ebook", retrievedMobi?.mimeType)
    }
}
