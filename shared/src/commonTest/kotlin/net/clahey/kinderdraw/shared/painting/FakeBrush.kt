package net.clahey.kinderdraw.shared.painting

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope

/** Records every render call it receives, instead of actually drawing anything. */
class FakeBrush : Brush {
    data class RenderCall(val points: List<Point>, val color: Color)

    private val mutableRenderCalls = mutableListOf<RenderCall>()
    val renderCalls: List<RenderCall> get() = mutableRenderCalls

    override fun DrawScope.render(points: List<Point>, color: Color) {
        mutableRenderCalls.add(RenderCall(points.toList(), color))
    }
}
