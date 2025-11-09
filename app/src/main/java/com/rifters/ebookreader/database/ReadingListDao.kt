package com.rifters.ebookreader.database

import androidx.lifecycle.LiveData
import androidx.room.*
import com.rifters.ebookreader.Book
import com.rifters.ebookreader.model.ReadingList
import com.rifters.ebookreader.model.ReadingListItem

@Dao
interface ReadingListDao {
    
    @Query("SELECT * FROM reading_lists ORDER BY orderIndex ASC, dateCreated DESC")
    fun getAllReadingLists(): LiveData<List<ReadingList>>
    
    @Query("SELECT * FROM reading_lists WHERE id = :listId")
    suspend fun getReadingListById(listId: Long): ReadingList?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReadingList(list: ReadingList): Long
    
    @Update
    suspend fun updateReadingList(list: ReadingList)
    
    @Delete
    suspend fun deleteReadingList(list: ReadingList)
    
    // Reading list items
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReadingListItem(item: ReadingListItem)
    
    @Delete
    suspend fun deleteReadingListItem(item: ReadingListItem)
    
    @Query("DELETE FROM reading_list_items WHERE readingListId = :listId")
    suspend fun clearReadingList(listId: Long)
    
    @Query("DELETE FROM reading_list_items WHERE bookId = :bookId")
    suspend fun removeBookFromAllLists(bookId: Long)
    
    @Query("""
        SELECT books.* FROM books
        INNER JOIN reading_list_items ON books.id = reading_list_items.bookId
        WHERE reading_list_items.readingListId = :listId
        ORDER BY reading_list_items.orderIndex ASC, reading_list_items.dateAdded DESC
    """)
    fun getBooksInList(listId: Long): LiveData<List<Book>>
    
    @Query("""
        SELECT COUNT(*) FROM reading_list_items
        WHERE readingListId = :listId
    """)
    suspend fun getListItemCount(listId: Long): Int
    
    @Query("""
        SELECT COUNT(*) FROM reading_list_items
        WHERE bookId = :bookId AND readingListId = :listId
    """)
    suspend fun isBookInList(bookId: Long, listId: Long): Int
}
