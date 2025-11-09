package com.rifters.ebookreader.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.rifters.ebookreader.database.BookDatabase
import com.rifters.ebookreader.model.ReadingGoal
import kotlinx.coroutines.launch

class ReadingGoalViewModel(application: Application) : AndroidViewModel(application) {
    
    private val goalDao = BookDatabase.getDatabase(application).readingGoalDao()
    
    val allGoals: LiveData<List<ReadingGoal>> = goalDao.getAllGoals()
    val activeGoal: LiveData<ReadingGoal?> = goalDao.getActiveGoal()
    
    fun insertGoal(goal: ReadingGoal) {
        viewModelScope.launch {
            goalDao.insertGoal(goal)
        }
    }
    
    fun updateGoal(goal: ReadingGoal) {
        viewModelScope.launch {
            goalDao.updateGoal(goal)
        }
    }
    
    fun deleteGoal(goal: ReadingGoal) {
        viewModelScope.launch {
            goalDao.deleteGoal(goal)
        }
    }
    
    fun activateGoal(goalId: Long) {
        viewModelScope.launch {
            goalDao.deactivateAllGoals()
            goalDao.activateGoal(goalId)
        }
    }
    
    fun incrementBooksRead() {
        viewModelScope.launch {
            goalDao.incrementBooksRead()
        }
    }
    
    fun addPagesRead(pages: Int) {
        viewModelScope.launch {
            goalDao.addPagesRead(pages)
        }
    }
    
    fun addMinutesRead(minutes: Int) {
        viewModelScope.launch {
            goalDao.addMinutesRead(minutes)
        }
    }
}
