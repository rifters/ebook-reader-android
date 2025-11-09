package com.rifters.ebookreader.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Entity(tableName = "tags")
@Parcelize
data class Tag(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val color: Int = -16776961, // Default blue color
    val dateCreated: Long = System.currentTimeMillis()
) : Parcelable
