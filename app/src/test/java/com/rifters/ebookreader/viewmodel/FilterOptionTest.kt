package com.rifters.ebookreader.viewmodel

import org.junit.Assert.*
import org.junit.Test

class FilterOptionTest {
    
    @Test
    fun `filter option enum has expected values`() {
        val values = FilterOption.values()
        
        assertEquals(3, values.size)
        assertTrue(values.contains(FilterOption.ALL))
        assertTrue(values.contains(FilterOption.COMPLETED))
        assertTrue(values.contains(FilterOption.NOT_COMPLETED))
    }
    
    @Test
    fun `filter option can be compared`() {
        val filter1 = FilterOption.ALL
        val filter2 = FilterOption.ALL
        val filter3 = FilterOption.COMPLETED
        
        assertEquals(filter1, filter2)
        assertNotEquals(filter1, filter3)
    }
    
    @Test
    fun `filter option toString returns expected values`() {
        assertEquals("ALL", FilterOption.ALL.toString())
        assertEquals("COMPLETED", FilterOption.COMPLETED.toString())
        assertEquals("NOT_COMPLETED", FilterOption.NOT_COMPLETED.toString())
    }
}
