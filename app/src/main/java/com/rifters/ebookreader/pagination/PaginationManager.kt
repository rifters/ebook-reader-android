package com.rifters.ebookreader.pagination

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.rifters.ebookreader.model.TableOfContentsItem
import org.json.JSONObject
import kotlin.math.max

/**
 * Coordinates pagination metadata such as chapter page counts and current position.
 *
 * The manager caches known chapter page counts per book per layout signature, so pagination work can
 * be reused between sessions.
 */
class PaginationManager(context: Context) {

    private val cachePrefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private var cacheKey: CacheKey? = null
    private var totalChapters: Int = 0
    private val chapterPageCounts: MutableMap<Int, Int> = mutableMapOf()
    private var tableOfContents: List<TableOfContentsItem> = emptyList()
    private var lastSnapshot: PaginationSnapshot? = null

    fun initialize(bookIdentifier: String, key: PaginationPreferencesKey, chapterCount: Int) {
        val newKey = CacheKey(bookIdentifier, key.signature())
        if (cacheKey == newKey && totalChapters == chapterCount) {
            return
        }

        cacheKey = newKey
        totalChapters = chapterCount

        chapterPageCounts.clear()
        chapterPageCounts.putAll(loadCachedChapterCounts(newKey))
        lastSnapshot = null
    }

    fun updateTableOfContents(items: List<TableOfContentsItem>) {
        tableOfContents = items
    }

    fun updateChapterPageCount(chapterIndex: Int, pageCount: Int) {
        if (chapterIndex < 0) return
        val normalized = max(1, pageCount)

        val current = chapterPageCounts[chapterIndex]
        if (current == normalized) {
            return
        }

        chapterPageCounts[chapterIndex] = normalized
        persistCache()
    }

    fun updatePosition(chapterIndex: Int, pageIndex: Int, explicitTitle: String? = null): PaginationSnapshot {
        val chapterPageCount = chapterPageCounts[chapterIndex]
            ?: max(pageIndex + 1, 1)

        val pageOffset = computeChapterOffset(chapterIndex)
        val bookPageCount = computeTotalBookPages(pageOffset, chapterIndex, chapterPageCount)
        val bookPageIndex = pageOffset + pageIndex

        val snapshot = PaginationSnapshot(
            chapterIndex = chapterIndex,
            chapterTitle = explicitTitle ?: resolveChapterTitle(chapterIndex),
            chapterPageIndex = pageIndex,
            chapterPageCount = chapterPageCount,
            bookPageIndex = bookPageIndex,
            bookPageCount = bookPageCount
        )

        lastSnapshot = snapshot
        return snapshot
    }

    fun getLastSnapshot(): PaginationSnapshot? = lastSnapshot

    fun clear() {
        cacheKey = null
        totalChapters = 0
        chapterPageCounts.clear()
        lastSnapshot = null
    }

    private fun computeChapterOffset(chapterIndex: Int): Int {
        if (chapterIndex <= 0) return 0
        var offset = 0
        for (index in 0 until chapterIndex) {
            val pages = chapterPageCounts[index]
            if (pages != null && pages > 0) {
                offset += pages
            }
        }
        return offset
    }

    private fun computeTotalBookPages(
        pageOffset: Int,
        chapterIndex: Int,
        chapterPageCount: Int
    ): Int {
        return if (chapterPageCounts.size == totalChapters && totalChapters > 0 &&
            chapterPageCounts.values.all { it > 0 }
        ) {
            chapterPageCounts.values.sum()
        } else {
            val currentContribution = max(chapterPageCount, 1)
            max(pageOffset + currentContribution, pageOffset + 1)
        }
    }

    private fun resolveChapterTitle(chapterIndex: Int): String? {
        if (tableOfContents.isEmpty()) {
            return null
        }
        return tableOfContents.getOrNull(chapterIndex)?.title?.ifBlank { null }
    }

    private fun loadCachedChapterCounts(key: CacheKey): Map<Int, Int> {
        val raw = cachePrefs.getString(key.asString(), null) ?: return emptyMap()
        return try {
            val root = JSONObject(raw)
            val chaptersObj = root.optJSONObject("chapters") ?: return emptyMap()
            val map = mutableMapOf<Int, Int>()
            val iter = chaptersObj.keys()
            while (iter.hasNext()) {
                val chapterKey = iter.next()
                val index = chapterKey.toIntOrNull() ?: continue
                val value = chaptersObj.optInt(chapterKey, 0)
                if (value > 0) {
                    map[index] = value
                }
            }
            map
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse pagination cache", e)
            emptyMap()
        }
    }

    private fun persistCache() {
        val key = cacheKey ?: return
        try {
            val root = JSONObject()
            val chapters = JSONObject()
            chapterPageCounts.forEach { (chapter, pages) ->
                if (pages > 0) {
                    chapters.put(chapter.toString(), pages)
                }
            }
            root.put("chapters", chapters)
            root.put("chapterCount", totalChapters)

            cachePrefs.edit().putString(key.asString(), root.toString()).apply()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to write pagination cache", e)
        }
    }

    private data class CacheKey(
        val bookIdentifier: String,
        val layoutSignature: String
    ) {
        fun asString(): String = "$bookIdentifier::$layoutSignature"
    }

    companion object {
        private const val PREFS_NAME = "pagination_metadata"
        private const val TAG = "PaginationManager"
    }
}
