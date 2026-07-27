package net.clahey.kinderdraw.shared.painting

import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import net.clahey.kinderdraw.shared.paintingstyle.FakeBrush
import net.clahey.kinderdraw.shared.paintingstyle.FakeStyleSettings
import net.clahey.kinderdraw.shared.paintingstyle.Point

private val saverScope = SaverScope { true }

class PaintingStateSaverTest {
    private val p0 = Point(0.1f, 0.1f)
    private val p1 = Point(0.2f, 0.3f)

    // @spec CANVAS-PAINT-011
    @Test
    fun restoredStateRendersEveryCompletedStrokeUnchanged() {
        val brush = FakeBrush()
        val settings = FakeStyleSettings(brush = brush)
        val original = PaintingState(settings)
        original.onPointerDown(p0)
        original.onPointerUp()
        original.onPointerDown(p1)
        original.onPointerUp()
        val saver = paintingStateSaver(settings)

        val saved = with(saver) { saverScope.save(original) }!!
        val restored = saver.restore(saved)!!
        testDrawScope { with(restored) { render() } }

        assertEquals(listOf(listOf(p0), listOf(p1)), brush.renderCalls)
    }

    // @spec CANVAS-PAINT-011
    @Test
    fun restoredStateFinalizesALiveStrokeUsingItsPointsSoFarWithoutStartingAReplacement() {
        val brush = FakeBrush()
        val settings = FakeStyleSettings(brush = brush)
        val original = PaintingState(settings)
        original.onPointerDown(p0)
        original.onPointerMove(p1)
        // Pointer never lifts - still live when persistence runs.
        val saver = paintingStateSaver(settings)

        val saved = with(saver) { saverScope.save(original) }!!
        val restored = saver.restore(saved)!!

        assertFalse(restored.isEmpty())
        testDrawScope { with(restored) { render() } }
        assertEquals(listOf(listOf(p0, p1)), brush.renderCalls)
    }

    // @spec CANVAS-PAINT-019
    @Test
    fun restoredStateReusesTheSavedBackgroundWithoutRequeryingStyleSettings() {
        val settings = FakeStyleSettings(background = Color.Red)
        val original = PaintingState(settings)
        val saver = paintingStateSaver(settings)
        val saved = with(saver) { saverScope.save(original) }!!

        // Simulate a background source that would return something else if queried again.
        settings.background = Color.Blue

        val restored = saver.restore(saved)!!
        val image = testDrawScope(width = 4, height = 4) { with(restored) { render() } }

        assertEquals(Color.Red, image.toPixelMap()[0, 0])
        // Only the original construction queried it - restore must not query again.
        assertEquals(1, settings.backgroundQueryCount)
    }
}
