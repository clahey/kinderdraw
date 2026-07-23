package net.clahey.kinderdraw.shared.painting

import androidx.compose.ui.graphics.drawscope.DrawScope

/** Records every render call it receives, instead of actually drawing anything. */
class FakeBrush : Brush {
    private val mutableRenderCalls = mutableListOf<List<Point>>()
    val renderCalls: List<List<Point>> get() = mutableRenderCalls

    override fun DrawScope.render(points: List<Point>) {
        mutableRenderCalls.add(points.toList())
    }
}
