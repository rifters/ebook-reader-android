package com.rifters.ebookreader

import org.junit.Assert.*
import org.junit.Test

/**
 * Test to validate the collection update logic that prevents blind updates.
 * This test verifies that only changed collection memberships trigger database operations.
 */
class CollectionUpdateLogicTest {
    
    data class CollectionChange(
        val collectionId: Long,
        val shouldAdd: Boolean,
        val shouldRemove: Boolean
    )
    
    /**
     * Simulates the logic from MainActivity.showAddToCollectionDialog
     * to determine which collections need add/remove operations.
     */
    private fun calculateCollectionChanges(
        collectionIds: List<Long>,
        initialState: BooleanArray,
        finalState: BooleanArray
    ): List<CollectionChange> {
        val changes = mutableListOf<CollectionChange>()
        
        collectionIds.forEachIndexed { index, collectionId ->
            val wasInCollection = initialState[index]
            val isNowInCollection = finalState[index]
            
            val shouldAdd = !wasInCollection && isNowInCollection
            val shouldRemove = wasInCollection && !isNowInCollection
            
            if (shouldAdd || shouldRemove) {
                changes.add(CollectionChange(collectionId, shouldAdd, shouldRemove))
            }
        }
        
        return changes
    }
    
    @Test
    fun `no changes when initial and final states are identical - all unchecked`() {
        val collectionIds = listOf(1L, 2L, 3L)
        val initialState = booleanArrayOf(false, false, false)
        val finalState = booleanArrayOf(false, false, false)
        
        val changes = calculateCollectionChanges(collectionIds, initialState, finalState)
        
        assertEquals("No operations should be performed when states are identical", 0, changes.size)
    }
    
    @Test
    fun `no changes when initial and final states are identical - all checked`() {
        val collectionIds = listOf(1L, 2L, 3L)
        val initialState = booleanArrayOf(true, true, true)
        val finalState = booleanArrayOf(true, true, true)
        
        val changes = calculateCollectionChanges(collectionIds, initialState, finalState)
        
        assertEquals("No operations should be performed when states are identical", 0, changes.size)
    }
    
    @Test
    fun `no changes when initial and final states are identical - mixed`() {
        val collectionIds = listOf(1L, 2L, 3L)
        val initialState = booleanArrayOf(true, false, true)
        val finalState = booleanArrayOf(true, false, true)
        
        val changes = calculateCollectionChanges(collectionIds, initialState, finalState)
        
        assertEquals("No operations should be performed when states are identical", 0, changes.size)
    }
    
    @Test
    fun `add operation when book was not in collection but now should be`() {
        val collectionIds = listOf(1L, 2L, 3L)
        val initialState = booleanArrayOf(false, false, false)
        val finalState = booleanArrayOf(true, false, false)
        
        val changes = calculateCollectionChanges(collectionIds, initialState, finalState)
        
        assertEquals("One add operation should be performed", 1, changes.size)
        assertEquals(1L, changes[0].collectionId)
        assertTrue("Should add to collection", changes[0].shouldAdd)
        assertFalse("Should not remove from collection", changes[0].shouldRemove)
    }
    
    @Test
    fun `remove operation when book was in collection but now should not be`() {
        val collectionIds = listOf(1L, 2L, 3L)
        val initialState = booleanArrayOf(true, false, false)
        val finalState = booleanArrayOf(false, false, false)
        
        val changes = calculateCollectionChanges(collectionIds, initialState, finalState)
        
        assertEquals("One remove operation should be performed", 1, changes.size)
        assertEquals(1L, changes[0].collectionId)
        assertFalse("Should not add to collection", changes[0].shouldAdd)
        assertTrue("Should remove from collection", changes[0].shouldRemove)
    }
    
    @Test
    fun `multiple add operations for multiple changes`() {
        val collectionIds = listOf(1L, 2L, 3L)
        val initialState = booleanArrayOf(false, false, false)
        val finalState = booleanArrayOf(true, true, false)
        
        val changes = calculateCollectionChanges(collectionIds, initialState, finalState)
        
        assertEquals("Two add operations should be performed", 2, changes.size)
        assertEquals(1L, changes[0].collectionId)
        assertTrue("Should add to collection 1", changes[0].shouldAdd)
        assertEquals(2L, changes[1].collectionId)
        assertTrue("Should add to collection 2", changes[1].shouldAdd)
    }
    
    @Test
    fun `multiple remove operations for multiple changes`() {
        val collectionIds = listOf(1L, 2L, 3L)
        val initialState = booleanArrayOf(true, true, false)
        val finalState = booleanArrayOf(false, false, false)
        
        val changes = calculateCollectionChanges(collectionIds, initialState, finalState)
        
        assertEquals("Two remove operations should be performed", 2, changes.size)
        assertEquals(1L, changes[0].collectionId)
        assertTrue("Should remove from collection 1", changes[0].shouldRemove)
        assertEquals(2L, changes[1].collectionId)
        assertTrue("Should remove from collection 2", changes[1].shouldRemove)
    }
    
    @Test
    fun `mixed add and remove operations`() {
        val collectionIds = listOf(1L, 2L, 3L, 4L)
        val initialState = booleanArrayOf(true, false, true, false)
        val finalState = booleanArrayOf(false, true, true, false)
        
        val changes = calculateCollectionChanges(collectionIds, initialState, finalState)
        
        assertEquals("Two operations should be performed (one add, one remove)", 2, changes.size)
        
        // First change: remove from collection 1
        assertEquals(1L, changes[0].collectionId)
        assertTrue("Should remove from collection 1", changes[0].shouldRemove)
        assertFalse("Should not add to collection 1", changes[0].shouldAdd)
        
        // Second change: add to collection 2
        assertEquals(2L, changes[1].collectionId)
        assertTrue("Should add to collection 2", changes[1].shouldAdd)
        assertFalse("Should not remove from collection 2", changes[1].shouldRemove)
    }
    
    @Test
    fun `old behavior would cause unnecessary operations`() {
        // This test demonstrates the bug in the old implementation
        val collectionIds = listOf(1L, 2L, 3L)
        val initialState = booleanArrayOf(true, false, true)
        val finalState = booleanArrayOf(true, false, true)
        
        val changes = calculateCollectionChanges(collectionIds, initialState, finalState)
        
        // With the old implementation, all 3 collections would have operations:
        // - Collection 1: try to add (even though already there)
        // - Collection 2: try to remove (even though not there)
        // - Collection 3: try to add (even though already there)
        // Total: 3 unnecessary operations
        
        // With the new implementation:
        assertEquals("No operations should be performed when no state changed", 0, changes.size)
    }
}
