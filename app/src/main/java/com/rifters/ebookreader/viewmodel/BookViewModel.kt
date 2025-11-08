package com.rifters.ebookreader.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import com.rifters.ebookreader.Book
import com.rifters.ebookreader.database.BookDatabase
import com.rifters.ebookreader.database.BookDao
import com.rifters.ebookreader.sync.FirebaseSyncService
import com.rifters.ebookreader.sync.SyncRepository
import com.rifters.ebookreader.util.PreferencesManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

enum class SortOrder {
    RECENTLY_READ, TITLE, AUTHOR
}

enum class FilterOption {
    ALL, COMPLETED, NOT_COMPLETED
}

class BookViewModel(application: Application) : AndroidViewModel(application) {
    
    private val bookDao: BookDao
    private val prefsManager: PreferencesManager
    private val syncRepository: SyncRepository
    
    private val _sortOrder = MutableLiveData<SortOrder>(SortOrder.RECENTLY_READ)
    private val _filterOption = MutableLiveData<FilterOption>(FilterOption.ALL)
    private val _searchQuery = MutableLiveData<String>("")
    
    init {
        val database = BookDatabase.getDatabase(application)
        bookDao = database.bookDao()
        
        prefsManager = PreferencesManager(application)
        
        val syncService = FirebaseSyncService()
        syncRepository = SyncRepository(
            cloudSyncService = syncService,
            bookDao = bookDao,
            bookmarkDao = database.bookmarkDao(),
            syncDao = database.syncDao()
        )
    }
    
    val allBooks: LiveData<List<Book>> = _searchQuery.switchMap { query ->
        _filterOption.switchMap { filter ->
            _sortOrder.switchMap { sort ->
                // Determine which query to use based on search and filter state
                when {
                    query.isNotEmpty() && filter == FilterOption.COMPLETED -> 
                        bookDao.searchBooksWithFilter(query, true)
                    query.isNotEmpty() && filter == FilterOption.NOT_COMPLETED -> 
                        bookDao.searchBooksWithFilter(query, false)
                    query.isNotEmpty() -> 
                        bookDao.searchBooks(query)
                    filter == FilterOption.COMPLETED && sort == SortOrder.TITLE -> 
                        bookDao.getCompletedBooksSortedByTitle()
                    filter == FilterOption.COMPLETED && sort == SortOrder.AUTHOR -> 
                        bookDao.getCompletedBooksSortedByAuthor()
                    filter == FilterOption.COMPLETED -> 
                        bookDao.getBooksByCompletionStatus(true)
                    filter == FilterOption.NOT_COMPLETED && sort == SortOrder.TITLE -> 
                        bookDao.getNotCompletedBooksSortedByTitle()
                    filter == FilterOption.NOT_COMPLETED && sort == SortOrder.AUTHOR -> 
                        bookDao.getNotCompletedBooksSortedByAuthor()
                    filter == FilterOption.NOT_COMPLETED -> 
                        bookDao.getBooksByCompletionStatus(false)
                    sort == SortOrder.TITLE -> 
                        bookDao.getBooksSortedByTitle()
                    sort == SortOrder.AUTHOR -> 
                        bookDao.getBooksSortedByAuthor()
                    else -> 
                        bookDao.getBooksSortedByRecentlyRead()
                }
            }
        }
    }
    
    fun setSortOrder(sortOrder: SortOrder) {
        _sortOrder.value = sortOrder
    }
    
    fun setFilterOption(filterOption: FilterOption) {
        _filterOption.value = filterOption
    }
    
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }
    
    fun getSortOrder(): SortOrder = _sortOrder.value ?: SortOrder.RECENTLY_READ
    
    fun getFilterOption(): FilterOption = _filterOption.value ?: FilterOption.ALL
    
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
            
            // Mark for sync if sync is enabled
            if (prefsManager.isSyncEnabled() && prefsManager.isAutoSyncEnabled()) {
                syncRepository.markBookForSync(bookId)
            }
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
