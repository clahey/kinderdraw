package net.clahey.kinderdraw.shared.paintingstyle

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import kotlin.test.Test
import kotlin.test.assertEquals
import net.clahey.kinderdraw.shared.painting.testDrawScope

class DefaultBrushTest {
    private val brush = DefaultBrush(colorSource = ConstantColor(Color.Red), strokeWidthPx = 4f)

    // @spec CANVAS-STYLE-002
    @Test
    fun singlePointTapRendersAVisibleMark() {
        val bitmap = testDrawScope(width = 20, height = 20) {
            with(brush) { render(listOf(Point(0f, 0f)), Color.Red) }
        }
        val pixels = bitmap.toPixelMap()

        assertEquals(Color.Red, pixels[10, 10])
        assertEquals(Color.Transparent, pixels[1, 1])
    }

    // @spec CANVAS-STYLE-002
    @Test
    fun multiPointStrokeRendersAConnectingLine() {
        val bitmap = testDrawScope(width = 20, height = 20) {
            with(brush) {
                render(listOf(Point(-0.4f, 0f), Point(0.4f, 0f)), Color.Red)
            }
        }
        val pixels = bitmap.toPixelMap()

        // Midpoint of the line is colored...
        assertEquals(Color.Red, pixels[10, 10])
        // ...but a corner well away from the line is untouched.
        assertEquals(Color.Transparent, pixels[1, 1])
    }

    // @spec CANVAS-STYLE-001
    @Test
    fun rendersCorrectlyWhenCalledRepeatedlyWithAGrowingPointList() {
        val points = mutableListOf(Point(-0.4f, 0f))
        val bitmapAfterFirstPoint = testDrawScope(width = 20, height = 20) {
            with(brush) { render(points, Color.Red) }
        }
        points.add(Point(0.4f, 0f))
        val bitmapAfterSecondPoint = testDrawScope(width = 20, height = 20) {
            with(brush) { render(points, Color.Red) }
        }

        // Rendering the one-point list drew only a mark at that point, not a line...
        assertEquals(Color.Transparent, bitmapAfterFirstPoint.toPixelMap()[10, 10])
        // ...while rendering the full two-point list (as if called again after a
        // new point arrived) drew the connecting line, with no leftover state
        // from the earlier call needed to do so.
        assertEquals(Color.Red, bitmapAfterSecondPoint.toPixelMap()[10, 10])
    }

    // @spec CANVAS-STYLE-002
    @Test
    fun rendersWithWhateverColorItsGiven() {
        val bitmap = testDrawScope(width = 20, height = 20) {
            with(brush) { render(listOf(Point(0f, 0f)), Color.Blue) }
        }

        assertEquals(Color.Blue, bitmap.toPixelMap()[10, 10])
    }
}
