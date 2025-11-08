package com.rifters.ebookreader.util

import android.content.Context
import android.content.SharedPreferences
import com.rifters.ebookreader.model.ReadingPreferences
import com.rifters.ebookreader.model.ReadingTheme

class PreferencesManager(context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME, Context.MODE_PRIVATE
    )
    
    fun saveReadingPreferences(preferences: ReadingPreferences) {
        prefs.edit().apply {
            putString(KEY_FONT_FAMILY, preferences.fontFamily)
            putString(KEY_THEME, preferences.theme.name)
            putFloat(KEY_LINE_SPACING, preferences.lineSpacing)
            putInt(KEY_MARGIN_HORIZONTAL, preferences.marginHorizontal)
            putInt(KEY_MARGIN_VERTICAL, preferences.marginVertical)
            apply()
        }
    }
    
    fun getReadingPreferences(): ReadingPreferences {
        val fontFamily = prefs.getString(KEY_FONT_FAMILY, "sans-serif") ?: "sans-serif"
        val themeName = prefs.getString(KEY_THEME, ReadingTheme.LIGHT.name) ?: ReadingTheme.LIGHT.name
        val theme = try {
            ReadingTheme.valueOf(themeName)
        } catch (e: IllegalArgumentException) {
            ReadingTheme.LIGHT
        }
        val lineSpacing = prefs.getFloat(KEY_LINE_SPACING, 1.5f)
        val marginHorizontal = prefs.getInt(KEY_MARGIN_HORIZONTAL, 16)
        val marginVertical = prefs.getInt(KEY_MARGIN_VERTICAL, 16)
        
        return ReadingPreferences(
            fontFamily = fontFamily,
            theme = theme,
            lineSpacing = lineSpacing,
            marginHorizontal = marginHorizontal,
            marginVertical = marginVertical
        )
    }
    
    companion object {
        private const val PREFS_NAME = "reading_preferences"
        private const val KEY_FONT_FAMILY = "font_family"
        private const val KEY_THEME = "theme"
        private const val KEY_LINE_SPACING = "line_spacing"
        private const val KEY_MARGIN_HORIZONTAL = "margin_horizontal"
        private const val KEY_MARGIN_VERTICAL = "margin_vertical"
    }
}
