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
 * Where a stroke may begin, given the edges the platform reserves for its own
 * gesture navigation — see the Painting LLD's System Gesture Coexistence. The
 * edge geometry itself is covered directly by [ReservedEdgesTest]; these tests
 * are about what the composable does with it.
 *
 * Every test passes its own `reservedInsets` rather than taking the default: a
 * desktop host reserves nothing, so the platform's own value would make the
 * suppression unobservable here.
 */
@OptIn(ExperimentalTestApi::class)
class PaintingReservedEdgesTest {
    // @spec CANVAS-PAINT-026
    @Test
    fun beginsNoStrokeWhenATouchLandsInAReservedEdge() = runComposeUiTest {
        val settings = FakeStyleSettings(brush = FakeBrush())
        val state = PaintingState(settings)

        setContent { EdgeAwarePainting(state) }

        onRoot().performTouchInput { down(bottomCenter) }
        onRoot().performTouchInput { up() }

        assertTrue(state.isEmpty(), "a touch in a reserved edge must leave no stroke behind")
        assertEquals(0, settings.brushQueryCount, "and must not reach PaintingState at all")
    }

    // @spec CANVAS-PAINT-030
    @Test
    fun beginsNoStrokeAtTheTopEdgeWhenThePlatformReservesItToo() = runComposeUiTest {
        val settings = FakeStyleSettings(brush = FakeBrush())
        val state = PaintingState(settings)

        setContent { EdgeAwarePainting(state, reservedInsets = TOP_AND_BOTTOM) }

        onRoot().performTouchInput { down(0, topCenter) }
        assertEquals(0, settings.brushQueryCount, "the top edge is reserved here as well")

        onRoot().performTouchInput { down(1, bottomCenter) }
        assertEquals(0, settings.brushQueryCount, "and so is the bottom")

        onRoot().performTouchInput { down(2, center) }
        assertEquals(1, settings.brushQueryCount, "the interior still draws")
    }

    // @spec CANVAS-PAINT-030
    @Test
    fun beginsStrokesAlongEveryEdgeThePlatformReservesNothingAlong() = runComposeUiTest {
        val settings = FakeStyleSettings(brush = FakeBrush())
        val state = PaintingState(settings)

        // Only the bottom is reserved, so the other three stay open however the
        // platform reports that one.
        setContent { EdgeAwarePainting(state) }

        onRoot().performTouchInput { down(0, centerLeft) }
        onRoot().performTouchInput { down(1, centerRight) }
        onRoot().performTouchInput { down(2, topCenter) }

        assertEquals(3, settings.brushQueryCount)
    }

    // @spec CANVAS-PAINT-027
    @Test
    fun holdsTheLockButConsumesNothingForATouchInAReservedEdge() = runComposeUiTest {
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
                EdgeAwarePainting(state, lock)
            }
        }

        onRoot().performTouchInput { down(bottomCenter) }
        // The pointer draws nothing, but it still bounds a gesture, which is what
        // keeps the loop live for anything that lands next.
        assertTrue(lock.isHeld(), "a touch in a reserved edge still takes the hold")

        onRoot().performTouchInput { moveTo(bottomCenter - Offset(0f, 2f)) }
        assertTrue(lock.isHeld())

        assertTrue(pressedChanges > 0, "the touch must reach Painting at all")
        assertEquals(0, consumedChanges, "and every change must come back unconsumed")

        onRoot().performTouchInput { up() }
        assertFalse(lock.isHeld(), "and the hold ends with it")
    }

    // @spec CANVAS-PAINT-027
    @Test
    fun leavesALiveStrokeAndItsHoldAloneWhenAnotherTouchLandsInAReservedEdge() = runComposeUiTest {
        val settings = FakeStyleSettings(brush = FakeBrush())
        val state = PaintingState(settings)
        val lock = InteractionLock()

        setContent { EdgeAwarePainting(state, lock) }

        onRoot().performTouchInput { down(0, center) }
        assertTrue(lock.isHeld())
        assertEquals(1, settings.brushQueryCount)

        // A second finger landing in a reserved edge joins nothing and starts
        // nothing.
        onRoot().performTouchInput { down(1, bottomCenter) }
        assertTrue(lock.isHeld(), "the live stroke's own hold is untouched")
        assertEquals(1, settings.brushQueryCount, "and no second stroke begins")

        // The suppressed pointer bounds the gesture like any other, so the hold
        // outlasts the drawing one and ends only once both have lifted.
        onRoot().performTouchInput { up(0) }
        assertTrue(lock.isHeld(), "the suppressed pointer is still down")

        onRoot().performTouchInput { up(1) }
        assertFalse(lock.isHeld())
    }

    // @spec CANVAS-PAINT-032
    @Test
    fun drawsNormallyWhileATouchRestsInsideAReservedEdge() = runComposeUiTest {
        val settings = FakeStyleSettings(brush = FakeBrush())
        val state = PaintingState(settings)

        setContent { EdgeAwarePainting(state, reservedInsets = TOP_AND_BOTTOM) }

        // A thumb settling in the reserved top band and staying there.
        onRoot().performTouchInput { down(0, topCenter) }
        assertEquals(0, settings.brushQueryCount)

        // The rest of the surface has to go on working underneath it, rather than
        // every later touch being swallowed until that thumb lifts.
        onRoot().performTouchInput { down(1, center) }
        assertEquals(1, settings.brushQueryCount, "a resting edge touch must not deaden the canvas")

        onRoot().performTouchInput { moveTo(1, center - Offset(0f, 10f)) }
        onRoot().performTouchInput { up(1) }
        assertFalse(state.isEmpty(), "and the stroke it drew is kept")
    }

    // @spec CANVAS-PAINT-028
    @Test
    fun beginsNoStrokeForAPointerThatLeavesAReservedEdgeWhileStillDown() = runComposeUiTest {
        val settings = FakeStyleSettings(brush = FakeBrush())
        val state = PaintingState(settings)

        setContent { EdgeAwarePainting(state) }

        onRoot().performTouchInput { down(bottomCenter) }
        // A swipe leaves the band within a few milliseconds; that exit must not
        // be read as a stroke start.
        onRoot().performTouchInput { moveTo(center) }
        onRoot().performTouchInput { moveTo(topCenter) }

        assertTrue(state.isEmpty())
        assertEquals(0, settings.brushQueryCount)

        // Only a fresh touch-down outside the reserved edges is eligible again.
        onRoot().performTouchInput { up() }
        onRoot().performTouchInput { down(center) }
        assertFalse(state.isEmpty())
    }

    // @spec CANVAS-PAINT-028
    @Test
    fun suppressesAPointerOnlyForAsLongAsItStaysDown() = runComposeUiTest {
        val settings = FakeStyleSettings(brush = FakeBrush())
        val state = PaintingState(settings)

        setContent { EdgeAwarePainting(state) }

        // One finger on the canvas keeps the gesture open throughout, so the
        // suppressed pointer below lifts without the gesture ending.
        onRoot().performTouchInput { down(0, center) }
        onRoot().performTouchInput { down(1, bottomCenter) }
        onRoot().performTouchInput { up(1) }
        assertEquals(1, settings.brushQueryCount, "the edge touch drew nothing while it was down")

        // That pointer is gone, so nothing about it should reach the next touch
        // that happens to carry the same id.
        onRoot().performTouchInput { down(1, center) }
        assertEquals(2, settings.brushQueryCount, "a later pointer reusing the id must draw")
    }

    // @spec CANVAS-PAINT-029
    @Test
    fun goesOnRecordingAStrokeThatMovesIntoAReservedEdge() = runComposeUiTest {
        val brush = FakeBrush()
        val state = PaintingState(FakeStyleSettings(brush = brush))

        setContent { EdgeAwarePainting(state) }

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
        assertEquals(2, points.size, "the point inside the reserved edge must be recorded like any other")
    }

    // @spec CANVAS-PAINT-031
    @Test
    fun leavesTheWholeSurfaceOpenWhenThePlatformReservesNothing() = runComposeUiTest {
        val settings = FakeStyleSettings(brush = FakeBrush())
        val state = PaintingState(settings)

        setContent { EdgeAwarePainting(state, reservedInsets = WindowInsets(bottom = 0.dp)) }

        onRoot().performTouchInput { down(bottomCenter) }

        assertFalse(state.isEmpty(), "with nothing reserved, every edge draws like the interior")
        assertEquals(1, settings.brushQueryCount)
    }

    // @spec CANVAS-PAINT-031
    @Test
    fun readsTheReservedEdgesAsOfEachTouchDown() = runComposeUiTest {
        val settings = FakeStyleSettings(brush = FakeBrush())
        val state = PaintingState(settings)
        var insets by mutableStateOf(WindowInsets(bottom = 0.dp))

        setContent { EdgeAwarePainting(state, reservedInsets = insets) }

        onRoot().performTouchInput { down(0, bottomCenter) }
        assertEquals(1, settings.brushQueryCount, "nothing is reserved yet")
        onRoot().performTouchInput { up(0) }

        // A rotation, or any other change to what the platform reserves, applies
        // to the next touch without Painting being told about it.
        insets = RESERVED
        waitForIdle()

        onRoot().performTouchInput { down(1, bottomCenter) }
        assertEquals(1, settings.brushQueryCount, "the same spot is now inside a reserved edge")
    }
}

/** A surface large enough that the insets below cover only its edges. */
private val SURFACE = 100.dp

/** Wide enough that a touch at the surface's bottom edge lands well inside it. */
private val RESERVED = WindowInsets(bottom = 20.dp)

/** What today's Android reports: the bottom edge reserved, and the top as well. */
private val TOP_AND_BOTTOM = WindowInsets(top = 20.dp, bottom = 20.dp)

@Composable
private fun EdgeAwarePainting(
    state: PaintingState,
    lock: InteractionLock = InteractionLock(),
    reservedInsets: WindowInsets = RESERVED,
) {
    Painting(
        state = state,
        lock = lock,
        modifier = Modifier.size(SURFACE),
        reservedInsets = reservedInsets,
    )
}
