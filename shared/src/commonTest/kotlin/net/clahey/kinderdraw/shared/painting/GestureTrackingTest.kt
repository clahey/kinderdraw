package net.clahey.kinderdraw.shared.painting

import kotlin.test.Test
import kotlin.test.assertEquals

class GestureTrackingTest {
    // @spec CANVAS-PAINT-018
    @Test
    fun firstPointerDownWithNothingElseTrackedStartsTheGesture() {
        val tracked = mutableSetOf<String>()

        val edge = applyGestureChanges(tracked, down = listOf("a"), up = emptyList())

        assertEquals(GestureEdge.STARTED, edge)
        assertEquals(setOf("a"), tracked)
    }

    // @spec CANVAS-PAINT-018
    @Test
    fun aSecondPointerDownWhileOneIsAlreadyTrackedDoesNotRestartTheGesture() {
        val tracked = mutableSetOf("a")

        val edge = applyGestureChanges(tracked, down = listOf("b"), up = emptyList())

        assertEquals(GestureEdge.UNCHANGED, edge)
        assertEquals(setOf("a", "b"), tracked)
    }

    // @spec CANVAS-PAINT-018
    @Test
    fun theLastTrackedPointerLiftingEndsTheGesture() {
        val tracked = mutableSetOf("a")

        val edge = applyGestureChanges(tracked, down = emptyList(), up = listOf("a"))

        assertEquals(GestureEdge.ENDED, edge)
        assertEquals(emptySet(), tracked)
    }

    // @spec CANVAS-PAINT-018
    @Test
    fun onePointerLiftingWhileAnotherStaysDownDoesNotEndTheGesture() {
        val tracked = mutableSetOf("a", "b")

        val edge = applyGestureChanges(tracked, down = emptyList(), up = listOf("a"))

        assertEquals(GestureEdge.UNCHANGED, edge)
        assertEquals(setOf("b"), tracked)
    }

    // @spec CANVAS-PAINT-018
    @Test
    fun aPointerLiftingAndADifferentPointerTouchingDownInTheSameEventDoesNotEndOrRestartTheGesture() {
        val tracked = mutableSetOf("a")

        // The exact race CANVAS-PAINT-018 exists for: one input event
        // batches pointer a's lift together with a new pointer b's touch-down.
        val edge = applyGestureChanges(tracked, down = listOf("b"), up = listOf("a"))

        assertEquals(GestureEdge.UNCHANGED, edge)
        assertEquals(setOf("b"), tracked)
    }

    // @spec CANVAS-PAINT-018
    @Test
    fun theOnlyTwoTrackedPointersBothLiftingInTheSameEventEndsTheGesture() {
        val tracked = mutableSetOf("a", "b")

        val edge = applyGestureChanges(tracked, down = emptyList(), up = listOf("a", "b"))

        assertEquals(GestureEdge.ENDED, edge)
        assertEquals(emptySet(), tracked)
    }
}
