package com.rifters.ebookreader.database

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.rifters.ebookreader.model.BookCollectionCrossRef
import com.rifters.ebookreader.model.Collection
import com.rifters.ebookreader.model.CollectionWithBooks

@Dao
interface CollectionDao {
    
    @Query("SELECT * FROM collections ORDER BY name ASC")
    fun getAllCollections(): LiveData<List<Collection>>
    
    @Query("SELECT * FROM collections WHERE id = :collectionId")
    suspend fun getCollectionById(collectionId: Long): Collection?
    
    @Transaction
    @Query("SELECT * FROM collections WHERE id = :collectionId")
    suspend fun getCollectionWithBooks(collectionId: Long): CollectionWithBooks?
    
    @Transaction
    @Query("SELECT * FROM collections ORDER BY name ASC")
    fun getAllCollectionsWithBooks(): LiveData<List<CollectionWithBooks>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCollection(collection: Collection): Long
    
    @Update
    suspend fun updateCollection(collection: Collection)
    
    @Delete
    suspend fun deleteCollection(collection: Collection)
    
    @Query("DELETE FROM collections WHERE id = :collectionId")
    suspend fun deleteCollectionById(collectionId: Long)
    
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addBookToCollection(crossRef: BookCollectionCrossRef)
    
    @Delete
    suspend fun removeBookFromCollection(crossRef: BookCollectionCrossRef)
    
    @Query("DELETE FROM book_collection_cross_ref WHERE bookId = :bookId AND collectionId = :collectionId")
    suspend fun removeBookFromCollectionById(bookId: Long, collectionId: Long)
    
    @Query("SELECT COUNT(*) FROM book_collection_cross_ref WHERE bookId = :bookId AND collectionId = :collectionId")
    suspend fun isBookInCollection(bookId: Long, collectionId: Long): Int
    
    @Query("SELECT COUNT(*) FROM book_collection_cross_ref WHERE collectionId = :collectionId")
    suspend fun getBookCountInCollection(collectionId: Long): Int
}
