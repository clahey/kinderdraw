package net.clahey.kinderdraw.shared.paintingstyle

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import kotlin.test.Test
import kotlin.test.assertEquals

class PointTest {
    // @spec CANVAS-STYLE-015
    @Test
    fun centerPointConvertsToTheOffsetAtTheCanvasCenter() {
        val point = Point(0f, 0f)

        assertEquals(Offset(50f, 40f), point.toOffset(Size(100f, 80f)))
    }

    // @spec CANVAS-STYLE-015
    @Test
    fun convertsToAnOffsetScaledByTheShorterDimension() {
        // Size(200, 100): the shorter dimension is the height, 100.
        val point = Point(0.5f, 0.5f)

        assertEquals(Offset(150f, 100f), point.toOffset(Size(200f, 100f)))
    }

    // @spec CANVAS-STYLE-015
    @Test
    fun pointsLeftOfOrAboveCenterAreNegative() {
        val offset = Offset(10f, 10f)

        assertEquals(Point(-0.4f, -0.4f), offset.toPoint(Size(100f, 100f)))
    }

    // @spec CANVAS-STYLE-015
    @Test
    fun offsetToPointAndBackRoundTrips() {
        val point = Point(0.3f, -0.6f)
        val size = Size(320f, 480f)

        assertEquals(point, point.toOffset(size).toPoint(size))
    }

    // @spec CANVAS-STYLE-015
    @Test
    fun aPureRotationReproducesTheSameOffsetRelativeToCenterAlongTheUnchangedShorterDimension() {
        // A pure rotation swaps width and height without changing the shorter
        // dimension's value (200 here), so a point's offset relative to the
        // new center lands the same pixel distance from center either way.
        val point = Point(0.25f, 0f)
        val portrait = Size(200f, 300f)
        val landscape = Size(300f, 200f)

        val inPortrait = point.toOffset(portrait)
        val inLandscape = point.toOffset(landscape)

        assertEquals(50f, inPortrait.x - portrait.width / 2f)
        assertEquals(50f, inLandscape.x - landscape.width / 2f)
    }
}
