package com.rifters.ebookreader

import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.rule.ActivityTestRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UI tests for ViewerActivity.
 * 
 * These tests verify the book viewing functionality.
 * Note: Some tests require actual book files to be available.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class ViewerActivityTest {
    
    @get:Rule
    val activityRule = ActivityTestRule(
        ViewerActivity::class.java,
        true,
        false  // Don't launch activity automatically
    )
    
    @Test
    fun viewerActivity_displaysCoreComponents() {
        // Launch activity with test intent
        val intent = Intent(ApplicationProvider.getApplicationContext(), ViewerActivity::class.java).apply {
            putExtra("book_id", 1L)
            putExtra("book_title", "Test Book")
            putExtra("book_path", "/test/path.pdf")
        }
        activityRule.launchActivity(intent)
        
        // Verify toolbar is displayed
        onView(withId(R.id.toolbar))
            .check(matches(isDisplayed()))
        
        // Verify content container is displayed
        onView(withId(R.id.contentContainer))
            .check(matches(isDisplayed()))
    }
    
    @Test
    fun viewerActivity_hasNavigationButtons() {
        // Launch activity with test intent
        val intent = Intent(ApplicationProvider.getApplicationContext(), ViewerActivity::class.java).apply {
            putExtra("book_id", 1L)
            putExtra("book_title", "Test Book")
            putExtra("book_path", "/test/path.pdf")
        }
        activityRule.launchActivity(intent)
        
        // Verify bottom navigation bar exists
        onView(withId(R.id.bottomAppBar))
            .check(matches(isDisplayed()))
        
        // Verify bookmark button exists
        onView(withId(R.id.btnBookmark))
            .check(matches(isDisplayed()))
    }
    
    @Test
    fun viewerActivity_displaysBookTitle() {
        // Launch activity with test intent
        val intent = Intent(ApplicationProvider.getApplicationContext(), ViewerActivity::class.java).apply {
            putExtra("book_id", 1L)
            putExtra("book_title", "Test Book Title")
            putExtra("book_path", "/test/path.pdf")
        }
        activityRule.launchActivity(intent)
        
        // Verify book title is displayed in toolbar
        onView(withText("Test Book Title"))
            .check(matches(isDisplayed()))
    }
    
    @Test
    fun viewerActivity_hasContentViews() {
        // Launch activity with test intent
        val intent = Intent(ApplicationProvider.getApplicationContext(), ViewerActivity::class.java).apply {
            putExtra("book_id", 1L)
            putExtra("book_title", "Test Book")
            putExtra("book_path", "/test/path.pdf")
        }
        activityRule.launchActivity(intent)
        
        // Note: The specific content view (PDF, EPUB, etc.) depends on the file type
        // We just verify that the content container exists
        onView(withId(R.id.contentContainer))
            .check(matches(isDisplayed()))
    }
    
    @Test
    fun viewerActivity_hasLoadingIndicator() {
        // Launch activity with test intent
        val intent = Intent(ApplicationProvider.getApplicationContext(), ViewerActivity::class.java).apply {
            putExtra("book_id", 1L)
            putExtra("book_title", "Test Book")
            putExtra("book_path", "/test/path.pdf")
        }
        activityRule.launchActivity(intent)
        
        // Verify loading progress bar exists
        // Note: We don't check if it's visible as it depends on load state
        onView(withId(R.id.loadingProgressBar))
            .check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
    }
}
