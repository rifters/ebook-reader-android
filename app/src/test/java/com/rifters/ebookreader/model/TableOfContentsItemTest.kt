package com.rifters.ebookreader.model

import org.junit.Assert.*
import org.junit.Test

class TableOfContentsItemTest {
    
    @Test
    fun `toc item creation with valid data`() {
        val tocItem = TableOfContentsItem(
            title = "Chapter 1: Introduction",
            href = "chapter1.html",
            page = 0,
            level = 0
        )
        
        assertEquals("Chapter 1: Introduction", tocItem.title)
        assertEquals("chapter1.html", tocItem.href)
        assertEquals(0, tocItem.page)
        assertEquals(0, tocItem.level)
    }
    
    @Test
    fun `toc item with default values`() {
        val tocItem = TableOfContentsItem(
            title = "Chapter 2",
            href = "chapter2.html"
        )
        
        assertEquals("Chapter 2", tocItem.title)
        assertEquals("chapter2.html", tocItem.href)
        assertEquals(0, tocItem.page)
        assertEquals(0, tocItem.level)
    }
    
    @Test
    fun `toc item with nested level`() {
        val tocItem = TableOfContentsItem(
            title = "Section 1.1",
            href = "section1-1.html",
            page = 5,
            level = 1
        )
        
        assertEquals(1, tocItem.level)
    }
    
    @Test
    fun `toc items for multiple chapters`() {
        val chapter1 = TableOfContentsItem(
            title = "Chapter 1",
            href = "chapter1.html",
            page = 0
        )
        
        val chapter2 = TableOfContentsItem(
            title = "Chapter 2",
            href = "chapter2.html",
            page = 10
        )
        
        assertNotEquals(chapter1.title, chapter2.title)
        assertNotEquals(chapter1.href, chapter2.href)
        assertNotEquals(chapter1.page, chapter2.page)
    }
    
    @Test
    fun `toc item with empty title`() {
        val tocItem = TableOfContentsItem(
            title = "",
            href = "chapter.html"
        )
        
        assertEquals("", tocItem.title)
        assertEquals("chapter.html", tocItem.href)
    }
    
    @Test
    fun `toc item with special characters in title`() {
        val tocItem = TableOfContentsItem(
            title = "Chapter 1: The Beginning & The End",
            href = "chapter1.html"
        )
        
        assertEquals("Chapter 1: The Beginning & The End", tocItem.title)
    }
    
    @Test
    fun `toc item with anchor in href`() {
        val tocItem = TableOfContentsItem(
            title = "Section 2.1",
            href = "chapter2.html#section1"
        )
        
        assertEquals("chapter2.html#section1", tocItem.href)
    }
    
    @Test
    fun `toc item hierarchical structure`() {
        val mainChapter = TableOfContentsItem(
            title = "Part I",
            href = "part1.html",
            page = 0,
            level = 0
        )
        
        val subChapter = TableOfContentsItem(
            title = "Chapter 1",
            href = "chapter1.html",
            page = 5,
            level = 1
        )
        
        val subSection = TableOfContentsItem(
            title = "Section 1.1",
            href = "section1-1.html",
            page = 7,
            level = 2
        )
        
        assertTrue(mainChapter.level < subChapter.level)
        assertTrue(subChapter.level < subSection.level)
    }
}
