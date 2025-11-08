package com.rifters.ebookreader.model

import org.junit.Assert.*
import org.junit.Test

class CollectionTest {
    
    @Test
    fun `collection creation with valid data`() {
        val collection = Collection(
            id = 1,
            name = "Science Fiction",
            dateCreated = 1234567890L
        )
        
        assertEquals(1, collection.id)
        assertEquals("Science Fiction", collection.name)
        assertEquals(1234567890L, collection.dateCreated)
    }
    
    @Test
    fun `collection default values`() {
        val collection = Collection(
            name = "Fantasy"
        )
        
        assertEquals(0, collection.id)
        assertEquals("Fantasy", collection.name)
        assertTrue(collection.dateCreated > 0)
    }
    
    @Test
    fun `collection timestamp is set on creation`() {
        val currentTime = System.currentTimeMillis()
        val collection = Collection(
            name = "Mystery"
        )
        
        assertTrue(collection.dateCreated >= currentTime)
        assertTrue(collection.dateCreated <= System.currentTimeMillis())
    }
    
    @Test
    fun `collection with different names are not equal`() {
        val collection1 = Collection(id = 1, name = "Fiction")
        val collection2 = Collection(id = 1, name = "Non-Fiction")
        
        assertNotEquals(collection1, collection2)
    }
    
    @Test
    fun `collection copy creates new instance`() {
        val original = Collection(
            id = 1,
            name = "Original",
            dateCreated = 1000L
        )
        
        val copy = original.copy(name = "Modified")
        
        assertEquals(original.id, copy.id)
        assertEquals("Modified", copy.name)
        assertEquals(original.dateCreated, copy.dateCreated)
        assertNotSame(original, copy)
    }
}
