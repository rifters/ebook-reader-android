package com.rifters.ebookreader.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.rifters.ebookreader.Book
import com.rifters.ebookreader.database.BookDatabase
import com.rifters.ebookreader.model.ReadingList
import com.rifters.ebookreader.model.ReadingListItem
import kotlinx.coroutines.launch

class ReadingListViewModel(application: Application) : AndroidViewModel(application) {
    
    private val listDao = BookDatabase.getDatabase(application).readingListDao()
    
    val allLists: LiveData<List<ReadingList>> = listDao.getAllReadingLists()
    
    fun getBooksInList(listId: Long): LiveData<List<Book>> {
        return listDao.getBooksInList(listId)
    }
    
    fun insertList(list: ReadingList) {
        viewModelScope.launch {
            listDao.insertReadingList(list)
        }
    }
    
    fun updateList(list: ReadingList) {
        viewModelScope.launch {
            listDao.updateReadingList(list)
        }
    }
    
    fun deleteList(list: ReadingList) {
        viewModelScope.launch {
            listDao.deleteReadingList(list)
        }
    }
    
    fun addBookToList(listId: Long, bookId: Long, orderIndex: Int = 0) {
        viewModelScope.launch {
            listDao.insertReadingListItem(
                ReadingListItem(listId, bookId, orderIndex)
            )
        }
    }
    
    fun removeBookFromList(listId: Long, bookId: Long) {
        viewModelScope.launch {
            listDao.deleteReadingListItem(
                ReadingListItem(listId, bookId)
            )
        }
    }
    
    fun clearList(listId: Long) {
        viewModelScope.launch {
            listDao.clearReadingList(listId)
        }
    }
    
    suspend fun isBookInList(bookId: Long, listId: Long): Boolean {
        return listDao.isBookInList(bookId, listId) > 0
    }
}
