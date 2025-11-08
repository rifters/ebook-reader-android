package com.rifters.ebookreader.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.rifters.ebookreader.database.BookDatabase
import com.rifters.ebookreader.database.CollectionDao
import com.rifters.ebookreader.model.BookCollectionCrossRef
import com.rifters.ebookreader.model.Collection
import com.rifters.ebookreader.model.CollectionWithBooks
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CollectionViewModel(application: Application) : AndroidViewModel(application) {
    
    private val collectionDao: CollectionDao
    val allCollections: LiveData<List<Collection>>
    val allCollectionsWithBooks: LiveData<List<CollectionWithBooks>>
    
    init {
        val database = BookDatabase.getDatabase(application)
        collectionDao = database.collectionDao()
        allCollections = collectionDao.getAllCollections()
        allCollectionsWithBooks = collectionDao.getAllCollectionsWithBooks()
    }
    
    fun insertCollection(collection: Collection) = viewModelScope.launch(Dispatchers.IO) {
        collectionDao.insertCollection(collection)
    }
    
    fun updateCollection(collection: Collection) = viewModelScope.launch(Dispatchers.IO) {
        collectionDao.updateCollection(collection)
    }
    
    fun deleteCollection(collection: Collection) = viewModelScope.launch(Dispatchers.IO) {
        collectionDao.deleteCollection(collection)
    }
    
    fun addBookToCollection(bookId: Long, collectionId: Long) = viewModelScope.launch(Dispatchers.IO) {
        collectionDao.addBookToCollection(BookCollectionCrossRef(bookId, collectionId))
    }
    
    fun removeBookFromCollection(bookId: Long, collectionId: Long) = viewModelScope.launch(Dispatchers.IO) {
        collectionDao.removeBookFromCollectionById(bookId, collectionId)
    }
    
    suspend fun getCollectionById(collectionId: Long): Collection? {
        return collectionDao.getCollectionById(collectionId)
    }
    
    suspend fun getCollectionWithBooks(collectionId: Long): CollectionWithBooks? {
        return collectionDao.getCollectionWithBooks(collectionId)
    }
    
    suspend fun isBookInCollection(bookId: Long, collectionId: Long): Boolean {
        return collectionDao.isBookInCollection(bookId, collectionId) > 0
    }
    
    suspend fun getBookCountInCollection(collectionId: Long): Int {
        return collectionDao.getBookCountInCollection(collectionId)
    }
}
