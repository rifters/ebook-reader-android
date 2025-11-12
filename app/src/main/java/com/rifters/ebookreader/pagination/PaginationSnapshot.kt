package com.rifters.ebookreader.pagination

/**
 * Represents the current pagination position for the active book.
 */
data class PaginationSnapshot(
    val chapterIndex: Int,
    val chapterTitle: String?,
    val chapterPageIndex: Int,
    val chapterPageCount: Int,
    val bookPageIndex: Int,
    val bookPageCount: Int
) {
    val chapterDisplayIndex: Int get() = chapterIndex + 1
    val chapterDisplayPage: Int get() = chapterPageIndex + 1
    val bookDisplayPage: Int get() = bookPageIndex + 1
}
