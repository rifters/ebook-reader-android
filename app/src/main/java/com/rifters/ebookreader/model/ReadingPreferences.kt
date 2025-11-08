package com.rifters.ebookreader.model

data class ReadingPreferences(
    val fontFamily: String = "sans-serif",
    val theme: ReadingTheme = ReadingTheme.LIGHT,
    val lineSpacing: Float = 1.5f,
    val marginHorizontal: Int = 16,
    val marginVertical: Int = 16
)

enum class ReadingTheme(val displayName: String, val backgroundColor: Int, val textColor: Int) {
    LIGHT("Light", 0xFFFAFAFA.toInt(), 0xDE000000.toInt()),
    DARK("Dark", 0xFF121212.toInt(), 0xFFFFFFFF.toInt()),
    SEPIA("Sepia", 0xFFF5DEB3.toInt(), 0xFF5C4033.toInt())
}
