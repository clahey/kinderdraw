package net.clahey.kinderdraw.shared.painting

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size

/**
 * A captured point, as a fraction of the drawing surface's width/height at
 * capture time — see the Painting LLD's Stroke Model. Converted to a pixel
 * [Offset] only at render time, against the drawing surface's current size.
 */
data class Point(val xFraction: Float, val yFraction: Float)

// @spec CANVAS-PAINT-014
fun Point.toOffset(size: Size): Offset = Offset(xFraction * size.width, yFraction * size.height)
