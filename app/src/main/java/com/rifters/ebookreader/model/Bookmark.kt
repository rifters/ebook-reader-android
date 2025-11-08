package com.rifters.ebookreader.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Entity(tableName = "bookmarks")
@Parcelize
data class Bookmark(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val bookId: Long,
    val page: Int,
    val position: Int = 0,
    val note: String? = null,
    val timestamp: Long = System.currentTimeMillis()
) : Parcelable
