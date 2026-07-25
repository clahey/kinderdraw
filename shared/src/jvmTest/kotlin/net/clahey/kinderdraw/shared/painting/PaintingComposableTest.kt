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
import kotlin.test.assertEquals
import kotlin.test.assertFalse

@OptIn(ExperimentalTestApi::class)
class PaintingComposableTest {
    // @spec CANVAS-PAINT-018
    @Test
    fun reportsStrokeActiveAcrossAGesture() = runComposeUiTest {
        val state = PaintingState(FakeActiveStrokeSettings(brush = FakeBrush()))
        val activeChanges = mutableListOf<Boolean>()

        setContent {
            Painting(
                state = state,
                modifier = Modifier.size(100.dp),
                onStrokeActiveChange = { activeChanges.add(it) },
            )
        }

        onRoot().performTouchInput {
            down(Offset(10f, 10f))
            moveTo(Offset(20f, 20f))
            up()
        }

        assertEquals(listOf(true, false), activeChanges)
        assertFalse(state.isEmpty())
    }
}
