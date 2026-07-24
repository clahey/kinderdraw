package net.clahey.kinderdraw.shared.painting

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import kotlin.test.Test
import kotlin.test.assertEquals

class PointTest {
    // @spec CANVAS-PAINT-014
    @Test
    fun convertsToAPixelOffsetProportionalToTheGivenSize() {
        val point = Point(xFraction = 0.25f, yFraction = 0.75f)

        assertEquals(Offset(25f, 75f), point.toOffset(Size(100f, 100f)))
    }

    // @spec CANVAS-PAINT-014
    @Test
    fun sameFractionalPointConvertsToADifferentPixelOffsetAfterAProportionalResize() {
        val point = Point(xFraction = 0.5f, yFraction = 0.5f)

        val beforeResize = point.toOffset(Size(100f, 200f))
        val afterResize = point.toOffset(Size(200f, 400f))

        assertEquals(Offset(50f, 100f), beforeResize)
        assertEquals(Offset(100f, 200f), afterResize)
        // Same relative position (the center) both times.
        assertEquals(0.5f, beforeResize.x / 100f)
        assertEquals(0.5f, afterResize.x / 200f)
    }
}
