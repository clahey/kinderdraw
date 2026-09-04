package net.clahey.kinderdraw.shared.painting

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GestureTrackingTest {
    // @spec CANVAS-PAINT-018
    @Test
    fun theFirstPointerDownLeavesTheGestureLive() {
        val tracked = mutableSetOf<String>()

        assertTrue(applyGestureChanges(tracked, down = listOf("a"), up = emptyList()))
        assertEquals(setOf("a"), tracked)
    }

    // @spec CANVAS-PAINT-018
    @Test
    fun aSecondPointerJoinsTheSameLiveGesture() {
        val tracked = mutableSetOf("a")

        assertTrue(applyGestureChanges(tracked, down = listOf("b"), up = emptyList()))
        assertEquals(setOf("a", "b"), tracked)
    }

    // @spec CANVAS-PAINT-018
    @Test
    fun theLastTrackedPointerLiftingEndsTheGesture() {
        val tracked = mutableSetOf("a")

        assertFalse(applyGestureChanges(tracked, down = emptyList(), up = listOf("a")))
        assertEquals(emptySet(), tracked)
    }

    // @spec CANVAS-PAINT-018
    @Test
    fun onePointerLiftingWhileAnotherStaysDownDoesNotEndTheGesture() {
        val tracked = mutableSetOf("a", "b")

        assertTrue(applyGestureChanges(tracked, down = emptyList(), up = listOf("a")))
        assertEquals(setOf("b"), tracked)
    }

    // @spec CANVAS-PAINT-018
    @Test
    fun aPointerLiftingAndADifferentPointerTouchingDownInTheSameEventDoesNotEndTheGesture() {
        val tracked = mutableSetOf("a")

        // The exact race CANVAS-PAINT-018 exists for: one input event
        // batches pointer a's lift together with a new pointer b's touch-down.
        assertTrue(applyGestureChanges(tracked, down = listOf("b"), up = listOf("a")))
        assertEquals(setOf("b"), tracked)
    }

    // @spec CANVAS-PAINT-018
    @Test
    fun theOnlyTwoTrackedPointersBothLiftingInTheSameEventEndsTheGesture() {
        val tracked = mutableSetOf("a", "b")

        assertFalse(applyGestureChanges(tracked, down = emptyList(), up = listOf("a", "b")))
        assertEquals(emptySet(), tracked)
    }
}
