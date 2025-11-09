package com.rifters.ebookreader.util

import com.rifters.ebookreader.Book
import com.rifters.ebookreader.model.Collection
import java.util.Calendar

/**
 * Utility for creating and managing smart collections
 * Auto-organizes books by various criteria
 */
object SmartCollections {
    
    enum class SmartCollectionType {
        BY_AUTHOR,
        BY_GENRE,
        BY_YEAR,
        BY_LANGUAGE,
        BY_PUBLISHER,
        RECENTLY_ADDED,
        RECENTLY_READ,
        IN_PROGRESS,
        COMPLETED,
        UNREAD,
        FAVORITES // Books with 4+ stars
    }
    
    /**
     * Generate smart collections from a list of books
     */
    fun generateSmartCollections(
        books: List<Book>,
        types: Set<SmartCollectionType> = SmartCollectionType.values().toSet()
    ): Map<Collection, List<Book>> {
        val collections = mutableMapOf<Collection, List<Book>>()
        
        if (types.contains(SmartCollectionType.BY_AUTHOR)) {
            collections.putAll(groupByAuthor(books))
        }
        
        if (types.contains(SmartCollectionType.BY_GENRE)) {
            collections.putAll(groupByGenre(books))
        }
        
        if (types.contains(SmartCollectionType.BY_YEAR)) {
            collections.putAll(groupByYear(books))
        }
        
        if (types.contains(SmartCollectionType.BY_LANGUAGE)) {
            collections.putAll(groupByLanguage(books))
        }
        
        if (types.contains(SmartCollectionType.BY_PUBLISHER)) {
            collections.putAll(groupByPublisher(books))
        }
        
        if (types.contains(SmartCollectionType.RECENTLY_ADDED)) {
            getRecentlyAdded(books)?.let { collections[it.first] = it.second }
        }
        
        if (types.contains(SmartCollectionType.RECENTLY_READ)) {
            getRecentlyRead(books)?.let { collections[it.first] = it.second }
        }
        
        if (types.contains(SmartCollectionType.IN_PROGRESS)) {
            getInProgress(books)?.let { collections[it.first] = it.second }
        }
        
        if (types.contains(SmartCollectionType.COMPLETED)) {
            getCompleted(books)?.let { collections[it.first] = it.second }
        }
        
        if (types.contains(SmartCollectionType.UNREAD)) {
            getUnread(books)?.let { collections[it.first] = it.second }
        }
        
        if (types.contains(SmartCollectionType.FAVORITES)) {
            getFavorites(books)?.let { collections[it.first] = it.second }
        }
        
        return collections
    }
    
    private fun groupByAuthor(books: List<Book>): Map<Collection, List<Book>> {
        return books.groupBy { it.author }
            .filter { it.key.isNotBlank() && it.value.size >= 2 } // Only authors with 2+ books
            .mapKeys { 
                Collection(
                    name = "Author: ${it.key}",
                    dateCreated = System.currentTimeMillis()
                )
            }
    }
    
    private fun groupByGenre(books: List<Book>): Map<Collection, List<Book>> {
        return books
            .filter { !it.genre.isNullOrBlank() }
            .groupBy { it.genre!! }
            .filter { it.value.size >= 2 } // Only genres with 2+ books
            .mapKeys { 
                Collection(
                    name = "Genre: ${it.key}",
                    dateCreated = System.currentTimeMillis()
                )
            }
    }
    
    private fun groupByYear(books: List<Book>): Map<Collection, List<Book>> {
        return books
            .filter { it.publishedYear != null }
            .groupBy { it.publishedYear!! }
            .filter { it.value.size >= 2 }
            .mapKeys { 
                Collection(
                    name = "Year: ${it.key}",
                    dateCreated = System.currentTimeMillis()
                )
            }
    }
    
    private fun groupByLanguage(books: List<Book>): Map<Collection, List<Book>> {
        return books
            .filter { !it.language.isNullOrBlank() }
            .groupBy { it.language!! }
            .filter { it.value.size >= 2 }
            .mapKeys { 
                Collection(
                    name = "Language: ${it.key}",
                    dateCreated = System.currentTimeMillis()
                )
            }
    }
    
    private fun groupByPublisher(books: List<Book>): Map<Collection, List<Book>> {
        return books
            .filter { !it.publisher.isNullOrBlank() }
            .groupBy { it.publisher!! }
            .filter { it.value.size >= 3 } // Only publishers with 3+ books
            .mapKeys { 
                Collection(
                    name = "Publisher: ${it.key}",
                    dateCreated = System.currentTimeMillis()
                )
            }
    }
    
    private fun getRecentlyAdded(books: List<Book>): Pair<Collection, List<Book>>? {
        val thirtyDaysAgo = System.currentTimeMillis() - (30 * 24 * 60 * 60 * 1000L)
        val recentBooks = books
            .filter { it.dateAdded >= thirtyDaysAgo }
            .sortedByDescending { it.dateAdded }
        
        return if (recentBooks.isNotEmpty()) {
            Collection(
                name = "Recently Added",
                dateCreated = System.currentTimeMillis()
            ) to recentBooks
        } else null
    }
    
    private fun getRecentlyRead(books: List<Book>): Pair<Collection, List<Book>>? {
        val recentBooks = books
            .filter { it.lastOpened > 0 }
            .sortedByDescending { it.lastOpened }
            .take(20)
        
        return if (recentBooks.isNotEmpty()) {
            Collection(
                name = "Recently Read",
                dateCreated = System.currentTimeMillis()
            ) to recentBooks
        } else null
    }
    
    private fun getInProgress(books: List<Book>): Pair<Collection, List<Book>>? {
        val inProgressBooks = books
            .filter { it.progressPercentage > 0 && !it.isCompleted }
            .sortedByDescending { it.lastOpened }
        
        return if (inProgressBooks.isNotEmpty()) {
            Collection(
                name = "In Progress",
                dateCreated = System.currentTimeMillis()
            ) to inProgressBooks
        } else null
    }
    
    private fun getCompleted(books: List<Book>): Pair<Collection, List<Book>>? {
        val completedBooks = books
            .filter { it.isCompleted }
            .sortedByDescending { it.lastOpened }
        
        return if (completedBooks.isNotEmpty()) {
            Collection(
                name = "Completed",
                dateCreated = System.currentTimeMillis()
            ) to completedBooks
        } else null
    }
    
    private fun getUnread(books: List<Book>): Pair<Collection, List<Book>>? {
        val unreadBooks = books
            .filter { it.progressPercentage == 0f && it.lastOpened == 0L }
            .sortedByDescending { it.dateAdded }
        
        return if (unreadBooks.isNotEmpty()) {
            Collection(
                name = "Unread",
                dateCreated = System.currentTimeMillis()
            ) to unreadBooks
        } else null
    }
    
    private fun getFavorites(books: List<Book>): Pair<Collection, List<Book>>? {
        val favorites = books
            .filter { it.rating >= 4.0f }
            .sortedByDescending { it.rating }
        
        return if (favorites.isNotEmpty()) {
            Collection(
                name = "Favorites",
                dateCreated = System.currentTimeMillis()
            ) to favorites
        } else null
    }
    
    /**
     * Suggest a collection name for a book based on its metadata
     */
    fun suggestCollectionForBook(book: Book): String? {
        return when {
            !book.genre.isNullOrBlank() -> book.genre
            book.publishedYear != null -> "${book.publishedYear}s"
            book.author.isNotBlank() -> book.author
            else -> null
        }
    }
}
