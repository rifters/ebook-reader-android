package com.rifters.ebookreader.model

import android.graphics.Color
import org.junit.Assert.*
import org.junit.Test

class HighlightTest {
    
    @Test
    fun `highlight creation with valid data`() {
        val highlight = Highlight(
            id = 1,
            bookId = 10,
            selectedText = "This is important text",
            page = 5,
            position = 100,
            color = Color.YELLOW,
            note = "Important section"
        )
        
        assertEquals(1, highlight.id)
        assertEquals(10, highlight.bookId)
        assertEquals("This is important text", highlight.selectedText)
        assertEquals(5, highlight.page)
        assertEquals(100, highlight.position)
        assertEquals(Color.YELLOW, highlight.color)
        assertEquals("Important section", highlight.note)
    }
    
    @Test
    fun `highlight with null note`() {
        val highlight = Highlight(
            bookId = 10,
            selectedText = "Highlighted text",
            page = 5,
            position = 100,
            note = null
        )
        
        assertNull(highlight.note)
    }
    
    @Test
    fun `highlight default values`() {
        val highlight = Highlight(
            bookId = 10,
            selectedText = "Some text"
        )
        
        assertEquals(0, highlight.id)
        assertEquals(0, highlight.page)
        assertEquals(0, highlight.position)
        assertEquals(-256, highlight.color) // Yellow default
        assertNull(highlight.note)
    }
    
    @Test
    fun `highlight timestamp is set on creation`() {
        val currentTime = System.currentTimeMillis()
        val highlight = Highlight(
            bookId = 10,
            selectedText = "Text to highlight"
        )
        
        assertTrue(highlight.timestamp >= currentTime)
        assertTrue(highlight.timestamp <= System.currentTimeMillis())
    }
    
    @Test
    fun `multiple highlights for same book`() {
        val highlight1 = Highlight(
            id = 1,
            bookId = 10,
            selectedText = "First highlight",
            page = 5
        )
        val highlight2 = Highlight(
            id = 2,
            bookId = 10,
            selectedText = "Second highlight",
            page = 10
        )
        
        assertEquals(highlight1.bookId, highlight2.bookId)
        assertNotEquals(highlight1.selectedText, highlight2.selectedText)
        assertNotEquals(highlight1.page, highlight2.page)
    }
    
    @Test
    fun `highlight with different colors`() {
        val yellowHighlight = Highlight(
            bookId = 10,
            selectedText = "Yellow text",
            color = Color.YELLOW
        )
        
        val greenHighlight = Highlight(
            bookId = 10,
            selectedText = "Green text",
            color = Color.GREEN
        )
        
        assertEquals(Color.YELLOW, yellowHighlight.color)
        assertEquals(Color.GREEN, greenHighlight.color)
        assertNotEquals(yellowHighlight.color, greenHighlight.color)
    }
    
    @Test
    fun `highlight with empty selected text should still be created`() {
        val highlight = Highlight(
            bookId = 10,
            selectedText = ""
        )
        
        assertEquals("", highlight.selectedText)
        assertEquals(10, highlight.bookId)
    }
    
    @Test
    fun `highlight position tracking`() {
        val highlight = Highlight(
            bookId = 10,
            selectedText = "Text at position 500",
            page = 3,
            position = 500
        )
        
        assertEquals(3, highlight.page)
        assertEquals(500, highlight.position)
    }
}
