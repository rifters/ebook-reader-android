package com.rifters.ebookreader

import androidx.room.Entity
import androidx.room.PrimaryKey
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Entity(tableName = "books")
@Parcelize
data class Book(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val author: String,
    val filePath: String,
    val fileSize: Long,
    val mimeType: String,
    val dateAdded: Long = System.currentTimeMillis(),
    val lastOpened: Long = 0,
    val coverImagePath: String? = null,
    val totalPages: Int = 0,
    val currentPage: Int = 0,
    val isCompleted: Boolean = false,
    val progressPercentage: Float = 0f
) : Parcelable