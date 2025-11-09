package com.rifters.ebookreader.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.rifters.ebookreader.database.BookDatabase
import com.rifters.ebookreader.model.Tag
import com.rifters.ebookreader.model.BookTagCrossRef
import kotlinx.coroutines.launch

class TagViewModel(application: Application) : AndroidViewModel(application) {
    
    private val tagDao = BookDatabase.getDatabase(application).tagDao()
    
    val allTags: LiveData<List<Tag>> = tagDao.getAllTags()
    
    fun getTagsForBook(bookId: Long): LiveData<List<Tag>> {
        return tagDao.getTagsForBook(bookId)
    }
    
    fun insertTag(tag: Tag) {
        viewModelScope.launch {
            tagDao.insertTag(tag)
        }
    }
    
    fun updateTag(tag: Tag) {
        viewModelScope.launch {
            tagDao.updateTag(tag)
        }
    }
    
    fun deleteTag(tag: Tag) {
        viewModelScope.launch {
            tagDao.deleteTag(tag)
        }
    }
    
    fun addTagToBook(bookId: Long, tagId: Long) {
        viewModelScope.launch {
            tagDao.insertBookTagCrossRef(BookTagCrossRef(bookId, tagId))
        }
    }
    
    fun removeTagFromBook(bookId: Long, tagId: Long) {
        viewModelScope.launch {
            tagDao.deleteBookTagCrossRef(BookTagCrossRef(bookId, tagId))
        }
    }
    
    suspend fun isBookTagged(bookId: Long, tagId: Long): Boolean {
        return tagDao.isBookTagged(bookId, tagId) > 0
    }
}
