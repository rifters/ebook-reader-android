package com.rifters.ebookreader.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Entity(tableName = "reading_lists")
@Parcelize
data class ReadingList(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val description: String? = null,
    val dateCreated: Long = System.currentTimeMillis(),
    val orderIndex: Int = 0
) : Parcelable
