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
import com.rifters.ebookreader.model.Highlight
import com.rifters.ebookreader.model.Tag
import com.rifters.ebookreader.model.BookTagCrossRef
import com.rifters.ebookreader.model.ReadingGoal
import com.rifters.ebookreader.model.ReadingList
import com.rifters.ebookreader.model.ReadingListItem

@Database(
    entities = [
        Book::class, 
        Bookmark::class, 
        Collection::class, 
        BookCollectionCrossRef::class, 
        SyncStatus::class, 
        Highlight::class,
        Tag::class,
        BookTagCrossRef::class,
        ReadingGoal::class,
        ReadingList::class,
        ReadingListItem::class
    ], 
    version = 7, 
    exportSchema = false
)
abstract class BookDatabase : RoomDatabase() {
    
    abstract fun bookDao(): BookDao
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun collectionDao(): CollectionDao
    abstract fun syncDao(): SyncDao
    abstract fun highlightDao(): HighlightDao
    abstract fun tagDao(): TagDao
    abstract fun readingGoalDao(): ReadingGoalDao
    abstract fun readingListDao(): ReadingListDao
    
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
