package net.clahey.kinderdraw.shared.paintingstyle

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope

/**
 * Today's only [Brush]: a fixed-width solid line connecting points as a
 * polyline, with no curve-fitting or smoothing — see this LLD's Brushes
 * section. Holds whatever [ColorSource] it's constructed with; color is
 * resolved per stroke, not per instance (CANVAS-STYLE-012).
 */
class DefaultBrush(
    colorSource: ColorSource,
    private val strokeWidthPx: Float = DEFAULT_STROKE_WIDTH_PX,
) : AbstractSimpleBrush(colorSource) {
    // @spec CANVAS-STYLE-002
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
