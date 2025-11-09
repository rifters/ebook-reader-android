package com.rifters.ebookreader.model

data class ReadingPreferences(
    val fontFamily: String = "sans-serif",
    val fontSize: Int = 16, // Default font size in sp
    val theme: ReadingTheme = ReadingTheme.LIGHT,
    val lineSpacing: Float = 1.5f,
    val marginHorizontal: Int = 16,
    val marginVertical: Int = 16,
    val layoutMode: LayoutMode = LayoutMode.SINGLE_COLUMN,
    val brightness: Float = -1f // -1 means use system brightness, 0.0-1.0 for custom
)

enum class LayoutMode(val displayName: String) {
    SINGLE_COLUMN("Single Column"),
    TWO_COLUMN("Two Columns"),
    CONTINUOUS_SCROLL("Continuous Scroll")
}

enum class ReadingTheme(val displayName: String, val backgroundColor: Int, val textColor: Int) {
    LIGHT("Light", 0xFFFAFAFA.toInt(), 0xDE000000.toInt()),
    DARK("Dark", 0xFF121212.toInt(), 0xFFFFFFFF.toInt()),
    SEPIA("Sepia", 0xFFF5DEB3.toInt(), 0xFF5C4033.toInt())
}
