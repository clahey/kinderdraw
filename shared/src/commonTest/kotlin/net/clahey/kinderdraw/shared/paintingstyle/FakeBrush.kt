package net.clahey.kinderdraw.shared.paintingstyle

import androidx.compose.ui.graphics.drawscope.DrawScope

/** Records every render call any stroke it started receives, instead of actually drawing anything. */
class FakeBrush : Brush {
    private val mutableRenderCalls = mutableListOf<List<Point>>()
    val renderCalls: List<List<Point>> get() = mutableRenderCalls

    override fun startStroke(point: Point): Stroke = FakeStroke(this, initialPoints = listOf(point))

    override fun restore(saved: Map<String, Any?>): Stroke {
        @Suppress("UNCHECKED_CAST")
        val points = saved.getValue("points") as List<Point>
        return FakeStroke(this, initialPoints = points)
    }

    private class FakeStroke(private val brush: FakeBrush, initialPoints: List<Point>) : Stroke {
        private val mutablePoints = mutableListOf<Point>().apply { addAll(initialPoints) }

        override fun addPoint(point: Point) {
            mutablePoints.add(point)
        }

        override fun DrawScope.render() {
            brush.mutableRenderCalls.add(mutablePoints.toList())
        }

        override fun restart(): Stroke = brush.startStroke(mutablePoints.last())

        override fun save(): Map<String, Any?> = mapOf("points" to mutablePoints.toList())
    }
}
