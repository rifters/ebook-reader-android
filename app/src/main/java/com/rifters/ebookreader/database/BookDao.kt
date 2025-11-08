package com.rifters.ebookreader.database

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.rifters.ebookreader.Book

@Dao
interface BookDao {
    
    @Query("SELECT * FROM books ORDER BY lastOpened DESC")
    fun getAllBooks(): LiveData<List<Book>>
    
    @Query("SELECT * FROM books ORDER BY title ASC")
    fun getBooksSortedByTitle(): LiveData<List<Book>>
    
    @Query("SELECT * FROM books ORDER BY author ASC")
    fun getBooksSortedByAuthor(): LiveData<List<Book>>
    
    @Query("SELECT * FROM books ORDER BY lastOpened DESC")
    fun getBooksSortedByRecentlyRead(): LiveData<List<Book>>
    
    @Query("SELECT * FROM books WHERE (title LIKE '%' || :query || '%' OR author LIKE '%' || :query || '%') ORDER BY lastOpened DESC")
    fun searchBooks(query: String): LiveData<List<Book>>
    
    @Query("SELECT * FROM books WHERE isCompleted = :isCompleted ORDER BY lastOpened DESC")
    fun getBooksByCompletionStatus(isCompleted: Boolean): LiveData<List<Book>>
    
    @Query("SELECT * FROM books WHERE id = :bookId")
    suspend fun getBookById(bookId: Long): Book?
    
    @Query("SELECT * FROM books WHERE filePath = :filePath")
    suspend fun getBookByPath(filePath: String): Book?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBook(book: Book): Long
    
    @Update
    suspend fun updateBook(book: Book)
    
    @Delete
    suspend fun deleteBook(book: Book)
    
    @Query("UPDATE books SET currentPage = :currentPage, progressPercentage = :progressPercentage, lastOpened = :lastOpened WHERE id = :bookId")
    suspend fun updateProgress(bookId: Long, currentPage: Int, progressPercentage: Float, lastOpened: Long)
    
    @Query("UPDATE books SET isCompleted = :isCompleted WHERE id = :bookId")
    suspend fun updateCompletionStatus(bookId: Long, isCompleted: Boolean)
    
    @Query("SELECT COUNT(*) FROM books")
    suspend fun getBookCount(): Int
    
    @Query("DELETE FROM books")
    suspend fun deleteAllBooks()
}
