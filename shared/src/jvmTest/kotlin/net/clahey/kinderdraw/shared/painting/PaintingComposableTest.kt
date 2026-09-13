package net.clahey.kinderdraw.shared.painting

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import net.clahey.kinderdraw.shared.paintingstyle.FakeBrush
import net.clahey.kinderdraw.shared.paintingstyle.FakeStyleSettings
import net.clahey.kinderdraw.shared.userexperience.InteractionLock
import net.clahey.kinderdraw.shared.userexperience.isHeld

@OptIn(ExperimentalTestApi::class)
class PaintingComposableTest {
    // @spec CANVAS-PAINT-018
    @Test
    fun holdsTheInteractionForAsLongAsTheGestureLasts() = runComposeUiTest {
        val state = PaintingState(FakeStyleSettings(brush = FakeBrush()))
        val lock = InteractionLock()

        setContent { Painting(state = state, lock = lock, modifier = Modifier.size(100.dp)) }

        // Each performTouchInput block only flushes its own events to the
        // composable's pointer input handler once the block exits, so the
        // gesture is split across calls to observe the lock mid-gesture.
        onRoot().performTouchInput { down(Offset(10f, 10f)) }
        assertTrue(lock.isHeld())

        onRoot().performTouchInput { moveTo(Offset(20f, 20f)) }
        assertTrue(lock.isHeld())

        onRoot().performTouchInput { up() }
        assertFalse(lock.isHeld())
        assertFalse(state.isEmpty())
    }

    // @spec CANVAS-PAINT-001, CANVAS-PAINT-018, CANVAS-PAINT-020
    @Test
    fun twoConcurrentTouchesEachDrawTheirOwnStrokeUnderOneHold() = runComposeUiTest {
        val state = PaintingState(FakeStyleSettings(brush = FakeBrush()))
        val lock = InteractionLock()

        setContent { Painting(state = state, lock = lock, modifier = Modifier.size(100.dp)) }

        onRoot().performTouchInput { down(0, Offset(10f, 10f)) }
        assertTrue(lock.isHeld())

        // A second concurrent touch joins the same held gesture rather than
        // taking a hold of its own.
        onRoot().performTouchInput { down(1, Offset(80f, 80f)) }
        assertTrue(lock.isHeld())

        onRoot().performTouchInput { up(0) }
        // The other finger is still down — the gesture isn't over yet.
        assertTrue(lock.isHeld())
        assertFalse(state.isEmpty())

        onRoot().performTouchInput { up(1) }
        assertFalse(lock.isHeld())
    }

    // @spec CANVAS-PAINT-022
    @Test
    fun startsNoStrokeAtAllWhenTheLockRefusesIt() = runComposeUiTest {
        val settings = FakeStyleSettings(brush = FakeBrush())
        val state = PaintingState(settings)
        val lock = InteractionLock()
        val heldElsewhere = assertNotNull(lock.tryAcquire())

        setContent { Painting(state = state, lock = lock, modifier = Modifier.size(100.dp)) }
        val brushQueriesBefore = settings.brushQueryCount

        onRoot().performTouchInput { down(Offset(10f, 10f)) }
        onRoot().performTouchInput { moveTo(Offset(20f, 20f)) }

        assertTrue(state.isEmpty())
        assertEquals(brushQueriesBefore, settings.brushQueryCount, "a refused pointer must not reach PaintingState")

        // Still refused for the rest of that gesture, even once the other
        // holder is gone — only a fresh touch-down is eligible.
        heldElsewhere.release()
        onRoot().performTouchInput { moveTo(Offset(30f, 30f)) }
        assertTrue(state.isEmpty())

        onRoot().performTouchInput { up() }
        onRoot().performTouchInput { down(Offset(40f, 40f)) }
        assertFalse(state.isEmpty())
    }

    // @spec CANVAS-PAINT-024
    @Test
    fun leavesAHoveringPointerEntirelyAlone() = runComposeUiTest {
        val settings = FakeStyleSettings(brush = FakeBrush())
        val state = PaintingState(settings)
        val lock = InteractionLock()

        // Painting leaving the change unconsumed is what a would-be hover
        // handler above it depends on, and is the only half of this observable
        // from outside: recording a hover against a stroke that was never
        // started is already a no-op in PaintingState.
        var hoverConsumed: Boolean? = null

        setContent {
            Box(
                Modifier.pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            // The Final pass runs after Painting has had the
                            // change, so consumption by it is visible here.
                            val change = awaitPointerEvent(PointerEventPass.Final).changes.single()
                            if (!change.pressed) hoverConsumed = change.isConsumed
                        }
                    }
                }
            ) {
                Painting(state = state, lock = lock, modifier = Modifier.size(100.dp))
            }
        }
        val brushQueriesBefore = settings.brushQueryCount

        // A mouse moving with no button down is a pointer that is neither
        // pressed nor newly lifted.
        onRoot().performMouseInput { moveTo(Offset(10f, 10f)) }
        onRoot().performMouseInput { moveTo(Offset(20f, 20f)) }

        assertEquals(false, hoverConsumed, "the hover must reach Painting and come back unconsumed")
        assertFalse(lock.isHeld())
        assertTrue(state.isEmpty())
        assertEquals(brushQueriesBefore, settings.brushQueryCount, "a hovering pointer must not reach PaintingState")
    }

    // @spec CANVAS-PAINT-023
    @Test
    fun releasesItsHoldWhenTheGestureIsCancelled() = runComposeUiTest {
        val state = PaintingState(FakeStyleSettings(brush = FakeBrush()))
        val lock = InteractionLock()
        var shown by mutableStateOf(true)

        setContent {
            if (shown) Painting(state = state, lock = lock, modifier = Modifier.size(100.dp))
        }

        onRoot().performTouchInput { down(Offset(10f, 10f)) }
        assertTrue(lock.isHeld())

        // Leaving composition cancels the pointer-input coroutine mid-gesture.
        shown = false
        waitForIdle()

        assertFalse(lock.isHeld())
        // The stroke that was live stays in the state for Lifecycle Survival.
        assertFalse(state.isEmpty())
    }
}
