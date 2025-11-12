package com.rifters.ebookreader.pagination

import com.rifters.ebookreader.model.LayoutMode
import com.rifters.ebookreader.model.ReadingPreferences

/**
 * Stable signature that captures the aspects of reading preferences that affect pagination.
 */
data class PaginationPreferencesKey(
    val fontFamily: String,
    val fontSize: Int,
    val lineSpacing: Float,
    val marginHorizontal: Int,
    val marginVertical: Int,
    val layoutMode: LayoutMode
) {
    fun signature(): String {
        return listOf(
            fontFamily,
            fontSize,
            "%.2f".format(lineSpacing),
            marginHorizontal,
            marginVertical,
            layoutMode.name
        ).joinToString(separator = "|")
    }

    companion object {
        fun from(preferences: ReadingPreferences): PaginationPreferencesKey {
            return PaginationPreferencesKey(
                fontFamily = preferences.fontFamily,
                fontSize = preferences.fontSize,
                lineSpacing = preferences.lineSpacing,
                marginHorizontal = preferences.marginHorizontal,
                marginVertical = preferences.marginVertical,
                layoutMode = preferences.layoutMode
            )
        }
    }
}
