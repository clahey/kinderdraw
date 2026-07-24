package net.clahey.kinderdraw.shared.painting

import androidx.compose.ui.graphics.drawscope.DrawScope

/**
 * Base [Brush] for the common case: a stroke that's just a flat, ordered
 * point list, rendered by a single function seeded with every point
 * captured so far — see the Painting LLD's Brushes section.
 */
abstract class AbstractSimpleBrush : Brush {
    abstract fun DrawScope.render(points: List<Point>)

    override fun startStroke(point: Point): Stroke = SimpleStroke(this).apply { addPoint(point) }

    private class SimpleStroke(private val brush: AbstractSimpleBrush) : Stroke {
        private val mutablePoints = mutableListOf<Point>()

        override fun addPoint(point: Point) {
            mutablePoints.add(point)
        }

        override fun DrawScope.render() {
            with(brush) { render(mutablePoints.toList()) }
        }

        override fun restart(): Stroke = brush.startStroke(mutablePoints.last())
    }
}
