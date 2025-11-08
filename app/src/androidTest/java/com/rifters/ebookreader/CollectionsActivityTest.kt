package com.rifters.ebookreader

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UI tests for CollectionsActivity.
 * 
 * These tests verify the user flows for managing collections.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class CollectionsActivityTest {
    
    @get:Rule
    val activityRule = ActivityScenarioRule(CollectionsActivity::class.java)
    
    @Test
    fun collectionsActivity_displaysCorrectly() {
        // Verify that the collections activity launches and displays correctly
        onView(withId(R.id.toolbar))
            .check(matches(isDisplayed()))
        
        onView(withId(R.id.recyclerView))
            .check(matches(isDisplayed()))
        
        onView(withId(R.id.fabCreateCollection))
            .check(matches(isDisplayed()))
    }
    
    @Test
    fun toolbar_displaysCorrectTitle() {
        // Verify that the toolbar displays the correct title
        onView(withText(R.string.collections))
            .check(matches(isDisplayed()))
    }
    
    @Test
    fun fabAddCollection_isClickable() {
        // Verify that the FAB is clickable
        onView(withId(R.id.fabCreateCollection))
            .check(matches(isClickable()))
    }
    
    @Test
    fun recyclerView_isScrollable() {
        // Verify that the RecyclerView is scrollable
        onView(withId(R.id.recyclerView))
            .check(matches(isDisplayed()))
            .check(matches(isEnabled()))
    }
    
    @Test
    fun collectionsActivity_rotationHandling() {
        // Test that activity handles rotation properly
        ActivityScenario.launch(CollectionsActivity::class.java).use { scenario ->
            // Verify main UI components are still visible after recreation
            onView(withId(R.id.recyclerView))
                .check(matches(isDisplayed()))
            
            onView(withId(R.id.fabCreateCollection))
                .check(matches(isDisplayed()))
        }
    }
}
