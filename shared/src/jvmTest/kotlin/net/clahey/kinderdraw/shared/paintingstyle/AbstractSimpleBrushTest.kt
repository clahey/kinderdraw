package net.clahey.kinderdraw.shared.paintingstyle

import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.test.Test
import kotlin.test.assertEquals
import net.clahey.kinderdraw.shared.painting.Point
import net.clahey.kinderdraw.shared.painting.testDrawScope

/** Records the point list passed to [render] on each call, instead of actually drawing anything. */
private class RecordingSimpleBrush : AbstractSimpleBrush() {
    val renderCalls = mutableListOf<List<Point>>()

    override fun DrawScope.render(points: List<Point>) {
        renderCalls.add(points)
    }
}

class AbstractSimpleBrushTest {
    // @spec CANVAS-STYLE-001
    @Test
    fun addPointExtendsWhatTheStrokeRenders() {
        val brush = RecordingSimpleBrush()
        val stroke = brush.startStroke(Point(0.1f, 0.5f))
        testDrawScope { with(stroke) { render() } }

        stroke.addPoint(Point(0.9f, 0.5f))
        testDrawScope { with(stroke) { render() } }

        assertEquals(
            listOf(listOf(Point(0.1f, 0.5f)), listOf(Point(0.1f, 0.5f), Point(0.9f, 0.5f))),
            brush.renderCalls,
        )
    }

    // @spec CANVAS-STYLE-011
    @Test
    fun restartProducesAStrokeContinuingFromTheLastPointWithTheSameBrush() {
        val brush = RecordingSimpleBrush()
        val stroke = brush.startStroke(Point(0.1f, 0.5f))
        stroke.addPoint(Point(0.5f, 0.5f))

        val restarted = stroke.restart()
        restarted.addPoint(Point(0.9f, 0.5f))
        testDrawScope { with(restarted) { render() } }

        // Continues from the interrupted stroke's last point, not its first,
        // and the render call lands on the same brush's recorder — proving
        // restart() carried the same brush forward.
        assertEquals(listOf(Point(0.5f, 0.5f), Point(0.9f, 0.5f)), brush.renderCalls.single())
    }
}
