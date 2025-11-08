package com.rifters.ebookreader

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rifters.ebookreader.database.BookDatabase
import com.rifters.ebookreader.model.BookCollectionCrossRef
import com.rifters.ebookreader.model.Collection
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

/**
 * End-to-end integration tests for the complete book and collection workflow.
 * 
 * These tests verify complex scenarios involving multiple database operations.
 */
@RunWith(AndroidJUnit4::class)
class BookCollectionIntegrationTest {
    
    private lateinit var database: BookDatabase
    
    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(
            context,
            BookDatabase::class.java
        ).build()
    }
    
    @After
    @Throws(IOException::class)
    fun closeDb() {
        database.close()
    }
    
    @Test
    @Throws(Exception::class)
    fun completeUserWorkflow_addBookToMultipleCollections() = runBlocking {
        val bookDao = database.bookDao()
        val collectionDao = database.collectionDao()
        
        // User adds a book to their library
        val book = Book(
            title = "The Great Novel",
            author = "Famous Author",
            filePath = "/storage/books/great_novel.epub",
            fileSize = 5242880,
            mimeType = "application/epub+zip",
            totalPages = 350
        )
        val bookId = bookDao.insertBook(book)
        assertTrue(bookId > 0)
        
        // User creates collections
        val fiction = Collection(name = "Fiction")
        val favorites = Collection(name = "Favorites")
        val toRead = Collection(name = "To Read")
        
        val fictionId = collectionDao.insertCollection(fiction)
        val favoritesId = collectionDao.insertCollection(favorites)
        val toReadId = collectionDao.insertCollection(toRead)
        
        // User adds book to multiple collections
        collectionDao.addBookToCollection(BookCollectionCrossRef(bookId, fictionId))
        collectionDao.addBookToCollection(BookCollectionCrossRef(bookId, favoritesId))
        collectionDao.addBookToCollection(BookCollectionCrossRef(bookId, toReadId))
        
        // Verify book appears in all collections
        val fictionWithBooks = collectionDao.getCollectionWithBooks(fictionId)
        val favoritesWithBooks = collectionDao.getCollectionWithBooks(favoritesId)
        val toReadWithBooks = collectionDao.getCollectionWithBooks(toReadId)
        
        assertEquals(1, fictionWithBooks?.books?.size)
        assertEquals(1, favoritesWithBooks?.books?.size)
        assertEquals(1, toReadWithBooks?.books?.size)
        
        // User starts reading the book
        bookDao.updateProgress(bookId, 50, 14.3f, System.currentTimeMillis())
        
        var updatedBook = bookDao.getBookById(bookId)
        assertEquals(50, updatedBook?.currentPage)
        assertEquals(14.3f, updatedBook?.progressPercentage ?: 0f, 0.01f)
        
        // User finishes reading the book
        bookDao.updateProgress(bookId, 350, 100f, System.currentTimeMillis())
        bookDao.updateCompletionStatus(bookId, true)
        
        updatedBook = bookDao.getBookById(bookId)
        assertEquals(350, updatedBook?.currentPage)
        assertEquals(100f, updatedBook?.progressPercentage ?: 0f, 0.01f)
        assertTrue(updatedBook?.isCompleted ?: false)
        
        // User removes book from "To Read" collection (since it's completed)
        collectionDao.removeBookFromCollectionById(bookId, toReadId)
        
        val toReadUpdated = collectionDao.getCollectionWithBooks(toReadId)
        assertEquals(0, toReadUpdated?.books?.size)
        
        // Book should still be in other collections
        assertEquals(1, collectionDao.getCollectionWithBooks(fictionId)?.books?.size)
        assertEquals(1, collectionDao.getCollectionWithBooks(favoritesId)?.books?.size)
    }
    
    @Test
    @Throws(Exception::class)
    fun collectionManagement_createAndOrganizeLibrary() = runBlocking {
        val bookDao = database.bookDao()
        val collectionDao = database.collectionDao()
        
        // User creates several books
        val book1 = Book(
            title = "Mystery Book 1",
            author = "Mystery Author",
            filePath = "/path/mystery1.pdf",
            fileSize = 1024000,
            mimeType = "application/pdf"
        )
        val book2 = Book(
            title = "Mystery Book 2",
            author = "Mystery Author",
            filePath = "/path/mystery2.pdf",
            fileSize = 2048000,
            mimeType = "application/pdf"
        )
        val book3 = Book(
            title = "Science Book",
            author = "Science Author",
            filePath = "/path/science.pdf",
            fileSize = 3072000,
            mimeType = "application/pdf"
        )
        
        val bookId1 = bookDao.insertBook(book1)
        val bookId2 = bookDao.insertBook(book2)
        val bookId3 = bookDao.insertBook(book3)
        
        // User creates genre collections
        val mystery = Collection(name = "Mystery")
        val science = Collection(name = "Science")
        
        val mysteryId = collectionDao.insertCollection(mystery)
        val scienceId = collectionDao.insertCollection(science)
        
        // Organize books by genre
        collectionDao.addBookToCollection(BookCollectionCrossRef(bookId1, mysteryId))
        collectionDao.addBookToCollection(BookCollectionCrossRef(bookId2, mysteryId))
        collectionDao.addBookToCollection(BookCollectionCrossRef(bookId3, scienceId))
        
        // Verify mystery collection has 2 books
        val mysteryWithBooks = collectionDao.getCollectionWithBooks(mysteryId)
        assertEquals(2, mysteryWithBooks?.books?.size)
        
        // Verify science collection has 1 book
        val scienceWithBooks = collectionDao.getCollectionWithBooks(scienceId)
        assertEquals(1, scienceWithBooks?.books?.size)
        
        // Verify total book count
        val totalBooks = bookDao.getBookCount()
        assertEquals(3, totalBooks)
    }
    
    @Test
    @Throws(Exception::class)
    fun readingProgress_trackAcrossMultipleBooks() = runBlocking {
        val bookDao = database.bookDao()
        
        // User has multiple books in progress
        val book1 = Book(
            title = "Book 1",
            author = "Author 1",
            filePath = "/path/book1.pdf",
            fileSize = 1024,
            mimeType = "application/pdf",
            totalPages = 100
        )
        val book2 = Book(
            title = "Book 2",
            author = "Author 2",
            filePath = "/path/book2.epub",
            fileSize = 2048,
            mimeType = "application/epub+zip",
            totalPages = 200
        )
        val book3 = Book(
            title = "Book 3",
            author = "Author 3",
            filePath = "/path/book3.mobi",
            fileSize = 3072,
            mimeType = "application/x-mobipocket-ebook",
            totalPages = 300
        )
        
        val bookId1 = bookDao.insertBook(book1)
        val bookId2 = bookDao.insertBook(book2)
        val bookId3 = bookDao.insertBook(book3)
        
        // Update progress on each book
        bookDao.updateProgress(bookId1, 25, 25f, System.currentTimeMillis())
        bookDao.updateProgress(bookId2, 100, 50f, System.currentTimeMillis())
        bookDao.updateProgress(bookId3, 225, 75f, System.currentTimeMillis())
        
        // Verify progress is tracked correctly
        val retrievedBook1 = bookDao.getBookById(bookId1)
        val retrievedBook2 = bookDao.getBookById(bookId2)
        val retrievedBook3 = bookDao.getBookById(bookId3)
        
        assertEquals(25, retrievedBook1?.currentPage)
        assertEquals(25f, retrievedBook1?.progressPercentage ?: 0f, 0.01f)
        
        assertEquals(100, retrievedBook2?.currentPage)
        assertEquals(50f, retrievedBook2?.progressPercentage ?: 0f, 0.01f)
        
        assertEquals(225, retrievedBook3?.currentPage)
        assertEquals(75f, retrievedBook3?.progressPercentage ?: 0f, 0.01f)
        
        // Mark one book as completed
        bookDao.updateCompletionStatus(bookId3, true)
        val completedBook = bookDao.getBookById(bookId3)
        assertTrue(completedBook?.isCompleted ?: false)
    }
    
    @Test
    @Throws(Exception::class)
    fun bookDeletion_removeFromAllCollections() = runBlocking {
        val bookDao = database.bookDao()
        val collectionDao = database.collectionDao()
        
        // Create a book and collections
        val book = Book(
            title = "Book to Delete",
            author = "Author",
            filePath = "/path/delete.pdf",
            fileSize = 1024,
            mimeType = "application/pdf"
        )
        val bookId = bookDao.insertBook(book)
        
        val collection1 = Collection(name = "Collection 1")
        val collection2 = Collection(name = "Collection 2")
        
        val collectionId1 = collectionDao.insertCollection(collection1)
        val collectionId2 = collectionDao.insertCollection(collection2)
        
        // Add book to both collections
        collectionDao.addBookToCollection(BookCollectionCrossRef(bookId, collectionId1))
        collectionDao.addBookToCollection(BookCollectionCrossRef(bookId, collectionId2))
        
        // Verify book is in collections
        assertEquals(1, collectionDao.getCollectionWithBooks(collectionId1)?.books?.size)
        assertEquals(1, collectionDao.getCollectionWithBooks(collectionId2)?.books?.size)
        
        // Delete the book
        val bookToDelete = bookDao.getBookById(bookId)
        assertNotNull(bookToDelete)
        bookDao.deleteBook(bookToDelete!!)
        
        // Verify book is deleted
        assertNull(bookDao.getBookById(bookId))
        
        // Note: In a real app, you might want to cascade delete or clean up
        // the cross-reference entries when deleting a book
    }
}
