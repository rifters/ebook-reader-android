package com.rifters.ebookreader.viewmodel

import org.junit.Assert.*
import org.junit.Test

class SortOrderTest {
    
    @Test
    fun `sort order enum has expected values`() {
        val values = SortOrder.values()
        
        assertEquals(3, values.size)
        assertTrue(values.contains(SortOrder.RECENTLY_READ))
        assertTrue(values.contains(SortOrder.TITLE))
        assertTrue(values.contains(SortOrder.AUTHOR))
    }
    
    @Test
    fun `sort order can be compared`() {
        val sortOrder1 = SortOrder.TITLE
        val sortOrder2 = SortOrder.TITLE
        val sortOrder3 = SortOrder.AUTHOR
        
        assertEquals(sortOrder1, sortOrder2)
        assertNotEquals(sortOrder1, sortOrder3)
    }
    
    @Test
    fun `sort order toString returns expected values`() {
        assertEquals("RECENTLY_READ", SortOrder.RECENTLY_READ.toString())
        assertEquals("TITLE", SortOrder.TITLE.toString())
        assertEquals("AUTHOR", SortOrder.AUTHOR.toString())
    }
}
