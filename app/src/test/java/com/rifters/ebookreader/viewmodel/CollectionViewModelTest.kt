package com.rifters.ebookreader.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.rifters.ebookreader.model.BookCollectionCrossRef
import com.rifters.ebookreader.model.Collection
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
 * Unit tests for CollectionViewModel.
 * 
 * Note: These tests focus on validating the ViewModel's data models and behavior.
 * Integration tests with actual Room database are covered in androidTest.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CollectionViewModelTest {
    
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
    fun `collection has correct fields`() {
        val collection = Collection(
            id = 1,
            name = "Fiction Books"
        )
        
        assertEquals(1, collection.id)
        assertEquals("Fiction Books", collection.name)
    }
    
    @Test
    fun `collection timestamp is set on creation`() {
        val currentTime = System.currentTimeMillis()
        val collection = Collection(
            name = "New Collection"
        )
        
        assertTrue(collection.dateCreated >= currentTime)
        assertTrue(collection.dateCreated <= System.currentTimeMillis())
    }
    
    @Test
    fun `collection can be created with minimal data`() {
        val collection = Collection(
            name = "Minimal Collection"
        )
        
        assertEquals(0, collection.id)
        assertEquals("Minimal Collection", collection.name)
        assertTrue(collection.dateCreated > 0)
    }
    
    @Test
    fun `bookCollectionCrossRef links book to collection`() {
        val bookId = 1L
        val collectionId = 5L
        val crossRef = BookCollectionCrossRef(
            bookId = bookId,
            collectionId = collectionId
        )
        
        assertEquals(bookId, crossRef.bookId)
        assertEquals(collectionId, crossRef.collectionId)
    }
    
    @Test
    fun `bookCollectionCrossRef represents many-to-many relationship`() {
        // A book can belong to multiple collections
        val bookId = 1L
        val crossRef1 = BookCollectionCrossRef(bookId, 10L)
        val crossRef2 = BookCollectionCrossRef(bookId, 20L)
        val crossRef3 = BookCollectionCrossRef(bookId, 30L)
        
        assertEquals(bookId, crossRef1.bookId)
        assertEquals(bookId, crossRef2.bookId)
        assertEquals(bookId, crossRef3.bookId)
        
        // Different collections
        assertNotEquals(crossRef1.collectionId, crossRef2.collectionId)
        assertNotEquals(crossRef2.collectionId, crossRef3.collectionId)
    }
    
    @Test
    fun `collection names can be updated`() {
        val collection = Collection(
            id = 1,
            name = "Old Name"
        )
        
        val updatedCollection = collection.copy(name = "New Name")
        
        assertEquals(1, updatedCollection.id)
        assertEquals("New Name", updatedCollection.name)
    }
    
    @Test
    fun `collection preserves dateCreated on update`() {
        val collection = Collection(
            id = 1,
            name = "Test Collection",
            dateCreated = 1234567890L
        )
        
        val updatedCollection = collection.copy(name = "Updated Collection")
        
        assertEquals("Updated Collection", updatedCollection.name)
        assertEquals(1234567890L, updatedCollection.dateCreated)
    }
    
    @Test
    fun `multiple collections can have unique names`() {
        val collection1 = Collection(name = "Fiction")
        val collection2 = Collection(name = "Non-Fiction")
        val collection3 = Collection(name = "Comics")
        
        assertNotEquals(collection1.name, collection2.name)
        assertNotEquals(collection2.name, collection3.name)
        assertNotEquals(collection1.name, collection3.name)
    }
}
