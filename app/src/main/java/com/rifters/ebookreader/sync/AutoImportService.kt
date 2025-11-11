package com.rifters.ebookreader.sync

import android.content.Context
import android.util.Log
import com.rifters.ebookreader.Book
import com.rifters.ebookreader.database.BookDatabase
import java.io.File

/**
 * Helper that imports downloaded books into the app's Room database.
 * This is a small, synchronous helper that can be called from background workers.
 */
object AutoImportService {

    private const val TAG = "AutoImportService"

    suspend fun importFile(context: Context, absolutePath: String) {
        try {
            val db = BookDatabase.getDatabase(context)
            val dao = db.bookDao()

            val file = File(absolutePath)
            if (!file.exists()) return

            val existing = dao.getBookByPath(absolutePath)
            if (existing != null) {
                Log.i(TAG, "File already imported: $absolutePath")
                return
            }

            val title = file.nameWithoutExtension
            val book = Book(
                title = title,
                author = "",
                filePath = absolutePath,
                fileSize = file.length(),
                mimeType = "",
            )

            dao.insertBook(book)
            Log.i(TAG, "Imported book: $absolutePath")
        } catch (e: Exception) {
            Log.e(TAG, "Auto-import failed", e)
        }
    }
}
