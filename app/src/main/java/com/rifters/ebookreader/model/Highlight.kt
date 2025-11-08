package com.rifters.ebookreader.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Entity(tableName = "highlights")
@Parcelize
data class Highlight(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val bookId: Long,
    val selectedText: String,
    val page: Int = 0,
    val position: Int = 0,
    val color: Int = -256, // Yellow by default (0xFFFFFF00)
    val note: String? = null,
    val timestamp: Long = System.currentTimeMillis()
) : Parcelable
