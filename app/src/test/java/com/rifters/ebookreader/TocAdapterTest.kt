package com.rifters.ebookreader

import com.rifters.ebookreader.model.TableOfContentsItem
import org.junit.Assert.*
import org.junit.Test

class TocAdapterTest {
    
    private val diffCallback = TocAdapter.TocDiffCallback()
    
    @Test
    fun `areItemsTheSame returns true for same href`() {
        val item1 = TableOfContentsItem(
            title = "Chapter 1",
            href = "chapter1.html",
            page = 0
        )
        
        val item2 = TableOfContentsItem(
            title = "Chapter One",
            href = "chapter1.html",
            page = 5
        )
        
        assertTrue(diffCallback.areItemsTheSame(item1, item2))
    }
    
    @Test
    fun `areItemsTheSame returns false for different href`() {
        val item1 = TableOfContentsItem(
            title = "Chapter 1",
            href = "chapter1.html",
            page = 0
        )
        
        val item2 = TableOfContentsItem(
            title = "Chapter 1",
            href = "chapter2.html",
            page = 0
        )
        
        assertFalse(diffCallback.areItemsTheSame(item1, item2))
    }
    
    @Test
    fun `areContentsTheSame returns true for identical items`() {
        val item1 = TableOfContentsItem(
            title = "Chapter 1",
            href = "chapter1.html",
            page = 0,
            level = 0
        )
        
        val item2 = TableOfContentsItem(
            title = "Chapter 1",
            href = "chapter1.html",
            page = 0,
            level = 0
        )
        
        assertTrue(diffCallback.areContentsTheSame(item1, item2))
    }
    
    @Test
    fun `areContentsTheSame returns false for different title`() {
        val item1 = TableOfContentsItem(
            title = "Chapter 1",
            href = "chapter1.html",
            page = 0
        )
        
        val item2 = TableOfContentsItem(
            title = "Chapter One",
            href = "chapter1.html",
            page = 0
        )
        
        assertFalse(diffCallback.areContentsTheSame(item1, item2))
    }
    
    @Test
    fun `areContentsTheSame returns false for different page`() {
        val item1 = TableOfContentsItem(
            title = "Chapter 1",
            href = "chapter1.html",
            page = 0
        )
        
        val item2 = TableOfContentsItem(
            title = "Chapter 1",
            href = "chapter1.html",
            page = 5
        )
        
        assertFalse(diffCallback.areContentsTheSame(item1, item2))
    }
    
    @Test
    fun `areContentsTheSame returns false for different level`() {
        val item1 = TableOfContentsItem(
            title = "Chapter 1",
            href = "chapter1.html",
            page = 0,
            level = 0
        )
        
        val item2 = TableOfContentsItem(
            title = "Chapter 1",
            href = "chapter1.html",
            page = 0,
            level = 1
        )
        
        assertFalse(diffCallback.areContentsTheSame(item1, item2))
    }
}
