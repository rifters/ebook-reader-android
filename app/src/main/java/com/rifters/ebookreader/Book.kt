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
    val progressPercentage: Float = 0f,
    val rating: Float = 0f, // 0-5 star rating
    val genre: String? = null, // Book genre/category
    val publisher: String? = null,
    val publishedYear: Int? = null,
    val language: String? = null,
    val isbn: String? = null,
    // TTS position tracking
    val ttsPosition: Int = 0, // Character position in text content for TTS resume
    val ttsLastPlayed: Long = 0 // Timestamp of last TTS playback
) : Parcelable