package com.rifters.ebookreader.database

import androidx.lifecycle.LiveData
import androidx.room.*
import com.rifters.ebookreader.model.Tag
import com.rifters.ebookreader.model.BookTagCrossRef

@Dao
interface TagDao {
    
    @Query("SELECT * FROM tags ORDER BY name ASC")
    fun getAllTags(): LiveData<List<Tag>>
    
    @Query("SELECT * FROM tags WHERE id = :tagId")
    suspend fun getTagById(tagId: Long): Tag?
    
    @Query("SELECT * FROM tags WHERE name = :name")
    suspend fun getTagByName(name: String): Tag?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTag(tag: Tag): Long
    
    @Update
    suspend fun updateTag(tag: Tag)
    
    @Delete
    suspend fun deleteTag(tag: Tag)
    
    @Query("DELETE FROM tags WHERE id = :tagId")
    suspend fun deleteTagById(tagId: Long)
    
    // Cross reference operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookTagCrossRef(crossRef: BookTagCrossRef)
    
    @Delete
    suspend fun deleteBookTagCrossRef(crossRef: BookTagCrossRef)
    
    @Query("DELETE FROM book_tag_cross_ref WHERE bookId = :bookId")
    suspend fun deleteBookTags(bookId: Long)
    
    @Query("DELETE FROM book_tag_cross_ref WHERE tagId = :tagId")
    suspend fun deleteTagFromAllBooks(tagId: Long)
    
    @Query("""
        SELECT tags.* FROM tags
        INNER JOIN book_tag_cross_ref ON tags.id = book_tag_cross_ref.tagId
        WHERE book_tag_cross_ref.bookId = :bookId
        ORDER BY tags.name ASC
    """)
    fun getTagsForBook(bookId: Long): LiveData<List<Tag>>
    
    @Query("""
        SELECT COUNT(*) FROM book_tag_cross_ref
        WHERE bookId = :bookId AND tagId = :tagId
    """)
    suspend fun isBookTagged(bookId: Long, tagId: Long): Int
}
