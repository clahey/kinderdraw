package net.clahey.kinderdraw.shared.painting

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onRoot
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

/**
 * Where a stroke may begin, given the strip the platform reserves along the
 * bottom edge for its own gesture navigation — see the Painting LLD's System
 * Gesture Coexistence.
 *
 * Every test passes its own `reservedInsets` rather than taking the default: a
 * desktop host reports no reserved region at all, so the platform's own value
 * would make the suppression unobservable here.
 */
@OptIn(ExperimentalTestApi::class)
class PaintingGestureStripTest {
    // @spec CANVAS-PAINT-026
    @Test
    fun beginsNoStrokeWhenATouchLandsInTheReservedBottomStrip() = runComposeUiTest {
        val settings = FakeStyleSettings(brush = FakeBrush())
        val state = PaintingState(settings)

        setContent { StripPainting(state) }

        onRoot().performTouchInput { down(bottomCenter) }
        onRoot().performTouchInput { up() }

        assertTrue(state.isEmpty(), "a touch in the reserved strip must leave no stroke behind")
        assertEquals(0, settings.brushQueryCount, "and must not reach PaintingState at all")
    }

    // @spec CANVAS-PAINT-027
    @Test
    fun takesNoHoldAndConsumesNothingForATouchInTheStrip() = runComposeUiTest {
        val state = PaintingState(FakeStyleSettings(brush = FakeBrush()))
        val lock = InteractionLock()

        // Leaving the change unconsumed is what the platform's own gesture
        // detection coexists with, and is only observable from above Painting.
        var pressedChanges = 0
        var consumedChanges = 0

        setContent {
            Box(
                Modifier.pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            // The Final pass runs after Painting has had the
                            // change, so consumption by it is visible here.
                            val change = awaitPointerEvent(PointerEventPass.Final).changes.single()
                            if (change.pressed) {
                                pressedChanges++
                                if (change.isConsumed) consumedChanges++
                            }
                        }
                    }
                }
            ) {
                StripPainting(state, lock)
            }
        }

        onRoot().performTouchInput { down(bottomCenter) }
        assertFalse(lock.isHeld(), "a swipe that begins in the strip starts no gesture to hold for")

        onRoot().performTouchInput { moveTo(bottomCenter - Offset(0f, 2f)) }
        assertFalse(lock.isHeld())

        assertTrue(pressedChanges > 0, "the touch must reach Painting at all")
        assertEquals(0, consumedChanges, "and every change must come back unconsumed")
    }

    // @spec CANVAS-PAINT-027
    @Test
    fun leavesALiveStrokeAndItsHoldAloneWhenAnotherTouchLandsInTheStrip() = runComposeUiTest {
        val settings = FakeStyleSettings(brush = FakeBrush())
        val state = PaintingState(settings)
        val lock = InteractionLock()

        setContent { StripPainting(state, lock) }

        onRoot().performTouchInput { down(0, center) }
        assertTrue(lock.isHeld())
        assertEquals(1, settings.brushQueryCount)

        // A second finger landing in the strip joins nothing and starts nothing.
        onRoot().performTouchInput { down(1, bottomCenter) }
        assertTrue(lock.isHeld(), "the live stroke's own hold is untouched")
        assertEquals(1, settings.brushQueryCount, "and no second stroke begins")

        // The hold still ends with the pointer that actually took it, not with
        // the suppressed one, which was never part of the gesture.
        onRoot().performTouchInput { up(0) }
        assertFalse(lock.isHeld())
    }

    // @spec CANVAS-PAINT-028
    @Test
    fun beginsNoStrokeForAPointerThatLeavesTheStripWhileStillDown() = runComposeUiTest {
        val settings = FakeStyleSettings(brush = FakeBrush())
        val state = PaintingState(settings)

        setContent { StripPainting(state) }

        onRoot().performTouchInput { down(bottomCenter) }
        // A swipe up leaves the strip within a few milliseconds; that exit must
        // not be read as a stroke start.
        onRoot().performTouchInput { moveTo(center) }
        onRoot().performTouchInput { moveTo(topCenter) }

        assertTrue(state.isEmpty())
        assertEquals(0, settings.brushQueryCount)

        // Only a fresh touch-down outside the strip is eligible again.
        onRoot().performTouchInput { up() }
        onRoot().performTouchInput { down(center) }
        assertFalse(state.isEmpty())
    }

    // @spec CANVAS-PAINT-029
    @Test
    fun goesOnRecordingAStrokeThatMovesIntoTheStrip() = runComposeUiTest {
        val brush = FakeBrush()
        val state = PaintingState(FakeStyleSettings(brush = brush))

        setContent { StripPainting(state) }

        onRoot().performTouchInput { down(center) }
        onRoot().performTouchInput { moveTo(bottomCenter) }
        onRoot().performTouchInput { up() }

        assertFalse(state.isEmpty())
        // Rasterizing renders every stroke through the brush, so the captured
        // points are readable without depending on a frame having been drawn.
        state.snapshot()
        val points = assertNotNull(
            brush.renderCalls.lastOrNull(),
            "the stroke must have rendered at least once",
        )
        assertEquals(2, points.size, "the point inside the strip must be recorded like any other")
    }

    // @spec CANVAS-PAINT-030
    @Test
    fun beginsStrokesNormallyAtEveryOtherEdge() = runComposeUiTest {
        val settings = FakeStyleSettings(brush = FakeBrush())
        val state = PaintingState(settings)

        setContent { StripPainting(state) }

        // The side edges reserve a gesture strip of their own, but it detects
        // back, which the canvas already consumes — so a stroke may start there.
        onRoot().performTouchInput { down(0, centerLeft) }
        onRoot().performTouchInput { down(1, centerRight) }
        onRoot().performTouchInput { down(2, topCenter) }

        assertEquals(3, settings.brushQueryCount)
    }

    // @spec CANVAS-PAINT-031
    @Test
    fun leavesTheWholeSurfaceOpenWhenThePlatformReportsNoStrip() = runComposeUiTest {
        val settings = FakeStyleSettings(brush = FakeBrush())
        val state = PaintingState(settings)

        setContent { StripPainting(state, reservedInsets = WindowInsets(bottom = 0.dp)) }

        onRoot().performTouchInput { down(bottomCenter) }

        assertFalse(state.isEmpty(), "with nothing reserved, the bottom edge draws like anywhere else")
        assertEquals(1, settings.brushQueryCount)
    }

    // @spec CANVAS-PAINT-031
    @Test
    fun readsTheReservedStripAsOfEachTouchDown() = runComposeUiTest {
        val settings = FakeStyleSettings(brush = FakeBrush())
        val state = PaintingState(settings)
        var insets by mutableStateOf(WindowInsets(bottom = 0.dp))

        setContent { StripPainting(state, reservedInsets = insets) }

        onRoot().performTouchInput { down(0, bottomCenter) }
        assertEquals(1, settings.brushQueryCount, "nothing is reserved yet")
        onRoot().performTouchInput { up(0) }

        // A rotation, or any other change to what the platform reserves, applies
        // to the next touch without Painting being told about it.
        insets = STRIP
        waitForIdle()

        onRoot().performTouchInput { down(1, bottomCenter) }
        assertEquals(1, settings.brushQueryCount, "the same spot is now inside the strip")
    }
}

/** A surface tall enough that [STRIP] covers only its bottom edge. */
private val SURFACE = 100.dp

/** Wide enough that a touch at the surface's bottom edge lands well inside it. */
private val STRIP = WindowInsets(bottom = 20.dp)

@Composable
private fun StripPainting(
    state: PaintingState,
    lock: InteractionLock = InteractionLock(),
    reservedInsets: WindowInsets = STRIP,
) {
    Painting(
        state = state,
        lock = lock,
        modifier = Modifier.size(SURFACE),
        reservedInsets = reservedInsets,
    )
}
