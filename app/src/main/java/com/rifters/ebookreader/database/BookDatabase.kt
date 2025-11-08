package com.rifters.ebookreader.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.rifters.ebookreader.Book
import com.rifters.ebookreader.model.Bookmark
import com.rifters.ebookreader.model.BookCollectionCrossRef
import com.rifters.ebookreader.model.Collection
import com.rifters.ebookreader.model.SyncStatus

@Database(
    entities = [Book::class, Bookmark::class, Collection::class, BookCollectionCrossRef::class, SyncStatus::class], 
    version = 4, 
    exportSchema = false
)
abstract class BookDatabase : RoomDatabase() {
    
    abstract fun bookDao(): BookDao
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun collectionDao(): CollectionDao
    abstract fun syncDao(): SyncDao
    
    companion object {
        @Volatile
        private var INSTANCE: BookDatabase? = null
        
        fun getDatabase(context: Context): BookDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    BookDatabase::class.java,
                    "book_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
