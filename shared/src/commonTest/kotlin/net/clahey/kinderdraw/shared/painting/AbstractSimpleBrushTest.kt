package net.clahey.kinderdraw.shared.painting

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import kotlin.test.Test
import kotlin.test.assertEquals

class AbstractSimpleBrushTest {
    private val brush = DefaultBrush(color = Color.Red, strokeWidthPx = 4f)

    // @spec CANVAS-PAINT-005, CANVAS-PAINT-007
    @Test
    fun addPointExtendsWhatTheStrokeRenders() {
        val stroke = brush.startStroke(Point(0.1f, 0.5f))
        val bitmapBeforeSecondPoint = testDrawScope(width = 20, height = 20) { with(stroke) { render() } }
        stroke.addPoint(Point(0.9f, 0.5f))
        val bitmapAfterSecondPoint = testDrawScope(width = 20, height = 20) { with(stroke) { render() } }

        assertEquals(Color.Transparent, bitmapBeforeSecondPoint.toPixelMap()[10, 10])
        assertEquals(Color.Red, bitmapAfterSecondPoint.toPixelMap()[10, 10])
    }

    // @spec CANVAS-PAINT-013
    @Test
    fun restartProducesAStrokeContinuingFromTheLastPointWithTheSameBrush() {
        val stroke = brush.startStroke(Point(0.1f, 0.5f))
        stroke.addPoint(Point(0.5f, 0.5f))

        val restarted = stroke.restart()
        restarted.addPoint(Point(0.9f, 0.5f))
        val pixels = testDrawScope(width = 20, height = 20) { with(restarted) { render() } }.toPixelMap()

        // The restarted stroke draws from the interrupted stroke's last point
        // (0.5, 0.5) to the new one (0.9, 0.5), through their midpoint...
        assertEquals(Color.Red, pixels[14, 10])
        // ...but not back to the original stroke's first point (0.1, 0.5) —
        // proving restart() continued from the last point, not the first,
        // and didn't keep appending to the original stroke's own point list.
        assertEquals(Color.Transparent, pixels[2, 10])
    }
}
