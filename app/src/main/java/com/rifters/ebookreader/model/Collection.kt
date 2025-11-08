package com.rifters.ebookreader.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Entity(tableName = "collections")
@Parcelize
data class Collection(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val dateCreated: Long = System.currentTimeMillis()
) : Parcelable
