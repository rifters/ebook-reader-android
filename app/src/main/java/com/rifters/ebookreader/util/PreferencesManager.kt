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
            putInt(KEY_FONT_SIZE, preferences.fontSize)
            putString(KEY_THEME, preferences.theme.name)
            putFloat(KEY_LINE_SPACING, preferences.lineSpacing)
            putInt(KEY_MARGIN_HORIZONTAL, preferences.marginHorizontal)
            putInt(KEY_MARGIN_VERTICAL, preferences.marginVertical)
            apply()
        }
    }
    
    fun getReadingPreferences(): ReadingPreferences {
        val fontFamily = prefs.getString(KEY_FONT_FAMILY, "sans-serif") ?: "sans-serif"
        val fontSize = prefs.getInt(KEY_FONT_SIZE, 16)
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
            fontSize = fontSize,
            theme = theme,
            lineSpacing = lineSpacing,
            marginHorizontal = marginHorizontal,
            marginVertical = marginVertical
        )
    }
    
    // Sync preferences
    fun setSyncEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SYNC_ENABLED, enabled).apply()
    }
    
    fun isSyncEnabled(): Boolean {
        return prefs.getBoolean(KEY_SYNC_ENABLED, false)
    }
    
    fun setAutoSyncEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_SYNC, enabled).apply()
    }
    
    fun isAutoSyncEnabled(): Boolean {
        return prefs.getBoolean(KEY_AUTO_SYNC, true)
    }
    
    fun setLastSyncTimestamp(timestamp: Long) {
        prefs.edit().putLong(KEY_LAST_SYNC, timestamp).apply()
    }
    
    fun getLastSyncTimestamp(): Long {
        return prefs.getLong(KEY_LAST_SYNC, 0L)
    }
    
    // Night mode preference (separate from theme)
    fun setNightModeEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_NIGHT_MODE, enabled).apply()
    }
    
    fun isNightModeEnabled(): Boolean {
        return prefs.getBoolean(KEY_NIGHT_MODE, false)
    }
    
    companion object {
        private const val PREFS_NAME = "reading_preferences"
        private const val KEY_FONT_FAMILY = "font_family"
        private const val KEY_FONT_SIZE = "font_size"
        private const val KEY_THEME = "theme"
        private const val KEY_LINE_SPACING = "line_spacing"
        private const val KEY_MARGIN_HORIZONTAL = "margin_horizontal"
        private const val KEY_MARGIN_VERTICAL = "margin_vertical"
        private const val KEY_SYNC_ENABLED = "sync_enabled"
        private const val KEY_AUTO_SYNC = "auto_sync"
        private const val KEY_LAST_SYNC = "last_sync_timestamp"
        private const val KEY_NIGHT_MODE = "night_mode_enabled"
    }
}
