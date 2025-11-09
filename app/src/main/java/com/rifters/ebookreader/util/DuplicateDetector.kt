package com.rifters.ebookreader.util

import com.rifters.ebookreader.Book
import kotlin.math.min

/**
 * Utility for detecting duplicate books in the library
 */
object DuplicateDetector {
    
    data class DuplicateGroup(
        val books: List<Book>,
        val similarityScore: Float
    )
    
    /**
     * Find potential duplicate books based on title and author similarity
     * @param books List of books to check
     * @param threshold Similarity threshold (0.0 to 1.0), default 0.8
     * @return List of duplicate groups
     */
    fun findDuplicates(books: List<Book>, threshold: Float = 0.8f): List<DuplicateGroup> {
        val duplicates = mutableListOf<DuplicateGroup>()
        val processed = mutableSetOf<Long>()
        
        for (i in books.indices) {
            if (books[i].id in processed) continue
            
            val similar = mutableListOf<Book>()
            similar.add(books[i])
            
            for (j in (i + 1) until books.size) {
                if (books[j].id in processed) continue
                
                val score = calculateSimilarity(books[i], books[j])
                if (score >= threshold) {
                    similar.add(books[j])
                    processed.add(books[j].id)
                }
            }
            
            if (similar.size > 1) {
                val avgScore = similar.drop(1).map { 
                    calculateSimilarity(books[i], it) 
                }.average().toFloat()
                
                duplicates.add(DuplicateGroup(similar, avgScore))
                processed.add(books[i].id)
            }
        }
        
        return duplicates.sortedByDescending { it.similarityScore }
    }
    
    /**
     * Calculate similarity score between two books
     * Uses combination of title and author similarity
     */
    private fun calculateSimilarity(book1: Book, book2: Book): Float {
        val titleSimilarity = calculateStringSimilarity(
            book1.title.lowercase(),
            book2.title.lowercase()
        )
        
        val authorSimilarity = calculateStringSimilarity(
            book1.author.lowercase(),
            book2.author.lowercase()
        )
        
        // Weighted average: title is more important
        return (titleSimilarity * 0.7f + authorSimilarity * 0.3f)
    }
    
    /**
     * Calculate string similarity using Levenshtein distance
     * Returns normalized score between 0 and 1
     */
    private fun calculateStringSimilarity(str1: String, str2: String): Float {
        if (str1 == str2) return 1.0f
        if (str1.isEmpty() || str2.isEmpty()) return 0.0f
        
        val distance = levenshteinDistance(str1, str2)
        val maxLength = max(str1.length, str2.length)
        
        return 1.0f - (distance.toFloat() / maxLength)
    }
    
    /**
     * Calculate Levenshtein distance between two strings
     */
    private fun levenshteinDistance(str1: String, str2: String): Int {
        val m = str1.length
        val n = str2.length
        
        val dp = Array(m + 1) { IntArray(n + 1) }
        
        for (i in 0..m) {
            dp[i][0] = i
        }
        
        for (j in 0..n) {
            dp[0][j] = j
        }
        
        for (i in 1..m) {
            for (j in 1..n) {
                val cost = if (str1[i - 1] == str2[j - 1]) 0 else 1
                
                dp[i][j] = minOf(
                    dp[i - 1][j] + 1,      // deletion
                    dp[i][j - 1] + 1,      // insertion
                    dp[i - 1][j - 1] + cost // substitution
                )
            }
        }
        
        return dp[m][n]
    }
    
    private fun max(a: Int, b: Int): Int = if (a > b) a else b
    
    /**
     * Check if two books are likely the same file
     * (same file size and similar names)
     */
    fun areSameFile(book1: Book, book2: Book): Boolean {
        return book1.fileSize == book2.fileSize &&
               calculateStringSimilarity(book1.title.lowercase(), book2.title.lowercase()) > 0.9f
    }
    
    /**
     * Find exact duplicates (same file path)
     */
    fun findExactDuplicates(books: List<Book>): List<DuplicateGroup> {
        val groups = books.groupBy { it.filePath }
            .filter { it.value.size > 1 }
            .map { DuplicateGroup(it.value, 1.0f) }
        
        return groups
    }
}
