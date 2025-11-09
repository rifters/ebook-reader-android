package com.rifters.ebookreader.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Entity(tableName = "reading_goals")
@Parcelize
data class ReadingGoal(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val targetBooks: Int = 0,
    val targetPages: Int = 0,
    val targetMinutes: Int = 0,
    val startDate: Long = System.currentTimeMillis(),
    val endDate: Long = System.currentTimeMillis(),
    val booksRead: Int = 0,
    val pagesRead: Int = 0,
    val minutesRead: Int = 0,
    val isActive: Boolean = true
) : Parcelable
