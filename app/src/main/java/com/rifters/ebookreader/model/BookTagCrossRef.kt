package com.rifters.ebookreader.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import com.rifters.ebookreader.Book

@Entity(
    tableName = "book_tag_cross_ref",
    primaryKeys = ["bookId", "tagId"],
    foreignKeys = [
        ForeignKey(
            entity = Book::class,
            parentColumns = ["id"],
            childColumns = ["bookId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Tag::class,
            parentColumns = ["id"],
            childColumns = ["tagId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("bookId"), Index("tagId")]
)
data class BookTagCrossRef(
    val bookId: Long,
    val tagId: Long
)
