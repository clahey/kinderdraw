package net.clahey.kinderdraw.shared.painting

import androidx.compose.ui.graphics.drawscope.DrawScope

/** Records every render call any stroke it started receives, instead of actually drawing anything. */
class FakeBrush : Brush {
    private val mutableRenderCalls = mutableListOf<List<Point>>()
    val renderCalls: List<List<Point>> get() = mutableRenderCalls

    override fun startStroke(point: Point): Stroke = FakeStroke(this).apply { addPoint(point) }

    private class FakeStroke(private val brush: FakeBrush) : Stroke {
        private val mutablePoints = mutableListOf<Point>()

        override fun addPoint(point: Point) {
            mutablePoints.add(point)
        }

        override fun DrawScope.render() {
            brush.mutableRenderCalls.add(mutablePoints.toList())
        }

        override fun restart(): Stroke = brush.startStroke(mutablePoints.last())
    }
}
