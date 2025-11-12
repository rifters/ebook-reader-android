package com.rifters.ebookreader.view

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.viewpager2.widget.ViewPager2

/**
 * Custom ViewPager2 that supports vertical orientation with page transformations.
 * Inspired by LibreraReader's VerticalViewPager implementation.
 */
class CustomViewPager2 @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : ViewPager2(context, attrs) {

    init {
        // Default to horizontal orientation
        orientation = ORIENTATION_HORIZONTAL
    }

    /**
     * Set vertical scrolling mode with page transformation
     */
    fun setVerticalMode() {
        orientation = ORIENTATION_VERTICAL
        setPageTransformer(VerticalPageTransformer())
    }

    /**
     * Set horizontal scrolling mode
     */
    fun setHorizontalMode() {
        orientation = ORIENTATION_HORIZONTAL
        setPageTransformer(null)
    }

    /**
     * Page transformer for smooth vertical scrolling animations
     */
    private class VerticalPageTransformer : PageTransformer {
        override fun transformPage(view: View, position: Float) {
            when {
                position < -1 -> {
                    // Page is way off-screen to the left
                    view.alpha = 0f
                }
                position <= 1 -> {
                    // Page is visible or moving
                    view.alpha = 1f
                    
                    // Apply parallax effect for smooth scrolling feel
                    view.translationX = view.width * -position
                    
                    val yPosition = position * view.height
                    view.translationY = yPosition
                }
                else -> {
                    // Page is way off-screen to the right
                    view.alpha = 0f
                }
            }
        }
    }

    /**
     * Enable smooth scrolling with custom duration
     */
    fun setScrollDuration(durationMs: Int) {
        try {
            val recyclerView = getChildAt(0)
            val touchSlopField = recyclerView.javaClass.getDeclaredField("mTouchSlop")
            touchSlopField.isAccessible = true
            val touchSlop = touchSlopField.get(recyclerView) as Int
            touchSlopField.set(recyclerView, touchSlop * 2)
        } catch (e: Exception) {
            // Ignore if reflection fails
        }
    }
}
