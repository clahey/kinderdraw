package net.clahey.kinderdraw.shared.paintingstyle

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.toArgb

/**
 * Base [Brush] for the common case: a stroke that's just a flat, ordered
 * point list, rendered by a single function seeded with every point
 * captured so far and the stroke's own resolved color — see this LLD's
 * Brushes section. A stroke's points are held as Compose-observable state
 * so [DrawScope.render] redraws as each new point arrives (see the
 * Painting LLD's Composable Shape).
 */
abstract class AbstractSimpleBrush(private val colorSource: ColorSource) : Brush {
    abstract fun DrawScope.render(points: List<Point>, color: Color)

    // @spec CANVAS-STYLE-012
    override fun startStroke(point: Point): Stroke {
        val color = colorSource.getNextColor()
        return SimpleStroke(this, color, initialPoints = listOf(point))
    }

    // @spec CANVAS-STYLE-017
    override fun restore(saved: Map<String, Any?>): Stroke {
        val color = Color(saved.getValue("color") as Int)
        val flatPoints = saved.getValue("points") as List<Float>
        val points = flatPoints.chunked(2).map { (x, y) -> Point(x, y) }
        return SimpleStroke(this, color, initialPoints = points)
    }

    private class SimpleStroke(
        private val brush: AbstractSimpleBrush,
        private val color: Color,
        initialPoints: List<Point>,
    ) : Stroke {
        private val mutablePoints = mutableStateListOf<Point>().apply { addAll(initialPoints) }

        // @spec CANVAS-STYLE-001
        override fun addPoint(point: Point) {
            mutablePoints.add(point)
        }

        override fun DrawScope.render() {
            with(brush) { render(mutablePoints.toList(), color) }
        }

        // @spec CANVAS-STYLE-011
        override fun restart(): Stroke = SimpleStroke(brush, color, initialPoints = listOf(mutablePoints.last()))

        // @spec CANVAS-STYLE-016
        override fun save(): Map<String, Any?> = mapOf(
            "color" to color.toArgb(),
            "points" to mutablePoints.flatMap { listOf(it.x, it.y) },
        )
    }
}
