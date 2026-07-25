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

@OptIn(ExperimentalTestApi::class)
class PaintingComposableTest {
    // @spec CANVAS-PAINT-018
    @Test
    fun reportsStrokeActiveWhileAGestureIsLive() = runComposeUiTest {
        val state = PaintingState(FakeActiveStrokeSettings(brush = FakeBrush()))
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
}
