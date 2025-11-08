package com.rifters.ebookreader.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import androidx.core.content.ContextCompat
import com.rifters.ebookreader.R
import kotlin.math.abs

object BookCoverGenerator {
    
    private val coverColors = listOf(
        "#6750A4", "#7D5260", "#006A6A", "#006E26", "#984061",
        "#5E4200", "#8E4585", "#006874", "#5A5A5A", "#8E2800"
    )
    
    /**
     * Generate a default book cover with title and author
     */
    fun generateDefaultCover(
        context: Context,
        title: String,
        author: String,
        width: Int = 400,
        height: Int = 600
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        
        // Choose color based on title hash for consistency
        val colorIndex = abs(title.hashCode()) % coverColors.size
        val backgroundColor = Color.parseColor(coverColors[colorIndex])
        
        // Draw background with gradient effect
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = backgroundColor
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        
        // Add a subtle darker overlay at bottom
        paint.color = Color.argb(40, 0, 0, 0)
        canvas.drawRect(0f, height * 0.6f, width.toFloat(), height.toFloat(), paint)
        
        // Draw decorative border
        paint.color = Color.WHITE
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 4f
        val margin = 20f
        canvas.drawRect(margin, margin, width - margin, height - margin, paint)
        
        // Draw inner border
        paint.strokeWidth = 2f
        canvas.drawRect(margin + 10f, margin + 10f, width - margin - 10f, height - margin - 10f, paint)
        
        // Setup text paint
        paint.style = Paint.Style.FILL
        paint.color = Color.WHITE
        paint.textAlign = Paint.Align.CENTER
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        
        // Draw title
        val titleMaxWidth = width - 80
        val titleSize = calculateTextSize(paint, title, titleMaxWidth, 72f, 28f)
        paint.textSize = titleSize
        
        val titleLines = wrapText(title, titleMaxWidth, paint)
        val titleStartY = height * 0.35f
        val lineHeight = paint.fontMetrics.let { abs(it.ascent) + abs(it.descent) + 10f }
        
        titleLines.forEachIndexed { index, line ->
            val y = titleStartY + (index * lineHeight)
            canvas.drawText(line, width / 2f, y, paint)
        }
        
        // Draw author
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 28f
        paint.color = Color.argb(220, 255, 255, 255)
        
        val authorLines = wrapText(author, titleMaxWidth, paint)
        val authorStartY = height * 0.7f
        
        authorLines.forEachIndexed { index, line ->
            val y = authorStartY + (index * lineHeight)
            canvas.drawText(line, width / 2f, y, paint)
        }
        
        // Draw book icon at top
        paint.textSize = 48f
        paint.color = Color.argb(180, 255, 255, 255)
        canvas.drawText("📖", width / 2f, height * 0.15f, paint)
        
        return bitmap
    }
    
    /**
     * Calculate optimal text size to fit within width
     */
    private fun calculateTextSize(
        paint: Paint,
        text: String,
        maxWidth: Int,
        maxSize: Float,
        minSize: Float
    ): Float {
        var size = maxSize
        paint.textSize = size
        
        while (size > minSize && paint.measureText(text) > maxWidth) {
            size -= 2f
            paint.textSize = size
        }
        
        return size
    }
    
    /**
     * Wrap text into multiple lines to fit within width
     */
    private fun wrapText(text: String, maxWidth: Int, paint: Paint): List<String> {
        val lines = mutableListOf<String>()
        val words = text.split(" ")
        var currentLine = ""
        
        for (word in words) {
            val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
            val width = paint.measureText(testLine)
            
            if (width > maxWidth && currentLine.isNotEmpty()) {
                lines.add(currentLine)
                currentLine = word
            } else {
                currentLine = testLine
            }
        }
        
        if (currentLine.isNotEmpty()) {
            lines.add(currentLine)
        }
        
        // Limit to 3 lines for title/author
        return lines.take(3).map { line ->
            if (lines.indexOf(line) == 2 && lines.size > 3) {
                line.take(20) + "..."
            } else {
                line
            }
        }
    }
    
    /**
     * Get a color based on string hash
     */
    fun getColorForBook(title: String): Int {
        val colorIndex = abs(title.hashCode()) % coverColors.size
        return Color.parseColor(coverColors[colorIndex])
    }
}
