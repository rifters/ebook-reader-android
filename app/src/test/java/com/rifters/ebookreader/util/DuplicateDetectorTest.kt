package com.rifters.ebookreader.util

import com.rifters.ebookreader.Book
import org.junit.Assert.*
import org.junit.Test

class DuplicateDetectorTest {
    
    @Test
    fun `findDuplicates detects exact title and author matches`() {
        val books = listOf(
            createBook(1, "The Great Gatsby", "F. Scott Fitzgerald"),
            createBook(2, "The Great Gatsby", "F. Scott Fitzgerald"),
            createBook(3, "1984", "George Orwell")
        )
        
        val duplicates = DuplicateDetector.findDuplicates(books, threshold = 0.8f)
        
        assertEquals(1, duplicates.size)
        assertEquals(2, duplicates[0].books.size)
        assertTrue(duplicates[0].books.any { it.id == 1L })
        assertTrue(duplicates[0].books.any { it.id == 2L })
    }
    
    @Test
    fun `findDuplicates detects similar titles`() {
        val books = listOf(
            createBook(1, "Harry Potter and the Philosopher's Stone", "J.K. Rowling"),
            createBook(2, "Harry Potter and the Sorcerer's Stone", "J.K. Rowling"),
            createBook(3, "The Hobbit", "J.R.R. Tolkien")
        )
        
        val duplicates = DuplicateDetector.findDuplicates(books, threshold = 0.7f)
        
        assertTrue(duplicates.isNotEmpty())
        assertTrue(duplicates[0].books.size >= 2)
    }
    
    @Test
    fun `findDuplicates returns empty list for unique books`() {
        val books = listOf(
            createBook(1, "1984", "George Orwell"),
            createBook(2, "Animal Farm", "George Orwell"),
            createBook(3, "The Hobbit", "J.R.R. Tolkien")
        )
        
        val duplicates = DuplicateDetector.findDuplicates(books, threshold = 0.8f)
        
        assertTrue(duplicates.isEmpty())
    }
    
    @Test
    fun `areSameFile detects same file size and name`() {
        val book1 = createBook(1, "The Great Book", "Author", fileSize = 1024000)
        val book2 = createBook(2, "The Great Book", "Author", fileSize = 1024000)
        
        assertTrue(DuplicateDetector.areSameFile(book1, book2))
    }
    
    @Test
    fun `areSameFile rejects different file sizes`() {
        val book1 = createBook(1, "The Great Book", "Author", fileSize = 1024000)
        val book2 = createBook(2, "The Great Book", "Author", fileSize = 2048000)
        
        assertFalse(DuplicateDetector.areSameFile(book1, book2))
    }
    
    @Test
    fun `findExactDuplicates finds same file paths`() {
        val books = listOf(
            createBook(1, "Book 1", "Author", filePath = "/path/to/book.pdf"),
            createBook(2, "Book 2", "Author", filePath = "/path/to/book.pdf"),
            createBook(3, "Book 3", "Author", filePath = "/path/to/other.pdf")
        )
        
        val duplicates = DuplicateDetector.findExactDuplicates(books)
        
        assertEquals(1, duplicates.size)
        assertEquals(2, duplicates[0].books.size)
    }
    
    private fun createBook(
        id: Long,
        title: String,
        author: String,
        filePath: String = "/path/to/file.pdf",
        fileSize: Long = 1000000
    ): Book {
        return Book(
            id = id,
            title = title,
            author = author,
            filePath = filePath,
            fileSize = fileSize,
            mimeType = "application/pdf"
        )
    }
}
