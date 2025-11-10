package com.rifters.ebookreader.util

import android.text.Html

/**
 * Utility class for splitting text into readable chunks for TTS.
 * Based on LibreraReader's text splitting patterns.
 */
object TtsTextSplitter {
    
    // Maximum chunk size for TTS (characters)
    private const val MAX_CHUNK_SIZE = 4000
    
    /**
     * Extract plain text from HTML content with better formatting preservation.
     * Optionally apply TTS replacements.
     */
    fun extractTextFromHtml(
        html: String, 
        applyReplacements: Boolean = false,
        replacementsJson: String = "",
        replacementsEnabled: Boolean = true
    ): String {
        if (!html.contains("<")) {
            val text = html
            return if (applyReplacements && replacementsEnabled) {
                TtsReplacementProcessor.applyReplacements(text, replacementsJson, replacementsEnabled)
            } else {
                text
            }
        }
        
        // Pre-process HTML to improve text extraction
        var processedHtml = html
            // Add line breaks for block elements
            .replace(Regex("</(p|div|h[1-6]|li|br)>", RegexOption.IGNORE_CASE), "\n\n")
            .replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "\n")
            // Add spaces around inline elements
            .replace(Regex("</(span|a|strong|em|b|i)>", RegexOption.IGNORE_CASE), " ")
        
        // Convert HTML to plain text
        var text = Html.fromHtml(processedHtml, Html.FROM_HTML_MODE_LEGACY).toString()
        
        // Clean up excessive whitespace while preserving paragraph breaks
        text = text
            .replace(Regex("[ \t]+"), " ") // Multiple spaces to single space
            .replace(Regex("\n{3,}"), "\n\n") // Multiple newlines to double newline
            .trim()
        
        // Apply TTS replacements if requested
        if (applyReplacements && replacementsEnabled) {
            text = TtsReplacementProcessor.applyReplacements(text, replacementsJson, replacementsEnabled)
        }
        
        return text
    }
    
    /**
     * Split text into paragraphs for natural TTS reading with pauses.
     * Returns list of text chunks with their start positions.
     */
    fun splitIntoParagraphs(text: String): List<TextChunk> {
        val chunks = mutableListOf<TextChunk>()
        
        if (text.isEmpty()) {
            return chunks
        }
        
        // Split on paragraph boundaries (double newlines, or single newlines with significant indentation)
        val paragraphs = text.split(Regex("\n\n+"))
        
        var currentPosition = 0
        
        for (paragraph in paragraphs) {
            val trimmedParagraph = paragraph.trim()
            
            if (trimmedParagraph.isEmpty()) {
                currentPosition += paragraph.length + 2 // Account for split delimiter
                continue
            }
            
            // If paragraph is too long, split it further by sentences
            if (trimmedParagraph.length > MAX_CHUNK_SIZE) {
                val sentences = splitIntoSentences(trimmedParagraph)
                var sentencePosition = currentPosition
                
                for (sentence in sentences) {
                    if (sentence.isNotEmpty()) {
                        chunks.add(TextChunk(sentence, sentencePosition, ChunkType.SENTENCE))
                        sentencePosition += sentence.length
                    }
                }
            } else {
                chunks.add(TextChunk(trimmedParagraph, currentPosition, ChunkType.PARAGRAPH))
            }
            
            currentPosition += paragraph.length + 2 // Account for split delimiter
        }
        
        return chunks
    }
    
    /**
     * Split text into sentences for very long paragraphs.
     */
    private fun splitIntoSentences(text: String): List<String> {
        // Split on sentence boundaries: . ! ? followed by space or newline
        // But not on common abbreviations
        val sentences = mutableListOf<String>()
        val sentencePattern = Regex("([.!?]+)\\s+")
        
        var lastIndex = 0
        val matches = sentencePattern.findAll(text)
        
        for (match in matches) {
            val sentence = text.substring(lastIndex, match.range.last + 1).trim()
            if (sentence.isNotEmpty()) {
                sentences.add(sentence)
            }
            lastIndex = match.range.last + 1
        }
        
        // Add remaining text
        if (lastIndex < text.length) {
            val remaining = text.substring(lastIndex).trim()
            if (remaining.isNotEmpty()) {
                sentences.add(remaining)
            }
        }
        
        return sentences
    }
    
    /**
     * Find the chunk containing the specified position.
     */
    fun findChunkAtPosition(chunks: List<TextChunk>, position: Int): Int {
        for (i in chunks.indices) {
            val chunk = chunks[i]
            val endPosition = chunk.startPosition + chunk.text.length
            if (position >= chunk.startPosition && position < endPosition) {
                return i
            }
        }
        return 0 // Default to first chunk if not found
    }
    
    /**
     * Represents a chunk of text with its position and type.
     */
    data class TextChunk(
        val text: String,
        val startPosition: Int,
        val type: ChunkType
    )
    
    enum class ChunkType {
        PARAGRAPH,
        SENTENCE
    }
}
