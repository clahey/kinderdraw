package net.clahey.kinderdraw.shared.painting

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope

/**
 * Today's only [Brush]: a fixed-width solid line connecting points as a
 * polyline, with no curve-fitting or smoothing — see the Painting LLD's
 * Brushes section.
 */
class DefaultBrush(private val strokeWidthPx: Float = DEFAULT_STROKE_WIDTH_PX) : Brush {
    // @spec CANVAS-PAINT-005, CANVAS-PAINT-006
    override fun DrawScope.render(points: List<Point>, color: Color) {
        val pixelPoints = points.map { it.toOffset(size) }
        if (pixelPoints.size == 1) {
            drawCircle(color = color, radius = strokeWidthPx / 2f, center = pixelPoints.first())
        } else {
            drawPoints(
                points = pixelPoints,
                pointMode = PointMode.Polygon,
                color = color,
                strokeWidth = strokeWidthPx,
                cap = StrokeCap.Round,
            )
        }
    }

    companion object {
        const val DEFAULT_STROKE_WIDTH_PX = 12f
    }
}
