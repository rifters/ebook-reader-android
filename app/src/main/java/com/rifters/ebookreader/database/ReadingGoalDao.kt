package com.rifters.ebookreader.database

import androidx.lifecycle.LiveData
import androidx.room.*
import com.rifters.ebookreader.model.ReadingGoal

@Dao
interface ReadingGoalDao {
    
    @Query("SELECT * FROM reading_goals ORDER BY startDate DESC")
    fun getAllGoals(): LiveData<List<ReadingGoal>>
    
    @Query("SELECT * FROM reading_goals WHERE isActive = 1 ORDER BY startDate DESC LIMIT 1")
    fun getActiveGoal(): LiveData<ReadingGoal?>
    
    @Query("SELECT * FROM reading_goals WHERE id = :goalId")
    suspend fun getGoalById(goalId: Long): ReadingGoal?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoal(goal: ReadingGoal): Long
    
    @Update
    suspend fun updateGoal(goal: ReadingGoal)
    
    @Delete
    suspend fun deleteGoal(goal: ReadingGoal)
    
    @Query("UPDATE reading_goals SET isActive = 0")
    suspend fun deactivateAllGoals()
    
    @Query("UPDATE reading_goals SET isActive = 1 WHERE id = :goalId")
    suspend fun activateGoal(goalId: Long)
    
    @Query("""
        UPDATE reading_goals 
        SET booksRead = booksRead + 1 
        WHERE isActive = 1 AND endDate >= :currentTime
    """)
    suspend fun incrementBooksRead(currentTime: Long = System.currentTimeMillis())
    
    @Query("""
        UPDATE reading_goals 
        SET pagesRead = pagesRead + :pages 
        WHERE isActive = 1 AND endDate >= :currentTime
    """)
    suspend fun addPagesRead(pages: Int, currentTime: Long = System.currentTimeMillis())
    
    @Query("""
        UPDATE reading_goals 
        SET minutesRead = minutesRead + :minutes 
        WHERE isActive = 1 AND endDate >= :currentTime
    """)
    suspend fun addMinutesRead(minutes: Int, currentTime: Long = System.currentTimeMillis())
}
