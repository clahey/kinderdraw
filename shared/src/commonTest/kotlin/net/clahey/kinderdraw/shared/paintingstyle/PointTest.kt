package net.clahey.kinderdraw.shared.paintingstyle

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

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
    fun aPureRotationLeavesAPointsOffsetFromCenterNumericallyUnchanged() {
        // A pure rotation swaps width and height without changing the shorter
        // dimension's value (200 here), so a point's offset from center, in
        // pixels, is the same number either way - though whether that offset
        // still falls inside the new bounds depends on which axis it's
        // measured against (see the test below).
        val point = Point(0.25f, 0f)
        val portrait = Size(200f, 300f)
        val landscape = Size(300f, 200f)

        val inPortrait = point.toOffset(portrait)
        val inLandscape = point.toOffset(landscape)

        assertEquals(50f, inPortrait.x - portrait.width / 2f)
        assertEquals(50f, inLandscape.x - landscape.width / 2f)
    }

    // @spec CANVAS-STYLE-015
    @Test
    fun aPointNearTheLongEdgeCanFallOutsideTheBoundsAfterAPureRotation() {
        // In portrait, y is measured against the longer dimension (300), so
        // this point sits safely inside the visible area. After rotating to
        // landscape, y is measured against the now-shorter dimension (200)
        // instead, pushing the very same point outside the new bounds - the
        // same overflow any other aspect-ratio-changing resize can cause.
        val point = Point(0f, 0.6f)
        val portrait = Size(200f, 300f)
        val landscape = Size(300f, 200f)

        val inPortrait = point.toOffset(portrait)
        val inLandscape = point.toOffset(landscape)

        assertTrue(inPortrait.y in 0f..portrait.height)
        assertTrue(inLandscape.y !in 0f..landscape.height)
    }
}
