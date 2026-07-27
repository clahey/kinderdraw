package net.clahey.kinderdraw.shared.paintingstyle

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import kotlin.math.min

/**
 * A captured point, relative to the drawing surface's center and scaled by
 * its shorter dimension at capture time — see the Painting Style LLD's
 * Point section. Signed, since a point left of or above center is negative.
 * Converted to a pixel [Offset] only at render time, against the drawing
 * surface's current size.
 */
data class Point(val x: Float, val y: Float)

// @spec CANVAS-STYLE-015
fun Point.toOffset(size: Size): Offset {
    val shorterDimension = min(size.width, size.height)
    return Offset(size.width / 2f + x * shorterDimension, size.height / 2f + y * shorterDimension)
}

fun Offset.toPoint(size: Size): Point {
    val shorterDimension = min(size.width, size.height)
    return Point(
        x = (x - size.width / 2f) / shorterDimension,
        y = (y - size.height / 2f) / shorterDimension,
    )
}
