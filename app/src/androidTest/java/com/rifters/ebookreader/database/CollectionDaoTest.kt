package com.rifters.ebookreader.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rifters.ebookreader.Book
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
 * Integration tests for CollectionDao and Room database.
 * 
 * These tests verify collection and book-collection relationship operations.
 */
@RunWith(AndroidJUnit4::class)
class CollectionDaoTest {
    
    private lateinit var collectionDao: CollectionDao
    private lateinit var bookDao: BookDao
    private lateinit var database: BookDatabase
    
    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(
            context,
            BookDatabase::class.java
        ).build()
        collectionDao = database.collectionDao()
        bookDao = database.bookDao()
    }
    
    @After
    @Throws(IOException::class)
    fun closeDb() {
        database.close()
    }
    
    @Test
    @Throws(Exception::class)
    fun insertAndGetCollection() = runBlocking {
        val collection = Collection(
            name = "Fiction Books"
        )
        
        val collectionId = collectionDao.insertCollection(collection)
        assertTrue(collectionId > 0)
        
        val retrievedCollection = collectionDao.getCollectionById(collectionId)
        assertNotNull(retrievedCollection)
        assertEquals("Fiction Books", retrievedCollection?.name)
    }
    
    @Test
    @Throws(Exception::class)
    fun updateCollection() = runBlocking {
        val collection = Collection(
            name = "Original Name"
        )
        
        val collectionId = collectionDao.insertCollection(collection)
        var retrievedCollection = collectionDao.getCollectionById(collectionId)
        assertNotNull(retrievedCollection)
        
        val updatedCollection = retrievedCollection!!.copy(
            name = "Updated Name"
        )
        collectionDao.updateCollection(updatedCollection)
        
        retrievedCollection = collectionDao.getCollectionById(collectionId)
        assertEquals("Updated Name", retrievedCollection?.name)
    }
    
    @Test
    @Throws(Exception::class)
    fun deleteCollection() = runBlocking {
        val collection = Collection(
            name = "Collection to Delete"
        )
        
        val collectionId = collectionDao.insertCollection(collection)
        var retrievedCollection = collectionDao.getCollectionById(collectionId)
        assertNotNull(retrievedCollection)
        
        collectionDao.deleteCollection(retrievedCollection!!)
        
        retrievedCollection = collectionDao.getCollectionById(collectionId)
        assertNull(retrievedCollection)
    }
    
    @Test
    @Throws(Exception::class)
    fun addBookToCollection() = runBlocking {
        // Create a book
        val book = Book(
            title = "Test Book",
            author = "Test Author",
            filePath = "/path/book.pdf",
            fileSize = 1024,
            mimeType = "application/pdf"
        )
        val bookId = bookDao.insertBook(book)
        
        // Create a collection
        val collection = Collection(
            name = "My Collection"
        )
        val collectionId = collectionDao.insertCollection(collection)
        
        // Add book to collection
        val crossRef = BookCollectionCrossRef(bookId, collectionId)
        collectionDao.addBookToCollection(crossRef)
        
        // Verify the relationship
        val collectionWithBooks = collectionDao.getCollectionWithBooks(collectionId)
        assertNotNull(collectionWithBooks)
        assertEquals(1, collectionWithBooks?.books?.size)
        assertEquals("Test Book", collectionWithBooks?.books?.get(0)?.title)
    }
    
    @Test
    @Throws(Exception::class)
    fun removeBookFromCollection() = runBlocking {
        // Create a book
        val book = Book(
            title = "Test Book",
            author = "Test Author",
            filePath = "/path/book.pdf",
            fileSize = 1024,
            mimeType = "application/pdf"
        )
        val bookId = bookDao.insertBook(book)
        
        // Create a collection
        val collection = Collection(
            name = "My Collection"
        )
        val collectionId = collectionDao.insertCollection(collection)
        
        // Add book to collection
        val crossRef = BookCollectionCrossRef(bookId, collectionId)
        collectionDao.addBookToCollection(crossRef)
        
        // Verify book is in collection
        var collectionWithBooks = collectionDao.getCollectionWithBooks(collectionId)
        assertEquals(1, collectionWithBooks?.books?.size)
        
        // Remove book from collection
        collectionDao.removeBookFromCollection(crossRef)
        
        // Verify book is removed
        collectionWithBooks = collectionDao.getCollectionWithBooks(collectionId)
        assertEquals(0, collectionWithBooks?.books?.size)
    }
    
    @Test
    @Throws(Exception::class)
    fun bookCanBelongToMultipleCollections() = runBlocking {
        // Create a book
        val book = Book(
            title = "Popular Book",
            author = "Author",
            filePath = "/path/popular.pdf",
            fileSize = 1024,
            mimeType = "application/pdf"
        )
        val bookId = bookDao.insertBook(book)
        
        // Create multiple collections
        val collection1 = Collection(name = "Fiction")
        val collection2 = Collection(name = "Favorites")
        val collection3 = Collection(name = "Reading List")
        
        val collectionId1 = collectionDao.insertCollection(collection1)
        val collectionId2 = collectionDao.insertCollection(collection2)
        val collectionId3 = collectionDao.insertCollection(collection3)
        
        // Add same book to all collections
        collectionDao.addBookToCollection(BookCollectionCrossRef(bookId, collectionId1))
        collectionDao.addBookToCollection(BookCollectionCrossRef(bookId, collectionId2))
        collectionDao.addBookToCollection(BookCollectionCrossRef(bookId, collectionId3))
        
        // Verify book appears in all collections
        val collection1WithBooks = collectionDao.getCollectionWithBooks(collectionId1)
        val collection2WithBooks = collectionDao.getCollectionWithBooks(collectionId2)
        val collection3WithBooks = collectionDao.getCollectionWithBooks(collectionId3)
        
        assertEquals(1, collection1WithBooks?.books?.size)
        assertEquals(1, collection2WithBooks?.books?.size)
        assertEquals(1, collection3WithBooks?.books?.size)
        
        assertEquals("Popular Book", collection1WithBooks?.books?.get(0)?.title)
        assertEquals("Popular Book", collection2WithBooks?.books?.get(0)?.title)
        assertEquals("Popular Book", collection3WithBooks?.books?.get(0)?.title)
    }
    
    @Test
    @Throws(Exception::class)
    fun collectionCanContainMultipleBooks() = runBlocking {
        // Create multiple books
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
        val book3 = Book(
            title = "Book 3",
            author = "Author 3",
            filePath = "/path/book3.pdf",
            fileSize = 3072,
            mimeType = "application/pdf"
        )
        
        val bookId1 = bookDao.insertBook(book1)
        val bookId2 = bookDao.insertBook(book2)
        val bookId3 = bookDao.insertBook(book3)
        
        // Create a collection
        val collection = Collection(name = "My Library")
        val collectionId = collectionDao.insertCollection(collection)
        
        // Add all books to collection
        collectionDao.addBookToCollection(BookCollectionCrossRef(bookId1, collectionId))
        collectionDao.addBookToCollection(BookCollectionCrossRef(bookId2, collectionId))
        collectionDao.addBookToCollection(BookCollectionCrossRef(bookId3, collectionId))
        
        // Verify all books are in collection
        val collectionWithBooks = collectionDao.getCollectionWithBooks(collectionId)
        assertNotNull(collectionWithBooks)
        assertEquals(3, collectionWithBooks?.books?.size)
        
        val bookTitles = collectionWithBooks?.books?.map { it.title } ?: emptyList()
        assertTrue(bookTitles.contains("Book 1"))
        assertTrue(bookTitles.contains("Book 2"))
        assertTrue(bookTitles.contains("Book 3"))
    }
}
