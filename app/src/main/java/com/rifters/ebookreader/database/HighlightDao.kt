package com.rifters.ebookreader.database

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.rifters.ebookreader.model.Highlight

@Dao
interface HighlightDao {
    
    @Query("SELECT * FROM highlights WHERE bookId = :bookId ORDER BY page ASC, position ASC")
    fun getHighlightsForBook(bookId: Long): LiveData<List<Highlight>>
    
    @Query("SELECT * FROM highlights WHERE bookId = :bookId ORDER BY page ASC, position ASC")
    suspend fun getHighlightsForBookSync(bookId: Long): List<Highlight>
    
    @Query("SELECT * FROM highlights WHERE id = :highlightId")
    suspend fun getHighlightById(highlightId: Long): Highlight?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHighlight(highlight: Highlight): Long
    
    @Update
    suspend fun updateHighlight(highlight: Highlight)
    
    @Delete
    suspend fun deleteHighlight(highlight: Highlight)
    
    @Query("DELETE FROM highlights WHERE bookId = :bookId")
    suspend fun deleteHighlightsForBook(bookId: Long)
    
    @Query("SELECT COUNT(*) FROM highlights WHERE bookId = :bookId")
    suspend fun getHighlightCount(bookId: Long): Int
}
