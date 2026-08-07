package net.clahey.kinderdraw.shared.painting

import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.clahey.kinderdraw.shared.paintingstyle.FakeBrush
import net.clahey.kinderdraw.shared.paintingstyle.FakeStyleSettings

@OptIn(ExperimentalTestApi::class)
class PaintingComposableTest {
    // @spec CANVAS-PAINT-018
    @Test
    fun reportsStrokeActiveWhileAGestureIsLive() = runComposeUiTest {
        val state = PaintingState(FakeStyleSettings(brush = FakeBrush()))
        var active = false

        setContent {
            Painting(
                state = state,
                modifier = Modifier.size(100.dp),
                onStrokeActiveChange = { active = it },
            )
        }

        // Each performTouchInput block only flushes its own events to the
        // composable's pointer input handler once the block exits, so the
        // gesture is split across calls to observe state mid-gesture.
        onRoot().performTouchInput { down(Offset(10f, 10f)) }
        assertTrue(active)

        onRoot().performTouchInput { moveTo(Offset(20f, 20f)) }
        assertTrue(active)

        onRoot().performTouchInput { up() }
        assertFalse(active)
        assertFalse(state.isEmpty())
    }

    // @spec CANVAS-PAINT-001, CANVAS-PAINT-018, CANVAS-PAINT-020
    @Test
    fun twoConcurrentTouchesEachDrawTheirOwnStrokeWithOneGestureSpanningBoth() = runComposeUiTest {
        val state = PaintingState(FakeStyleSettings(brush = FakeBrush()))
        var active = false

        setContent {
            Painting(
                state = state,
                modifier = Modifier.size(100.dp),
                onStrokeActiveChange = { active = it },
            )
        }

        onRoot().performTouchInput { down(0, Offset(10f, 10f)) }
        assertTrue(active)

        onRoot().performTouchInput { down(1, Offset(80f, 80f)) }
        // A second concurrent touch joins the same live gesture rather than
        // re-triggering onStrokeActiveChange.
        assertTrue(active)

        onRoot().performTouchInput { up(0) }
        // The other finger is still down — the gesture isn't over yet.
        assertTrue(active)
        assertFalse(state.isEmpty())

        onRoot().performTouchInput { up(1) }
        assertFalse(active)
    }
}
