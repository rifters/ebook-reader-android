package com.rifters.ebookreader.model

import org.junit.Assert.*
import org.junit.Test

class BookCollectionCrossRefTest {
    
    @Test
    fun `cross reference creation with valid data`() {
        val crossRef = BookCollectionCrossRef(
            bookId = 1,
            collectionId = 2
        )
        
        assertEquals(1, crossRef.bookId)
        assertEquals(2, crossRef.collectionId)
    }
    
    @Test
    fun `cross reference equality check`() {
        val crossRef1 = BookCollectionCrossRef(bookId = 1, collectionId = 2)
        val crossRef2 = BookCollectionCrossRef(bookId = 1, collectionId = 2)
        val crossRef3 = BookCollectionCrossRef(bookId = 2, collectionId = 1)
        
        assertEquals(crossRef1, crossRef2)
        assertNotEquals(crossRef1, crossRef3)
    }
    
    @Test
    fun `cross reference with different book ids are not equal`() {
        val crossRef1 = BookCollectionCrossRef(bookId = 1, collectionId = 1)
        val crossRef2 = BookCollectionCrossRef(bookId = 2, collectionId = 1)
        
        assertNotEquals(crossRef1, crossRef2)
    }
    
    @Test
    fun `cross reference with different collection ids are not equal`() {
        val crossRef1 = BookCollectionCrossRef(bookId = 1, collectionId = 1)
        val crossRef2 = BookCollectionCrossRef(bookId = 1, collectionId = 2)
        
        assertNotEquals(crossRef1, crossRef2)
    }
}
