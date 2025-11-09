package com.rifters.ebookreader.model

import org.junit.Assert.*
import org.junit.Test

class ReadingPreferencesTest {
    
    @Test
    fun `default reading preferences values`() {
        val preferences = ReadingPreferences()
        
        assertEquals("sans-serif", preferences.fontFamily)
        assertEquals(16, preferences.fontSize)
        assertEquals(ReadingTheme.LIGHT, preferences.theme)
        assertEquals(1.5f, preferences.lineSpacing, 0.01f)
        assertEquals(16, preferences.marginHorizontal)
        assertEquals(16, preferences.marginVertical)
    }
    
    @Test
    fun `reading preferences with custom values`() {
        val preferences = ReadingPreferences(
            fontFamily = "serif",
            fontSize = 20,
            theme = ReadingTheme.DARK,
            lineSpacing = 2.0f,
            marginHorizontal = 24,
            marginVertical = 32
        )
        
        assertEquals("serif", preferences.fontFamily)
        assertEquals(20, preferences.fontSize)
        assertEquals(ReadingTheme.DARK, preferences.theme)
        assertEquals(2.0f, preferences.lineSpacing, 0.01f)
        assertEquals(24, preferences.marginHorizontal)
        assertEquals(32, preferences.marginVertical)
    }
    
    @Test
    fun `reading theme has correct display names`() {
        assertEquals("Light", ReadingTheme.LIGHT.displayName)
        assertEquals("Dark", ReadingTheme.DARK.displayName)
        assertEquals("Sepia", ReadingTheme.SEPIA.displayName)
    }
    
    @Test
    fun `reading theme light has correct colors`() {
        val theme = ReadingTheme.LIGHT
        
        assertEquals(0xFFFAFAFA.toInt(), theme.backgroundColor)
        assertEquals(0xDE000000.toInt(), theme.textColor)
    }
    
    @Test
    fun `reading theme dark has correct colors`() {
        val theme = ReadingTheme.DARK
        
        assertEquals(0xFF121212.toInt(), theme.backgroundColor)
        assertEquals(0xFFFFFFFF.toInt(), theme.textColor)
    }
    
    @Test
    fun `reading theme sepia has correct colors`() {
        val theme = ReadingTheme.SEPIA
        
        assertEquals(0xFFF5DEB3.toInt(), theme.backgroundColor)
        assertEquals(0xFF5C4033.toInt(), theme.textColor)
    }
    
    @Test
    fun `reading theme enum values`() {
        val themes = ReadingTheme.values()
        
        assertEquals(3, themes.size)
        assertTrue(themes.contains(ReadingTheme.LIGHT))
        assertTrue(themes.contains(ReadingTheme.DARK))
        assertTrue(themes.contains(ReadingTheme.SEPIA))
    }
}
