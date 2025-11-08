package com.rifters.ebookreader.model

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation
import com.rifters.ebookreader.Book

data class CollectionWithBooks(
    @Embedded val collection: Collection,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = BookCollectionCrossRef::class,
            parentColumn = "collectionId",
            entityColumn = "bookId"
        )
    )
    val books: List<Book>
)

data class BookWithCollections(
    @Embedded val book: Book,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = BookCollectionCrossRef::class,
            parentColumn = "bookId",
            entityColumn = "collectionId"
        )
    )
    val collections: List<Collection>
)
