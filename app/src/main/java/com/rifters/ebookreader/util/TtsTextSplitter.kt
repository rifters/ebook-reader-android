package com.rifters.ebookreader.util

import android.text.Html

/**
 * Utility class for splitting text into readable chunks for TTS.
 * Based on LibreraReader's text splitting patterns.
 */
object TtsTextSplitter {
    
    // Maximum chunk size for TTS (characters)
    const val MAX_CHUNK_SIZE = 4000
    
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
            val chunkSegments = splitParagraphIntoChunks(trimmedParagraph)
            var segmentPosition = currentPosition
            chunkSegments.forEach { segment ->
                chunks.add(TextChunk(segment, segmentPosition, ChunkType.PARAGRAPH))
                segmentPosition += segment.length
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
     * Split a single paragraph into TTS-friendly chunks that respect the max length.
     */
    fun splitParagraphIntoChunks(paragraph: String): List<String> {
        val trimmed = paragraph.trim()
        if (trimmed.isEmpty()) {
            return emptyList()
        }

        if (trimmed.length <= MAX_CHUNK_SIZE) {
            return listOf(trimmed)
        }

        val sentences = splitIntoSentences(trimmed)
        if (sentences.isEmpty()) {
            return listOf(trimmed)
        }

        val chunks = mutableListOf<String>()
        val currentChunk = StringBuilder()

        for (sentence in sentences) {
            if (sentence.isEmpty()) continue

            val sentenceWithSpace = if (sentence.endsWith(" ")) sentence else "$sentence "
            if (currentChunk.isEmpty()) {
                currentChunk.append(sentenceWithSpace)
                continue
            }

            if (currentChunk.length + sentenceWithSpace.length <= MAX_CHUNK_SIZE) {
                currentChunk.append(sentenceWithSpace)
            } else {
                chunks.add(currentChunk.toString().trim())
                currentChunk.clear()
                currentChunk.append(sentenceWithSpace)
            }
        }

        if (currentChunk.isNotEmpty()) {
            chunks.add(currentChunk.toString().trim())
        }

        // Ensure no chunk exceeds the maximum size; fall back to word-based splitting if needed
        return chunks.flatMap { chunk ->
            if (chunk.length <= MAX_CHUNK_SIZE) {
                listOf(chunk)
            } else {
                splitByWords(chunk)
            }
        }
    }

    private fun splitByWords(text: String): List<String> {
        if (text.length <= MAX_CHUNK_SIZE) {
            return listOf(text)
        }

        val words = text.split(Regex("\\s+"))
        val chunks = mutableListOf<String>()
        val currentChunk = StringBuilder()

        for (word in words) {
            if (word.isEmpty()) continue
            val candidateLength = if (currentChunk.isEmpty()) word.length else currentChunk.length + 1 + word.length
            if (candidateLength > MAX_CHUNK_SIZE) {
                if (currentChunk.isNotEmpty()) {
                    chunks.add(currentChunk.toString())
                    currentChunk.clear()
                }
                if (word.length > MAX_CHUNK_SIZE) {
                    // Split extremely long word by characters
                    chunks.addAll(word.chunked(MAX_CHUNK_SIZE))
                } else {
                    currentChunk.append(word)
                }
            } else {
                if (currentChunk.isNotEmpty()) {
                    currentChunk.append(' ')
                }
                currentChunk.append(word)
            }
        }

        if (currentChunk.isNotEmpty()) {
            chunks.add(currentChunk.toString())
        }

        return chunks
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
