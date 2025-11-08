package com.rifters.ebookreader.util

import android.content.Context
import android.content.SharedPreferences
import com.rifters.ebookreader.model.ReadingPreferences
import com.rifters.ebookreader.model.ReadingTheme
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations

class PreferencesManagerTest {
    
    @Mock
    private lateinit var context: Context
    
    @Mock
    private lateinit var sharedPreferences: SharedPreferences
    
    @Mock
    private lateinit var editor: SharedPreferences.Editor
    
    private lateinit var preferencesManager: PreferencesManager
    
    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        
        `when`(context.getSharedPreferences(anyString(), anyInt())).thenReturn(sharedPreferences)
        `when`(sharedPreferences.edit()).thenReturn(editor)
        `when`(editor.putString(anyString(), anyString())).thenReturn(editor)
        `when`(editor.putFloat(anyString(), anyFloat())).thenReturn(editor)
        `when`(editor.putInt(anyString(), anyInt())).thenReturn(editor)
        
        preferencesManager = PreferencesManager(context)
    }
    
    @Test
    fun `save reading preferences stores all values`() {
        val preferences = ReadingPreferences(
            fontFamily = "serif",
            theme = ReadingTheme.DARK,
            lineSpacing = 2.0f,
            marginHorizontal = 24,
            marginVertical = 32
        )
        
        preferencesManager.saveReadingPreferences(preferences)
        
        verify(editor).putString("font_family", "serif")
        verify(editor).putString("theme", "DARK")
        verify(editor).putFloat("line_spacing", 2.0f)
        verify(editor).putInt("margin_horizontal", 24)
        verify(editor).putInt("margin_vertical", 32)
        verify(editor).apply()
    }
    
    @Test
    fun `get reading preferences returns default values when no preferences exist`() {
        `when`(sharedPreferences.getString("font_family", "sans-serif")).thenReturn("sans-serif")
        `when`(sharedPreferences.getString("theme", "LIGHT")).thenReturn("LIGHT")
        `when`(sharedPreferences.getFloat("line_spacing", 1.5f)).thenReturn(1.5f)
        `when`(sharedPreferences.getInt("margin_horizontal", 16)).thenReturn(16)
        `when`(sharedPreferences.getInt("margin_vertical", 16)).thenReturn(16)
        
        val preferences = preferencesManager.getReadingPreferences()
        
        assertEquals("sans-serif", preferences.fontFamily)
        assertEquals(ReadingTheme.LIGHT, preferences.theme)
        assertEquals(1.5f, preferences.lineSpacing, 0.01f)
        assertEquals(16, preferences.marginHorizontal)
        assertEquals(16, preferences.marginVertical)
    }
    
    @Test
    fun `get reading preferences returns stored values`() {
        `when`(sharedPreferences.getString("font_family", "sans-serif")).thenReturn("monospace")
        `when`(sharedPreferences.getString("theme", "LIGHT")).thenReturn("SEPIA")
        `when`(sharedPreferences.getFloat("line_spacing", 1.5f)).thenReturn(2.5f)
        `when`(sharedPreferences.getInt("margin_horizontal", 16)).thenReturn(32)
        `when`(sharedPreferences.getInt("margin_vertical", 16)).thenReturn(40)
        
        val preferences = preferencesManager.getReadingPreferences()
        
        assertEquals("monospace", preferences.fontFamily)
        assertEquals(ReadingTheme.SEPIA, preferences.theme)
        assertEquals(2.5f, preferences.lineSpacing, 0.01f)
        assertEquals(32, preferences.marginHorizontal)
        assertEquals(40, preferences.marginVertical)
    }
    
    @Test
    fun `get reading preferences handles invalid theme gracefully`() {
        `when`(sharedPreferences.getString("font_family", "sans-serif")).thenReturn("sans-serif")
        `when`(sharedPreferences.getString("theme", "LIGHT")).thenReturn("INVALID_THEME")
        `when`(sharedPreferences.getFloat("line_spacing", 1.5f)).thenReturn(1.5f)
        `when`(sharedPreferences.getInt("margin_horizontal", 16)).thenReturn(16)
        `when`(sharedPreferences.getInt("margin_vertical", 16)).thenReturn(16)
        
        val preferences = preferencesManager.getReadingPreferences()
        
        // Should fall back to LIGHT theme
        assertEquals(ReadingTheme.LIGHT, preferences.theme)
    }
}
