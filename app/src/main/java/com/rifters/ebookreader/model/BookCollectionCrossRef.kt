package com.rifters.ebookreader.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import com.rifters.ebookreader.Book

@Entity(
    tableName = "book_collection_cross_ref",
    primaryKeys = ["bookId", "collectionId"],
    foreignKeys = [
        ForeignKey(
            entity = Book::class,
            parentColumns = ["id"],
            childColumns = ["bookId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Collection::class,
            parentColumns = ["id"],
            childColumns = ["collectionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("bookId"), Index("collectionId")]
)
data class BookCollectionCrossRef(
    val bookId: Long,
    val collectionId: Long
)
