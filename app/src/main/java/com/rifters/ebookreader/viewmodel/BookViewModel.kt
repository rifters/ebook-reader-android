package com.rifters.ebookreader.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.rifters.ebookreader.Book
import com.rifters.ebookreader.database.BookDatabase
import com.rifters.ebookreader.database.BookDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BookViewModel(application: Application) : AndroidViewModel(application) {
    
    private val bookDao: BookDao
    val allBooks: LiveData<List<Book>>
    
    init {
        val database = BookDatabase.getDatabase(application)
        bookDao = database.bookDao()
        allBooks = bookDao.getAllBooks()
    }
    
    fun insertBook(book: Book) = viewModelScope.launch(Dispatchers.IO) {
        bookDao.insertBook(book)
    }
    
    fun updateBook(book: Book) = viewModelScope.launch(Dispatchers.IO) {
        bookDao.updateBook(book)
    }
    
    fun deleteBook(book: Book) = viewModelScope.launch(Dispatchers.IO) {
        bookDao.deleteBook(book)
    }
    
    fun updateProgress(bookId: Long, currentPage: Int, progressPercentage: Float) = 
        viewModelScope.launch(Dispatchers.IO) {
            val lastOpened = System.currentTimeMillis()
            bookDao.updateProgress(bookId, currentPage, progressPercentage, lastOpened)
        }
    
    fun updateCompletionStatus(bookId: Long, isCompleted: Boolean) = 
        viewModelScope.launch(Dispatchers.IO) {
            bookDao.updateCompletionStatus(bookId, isCompleted)
        }
    
    suspend fun getBookById(bookId: Long): Book? {
        return bookDao.getBookById(bookId)
    }
    
    suspend fun getBookByPath(filePath: String): Book? {
        return bookDao.getBookByPath(filePath)
    }
}
