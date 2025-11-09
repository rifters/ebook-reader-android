package com.rifters.ebookreader.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import com.rifters.ebookreader.Book

@Entity(
    tableName = "reading_list_items",
    primaryKeys = ["readingListId", "bookId"],
    foreignKeys = [
        ForeignKey(
            entity = ReadingList::class,
            parentColumns = ["id"],
            childColumns = ["readingListId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Book::class,
            parentColumns = ["id"],
            childColumns = ["bookId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("readingListId"), Index("bookId")]
)
data class ReadingListItem(
    val readingListId: Long,
    val bookId: Long,
    val orderIndex: Int = 0,
    val dateAdded: Long = System.currentTimeMillis()
)
