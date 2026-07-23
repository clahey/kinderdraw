package net.clahey.kinderdraw.shared.painting

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import kotlin.test.Test
import kotlin.test.assertEquals

class DefaultBrushTest {
    private val brush = DefaultBrush(color = Color.Red, strokeWidthPx = 4f)

    // @spec CANVAS-PAINT-006
    @Test
    fun singlePointTapRendersAVisibleMark() {
        val bitmap = testDrawScope(width = 20, height = 20) {
            with(brush) { render(listOf(Point(0.5f, 0.5f))) }
        }
        val pixels = bitmap.toPixelMap()

        assertEquals(Color.Red, pixels[10, 10])
        assertEquals(Color.Transparent, pixels[1, 1])
    }

    // @spec CANVAS-PAINT-006
    @Test
    fun multiPointStrokeRendersAConnectingLine() {
        val bitmap = testDrawScope(width = 20, height = 20) {
            with(brush) {
                render(listOf(Point(0.1f, 0.5f), Point(0.9f, 0.5f)))
            }
        }
        val pixels = bitmap.toPixelMap()

        // Midpoint of the line is colored...
        assertEquals(Color.Red, pixels[10, 10])
        // ...but a corner well away from the line is untouched.
        assertEquals(Color.Transparent, pixels[1, 1])
    }

    // @spec CANVAS-PAINT-005
    @Test
    fun rendersCorrectlyWhenCalledRepeatedlyWithAGrowingPointList() {
        val points = mutableListOf(Point(0.1f, 0.5f))
        val bitmapAfterFirstPoint = testDrawScope(width = 20, height = 20) {
            with(brush) { render(points) }
        }
        points.add(Point(0.9f, 0.5f))
        val bitmapAfterSecondPoint = testDrawScope(width = 20, height = 20) {
            with(brush) { render(points) }
        }

        // Rendering the one-point list drew only a mark at that point, not a line...
        assertEquals(Color.Transparent, bitmapAfterFirstPoint.toPixelMap()[10, 10])
        // ...while rendering the full two-point list (as if called again after a
        // new point arrived) drew the connecting line, with no leftover state
        // from the earlier call needed to do so.
        assertEquals(Color.Red, bitmapAfterSecondPoint.toPixelMap()[10, 10])
    }

    // @spec CANVAS-PAINT-006
    @Test
    fun colorIsFixedAtConstruction() {
        val blueBrush = DefaultBrush(color = Color.Blue, strokeWidthPx = 4f)
        val bitmap = testDrawScope(width = 20, height = 20) {
            with(blueBrush) { render(listOf(Point(0.5f, 0.5f))) }
        }

        assertEquals(Color.Blue, bitmap.toPixelMap()[10, 10])
    }
}
