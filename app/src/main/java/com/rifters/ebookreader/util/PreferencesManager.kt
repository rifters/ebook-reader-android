package com.rifters.ebookreader.util

import android.content.Context
import android.content.SharedPreferences
import com.rifters.ebookreader.model.ReadingPreferences
import com.rifters.ebookreader.model.ReadingTheme
import org.json.JSONObject
import com.rifters.ebookreader.model.LayoutMode
import com.rifters.ebookreader.util.TtsReplacementProcessor

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
            putString(KEY_LAYOUT_MODE, preferences.layoutMode.name)
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
        val layoutModeName = prefs.getString(KEY_LAYOUT_MODE, LayoutMode.SINGLE_COLUMN.name)
        val layoutMode = try {
            LayoutMode.valueOf(layoutModeName ?: LayoutMode.SINGLE_COLUMN.name)
        } catch (_: IllegalArgumentException) {
            LayoutMode.SINGLE_COLUMN
        }
        
        return ReadingPreferences(
            fontFamily = fontFamily,
            fontSize = fontSize,
            theme = theme,
            lineSpacing = lineSpacing,
            marginHorizontal = marginHorizontal,
            marginVertical = marginVertical,
            layoutMode = layoutMode
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
    
    // TTS preferences
    fun setTtsRate(rate: Float) {
        prefs.edit().putFloat(KEY_TTS_RATE, rate).apply()
    }
    
    fun getTtsRate(): Float {
        return prefs.getFloat(KEY_TTS_RATE, 1.0f)
    }
    
    fun setTtsPitch(pitch: Float) {
        prefs.edit().putFloat(KEY_TTS_PITCH, pitch).apply()
    }
    
    fun getTtsPitch(): Float {
        return prefs.getFloat(KEY_TTS_PITCH, 1.0f)
    }
    
    // TTS Replacements
    fun isTtsReplacementsEnabled(): Boolean {
        return prefs.getBoolean(KEY_TTS_REPLACEMENTS_ENABLED, true)
    }
    
    fun setTtsReplacementsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_TTS_REPLACEMENTS_ENABLED, enabled).apply()
    }
    
    fun getTtsReplacements(): String {
        val default = getDefaultTtsReplacements()
        val stored = prefs.getString(KEY_TTS_REPLACEMENTS, null) ?: return default

        return if (TtsReplacementProcessor.isValidReplacementsJson(stored)) {
            stored
        } else {
            android.util.Log.w("PreferencesManager", "Invalid TTS replacements JSON detected, resetting to default")
            prefs.edit().putString(KEY_TTS_REPLACEMENTS, default).apply()
            default
        }
    }
    
    fun setTtsReplacements(replacements: String) {
        prefs.edit().putString(KEY_TTS_REPLACEMENTS, replacements).apply()
    }
    
    private fun getDefaultTtsReplacements(): String {
        return try {
            val replacements = JSONObject().apply {
                put("*[()\\\"«»'\\\\[\\\\]“”‘’]", " ")
                put("*[?!:;–—―]", ". ")
                    put("“", "\"")
                    put("”", "\"")
                put("‘", "'")
                put("’", "'")
                put("it's", "it is")
                put("it’s", "it is")
                put("can't", "cannot")
                put("can’t", "cannot")
                put("won't", "will not")
                put("won’t", "will not")
                put("don't", "do not")
                put("don’t", "do not")
                put("I'm", "I am")
                put("I’m", "I am")
                put("you're", "you are")
                put("you’re", "you are")
                put("he's", "he is")
                put("he’s", "he is")
                put("she's", "she is")
                put("she’s", "she is")
                put("we're", "we are")
                put("we’re", "we are")
                put("they're", "they are")
                put("they’re", "they are")
                put("...", " [pause] ")
                put("…", " [pause] ")
            }
            replacements.toString()
        } catch (e: Exception) {
            // Fallback to a minimal valid JSON object if something goes wrong
            android.util.Log.e("PreferencesManager", "Failed to build default TTS replacements", e)
            "{}"
        }
    }
    
    companion object {
        private const val PREFS_NAME = "reading_preferences"
        private const val KEY_FONT_FAMILY = "font_family"
        private const val KEY_FONT_SIZE = "font_size"
        private const val KEY_THEME = "theme"
        private const val KEY_LINE_SPACING = "line_spacing"
        private const val KEY_MARGIN_HORIZONTAL = "margin_horizontal"
        private const val KEY_MARGIN_VERTICAL = "margin_vertical"
        private const val KEY_LAYOUT_MODE = "layout_mode"
        private const val KEY_SYNC_ENABLED = "sync_enabled"
        private const val KEY_AUTO_SYNC = "auto_sync"
        private const val KEY_LAST_SYNC = "last_sync_timestamp"
        private const val KEY_NIGHT_MODE = "night_mode_enabled"
        private const val KEY_TTS_RATE = "tts_rate"
        private const val KEY_TTS_PITCH = "tts_pitch"
        private const val KEY_TTS_REPLACEMENTS_ENABLED = "tts_replacements_enabled"
        private const val KEY_TTS_REPLACEMENTS = "tts_replacements"
    }
}
