package com.rifters.ebookreader.util

import com.rifters.ebookreader.Book
import org.junit.Assert.*
import org.junit.Test

class SmartCollectionsTest {
    
    @Test
    fun `generateSmartCollections groups by author`() {
        val books = listOf(
            createBook(1, "1984", "George Orwell"),
            createBook(2, "Animal Farm", "George Orwell"),
            createBook(3, "The Hobbit", "J.R.R. Tolkien"),
            createBook(4, "The Lord of the Rings", "J.R.R. Tolkien")
        )
        
        val collections = SmartCollections.generateSmartCollections(
            books,
            setOf(SmartCollections.SmartCollectionType.BY_AUTHOR)
        )
        
        assertTrue(collections.isNotEmpty())
        assertTrue(collections.keys.any { it.name.contains("George Orwell") })
        assertTrue(collections.keys.any { it.name.contains("J.R.R. Tolkien") })
    }
    
    @Test
    fun `generateSmartCollections groups by genre`() {
        val books = listOf(
            createBook(1, "Book 1", "Author 1", genre = "Fantasy"),
            createBook(2, "Book 2", "Author 2", genre = "Fantasy"),
            createBook(3, "Book 3", "Author 3", genre = "Science Fiction"),
            createBook(4, "Book 4", "Author 4", genre = "Science Fiction")
        )
        
        val collections = SmartCollections.generateSmartCollections(
            books,
            setOf(SmartCollections.SmartCollectionType.BY_GENRE)
        )
        
        assertTrue(collections.isNotEmpty())
        assertTrue(collections.keys.any { it.name.contains("Fantasy") })
        assertTrue(collections.keys.any { it.name.contains("Science Fiction") })
    }
    
    @Test
    fun `generateSmartCollections creates in progress collection`() {
        val books = listOf(
            createBook(1, "Book 1", "Author 1", progressPercentage = 50f),
            createBook(2, "Book 2", "Author 2", progressPercentage = 75f),
            createBook(3, "Book 3", "Author 3", progressPercentage = 0f)
        )
        
        val collections = SmartCollections.generateSmartCollections(
            books,
            setOf(SmartCollections.SmartCollectionType.IN_PROGRESS)
        )
        
        assertTrue(collections.isNotEmpty())
        val inProgress = collections.entries.first()
        assertEquals("In Progress", inProgress.key.name)
        assertEquals(2, inProgress.value.size)
    }
    
    @Test
    fun `generateSmartCollections creates completed collection`() {
        val books = listOf(
            createBook(1, "Book 1", "Author 1", isCompleted = true),
            createBook(2, "Book 2", "Author 2", isCompleted = true),
            createBook(3, "Book 3", "Author 3", isCompleted = false)
        )
        
        val collections = SmartCollections.generateSmartCollections(
            books,
            setOf(SmartCollections.SmartCollectionType.COMPLETED)
        )
        
        assertTrue(collections.isNotEmpty())
        val completed = collections.entries.first()
        assertEquals("Completed", completed.key.name)
        assertEquals(2, completed.value.size)
    }
    
    @Test
    fun `generateSmartCollections creates favorites collection`() {
        val books = listOf(
            createBook(1, "Book 1", "Author 1", rating = 5.0f),
            createBook(2, "Book 2", "Author 2", rating = 4.5f),
            createBook(3, "Book 3", "Author 3", rating = 3.0f)
        )
        
        val collections = SmartCollections.generateSmartCollections(
            books,
            setOf(SmartCollections.SmartCollectionType.FAVORITES)
        )
        
        assertTrue(collections.isNotEmpty())
        val favorites = collections.entries.first()
        assertEquals("Favorites", favorites.key.name)
        assertEquals(2, favorites.value.size)
    }
    
    @Test
    fun `generateSmartCollections creates unread collection`() {
        val books = listOf(
            createBook(1, "Book 1", "Author 1", progressPercentage = 0f, lastOpened = 0),
            createBook(2, "Book 2", "Author 2", progressPercentage = 0f, lastOpened = 0),
            createBook(3, "Book 3", "Author 3", progressPercentage = 50f, lastOpened = 1000)
        )
        
        val collections = SmartCollections.generateSmartCollections(
            books,
            setOf(SmartCollections.SmartCollectionType.UNREAD)
        )
        
        assertTrue(collections.isNotEmpty())
        val unread = collections.entries.first()
        assertEquals("Unread", unread.key.name)
        assertEquals(2, unread.value.size)
    }
    
    @Test
    fun `suggestCollectionForBook suggests genre first`() {
        val book = createBook(1, "Book", "Author", genre = "Fantasy", publishedYear = 2020)
        val suggestion = SmartCollections.suggestCollectionForBook(book)
        assertEquals("Fantasy", suggestion)
    }
    
    @Test
    fun `suggestCollectionForBook suggests year if no genre`() {
        val book = createBook(1, "Book", "Author", genre = null, publishedYear = 2020)
        val suggestion = SmartCollections.suggestCollectionForBook(book)
        assertEquals("2020s", suggestion)
    }
    
    @Test
    fun `suggestCollectionForBook suggests author if no genre or year`() {
        val book = createBook(1, "Book", "George Orwell", genre = null, publishedYear = null)
        val suggestion = SmartCollections.suggestCollectionForBook(book)
        assertEquals("George Orwell", suggestion)
    }
    
    private fun createBook(
        id: Long,
        title: String,
        author: String,
        genre: String? = null,
        publishedYear: Int? = null,
        progressPercentage: Float = 0f,
        isCompleted: Boolean = false,
        rating: Float = 0f,
        lastOpened: Long = 0
    ): Book {
        return Book(
            id = id,
            title = title,
            author = author,
            filePath = "/path/to/file.pdf",
            fileSize = 1000000,
            mimeType = "application/pdf",
            genre = genre,
            publishedYear = publishedYear,
            progressPercentage = progressPercentage,
            isCompleted = isCompleted,
            rating = rating,
            lastOpened = lastOpened
        )
    }
}
