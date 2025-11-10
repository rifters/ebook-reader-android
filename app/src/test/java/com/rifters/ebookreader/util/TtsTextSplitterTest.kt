package com.rifters.ebookreader.util

import org.junit.Test
import org.junit.Assert.*

class TtsTextSplitterTest {
    
    @Test
    fun testSplitIntoParagraphs_singleParagraph() {
        val text = "This is a single paragraph."
        val chunks = TtsTextSplitter.splitIntoParagraphs(text)
        
        assertEquals(1, chunks.size)
        assertEquals(text, chunks[0].text)
        assertEquals(0, chunks[0].startPosition)
        assertEquals(TtsTextSplitter.ChunkType.PARAGRAPH, chunks[0].type)
    }
    
    @Test
    fun testSplitIntoParagraphs_multipleParagraphs() {
        val text = "First paragraph.\n\nSecond paragraph.\n\nThird paragraph."
        val chunks = TtsTextSplitter.splitIntoParagraphs(text)
        
        assertEquals(3, chunks.size)
        assertEquals("First paragraph.", chunks[0].text)
        assertEquals("Second paragraph.", chunks[1].text)
        assertEquals("Third paragraph.", chunks[2].text)
    }
    
    @Test
    fun testSplitIntoParagraphs_emptyText() {
        val text = ""
        val chunks = TtsTextSplitter.splitIntoParagraphs(text)
        
        assertEquals(0, chunks.size)
    }
    
    @Test
    fun testSplitIntoParagraphs_longParagraph() {
        // Create a paragraph longer than MAX_CHUNK_SIZE (4000 chars)
        val longSentence = "This is a very long sentence. ".repeat(200) // ~6000 chars
        val chunks = TtsTextSplitter.splitIntoParagraphs(longSentence)
        
        // Should be split into multiple sentences
        assertTrue(chunks.size > 1)
        for (chunk in chunks) {
            assertEquals(TtsTextSplitter.ChunkType.SENTENCE, chunk.type)
        }
    }
    
    @Test
    fun testFindChunkAtPosition_firstChunk() {
        val text = "First paragraph.\n\nSecond paragraph.\n\nThird paragraph."
        val chunks = TtsTextSplitter.splitIntoParagraphs(text)
        
        val chunkIndex = TtsTextSplitter.findChunkAtPosition(chunks, 5)
        assertEquals(0, chunkIndex)
    }
    
    @Test
    fun testFindChunkAtPosition_middleChunk() {
        val text = "First paragraph.\n\nSecond paragraph.\n\nThird paragraph."
        val chunks = TtsTextSplitter.splitIntoParagraphs(text)
        
        // Position should be in second chunk
        val chunkIndex = TtsTextSplitter.findChunkAtPosition(chunks, 25)
        assertEquals(1, chunkIndex)
    }
    
    @Test
    fun testFindChunkAtPosition_outOfRange() {
        val text = "First paragraph.\n\nSecond paragraph."
        val chunks = TtsTextSplitter.splitIntoParagraphs(text)
        
        // Position beyond text length should return first chunk
        val chunkIndex = TtsTextSplitter.findChunkAtPosition(chunks, 10000)
        assertEquals(0, chunkIndex)
    }
    
    @Test
    fun testChunkPositionTracking() {
        val text = "First.\n\nSecond.\n\nThird."
        val chunks = TtsTextSplitter.splitIntoParagraphs(text)
        
        // Check that positions are tracked correctly
        assertEquals(0, chunks[0].startPosition)
        assertTrue(chunks[1].startPosition > chunks[0].startPosition)
        assertTrue(chunks[2].startPosition > chunks[1].startPosition)
    }
    
    @Test
    fun testExtractTextFromHtml_plainText() {
        val plainText = "This is plain text without any HTML tags"
        val result = TtsTextSplitter.extractTextFromHtml(plainText)
        
        assertEquals(plainText, result)
    }
    
    @Test
    fun testSplitIntoParagraphs_withWhitespace() {
        val text = "First paragraph.\n\n  \n\nSecond paragraph."
        val chunks = TtsTextSplitter.splitIntoParagraphs(text)
        
        // Should only have 2 chunks (empty lines ignored)
        assertEquals(2, chunks.size)
        assertEquals("First paragraph.", chunks[0].text)
        assertEquals("Second paragraph.", chunks[1].text)
    }
}
