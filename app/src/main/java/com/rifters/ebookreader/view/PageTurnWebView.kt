package com.rifters.ebookreader.view

import android.content.Context
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.webkit.WebView
import androidx.core.view.GestureDetectorCompat
import kotlin.math.abs

/**
 * Custom WebView that supports page turning via swipe gestures.
 * Inspired by LibreraReader's gesture handling for book navigation.
 */
class PageTurnWebView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : WebView(context, attrs, defStyleAttr) {

    private var gestureDetector: GestureDetectorCompat
    private var pageNavigationEnabled = false
    private var onPageTurnListener: OnPageTurnListener? = null

    init {
        gestureDetector = GestureDetectorCompat(context, PageTurnGestureListener())
    }

    interface OnPageTurnListener {
        fun onNextPage()
        fun onPreviousPage()
        fun onSingleTap()
    }

    fun setPageNavigationEnabled(enabled: Boolean) {
        pageNavigationEnabled = enabled
    }

    fun setOnPageTurnListener(listener: OnPageTurnListener?) {
        onPageTurnListener = listener
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        // Let gesture detector handle the event first
        val handled = gestureDetector.onTouchEvent(event)
        
        // If page navigation is disabled, use default WebView behavior
        if (!pageNavigationEnabled) {
            return super.onTouchEvent(event)
        }
        
        // If gesture was handled, consume it
        // Otherwise, let WebView handle it (for continuous scroll mode)
        return if (handled) {
            true
        } else {
            super.onTouchEvent(event)
        }
    }

    private inner class PageTurnGestureListener : GestureDetector.SimpleOnGestureListener() {
        private val SWIPE_THRESHOLD = 100
        private val SWIPE_VELOCITY_THRESHOLD = 100

        override fun onDown(e: MotionEvent): Boolean {
            // Return true to indicate we want to handle gestures
            return true
        }

        override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
            if (pageNavigationEnabled) {
                onPageTurnListener?.onSingleTap()
                return true
            }
            return false
        }

        override fun onFling(
            e1: MotionEvent?,
            e2: MotionEvent,
            velocityX: Float,
            velocityY: Float
        ): Boolean {
            if (e1 == null || !pageNavigationEnabled) {
                return false
            }

            val diffX = e2.x - e1.x
            val diffY = e2.y - e1.y

            // Determine if this is a horizontal or vertical swipe
            if (abs(diffX) > abs(diffY)) {
                // Horizontal swipe
                if (abs(diffX) > SWIPE_THRESHOLD && abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                    if (diffX > 0) {
                        // Swipe right -> previous page
                        onPageTurnListener?.onPreviousPage()
                    } else {
                        // Swipe left -> next page
                        onPageTurnListener?.onNextPage()
                    }
                    return true
                }
            } else {
                // Vertical swipe
                if (abs(diffY) > SWIPE_THRESHOLD && abs(velocityY) > SWIPE_VELOCITY_THRESHOLD) {
                    if (diffY > 0) {
                        // Swipe down -> previous page
                        onPageTurnListener?.onPreviousPage()
                    } else {
                        // Swipe up -> next page
                        onPageTurnListener?.onNextPage()
                    }
                    return true
                }
            }
            return false
        }
    }
}
