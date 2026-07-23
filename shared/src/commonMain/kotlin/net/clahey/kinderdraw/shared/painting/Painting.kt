package net.clahey.kinderdraw.shared.painting

import androidx.compose.ui.graphics.drawscope.DrawScope

/**
 * Converts a pointer stream into stroke data and renders it — see the
 * Painting LLD. Receives one pointer's down/move/up sequence at a time;
 * arbitrating which pointer reaches Painting is User Experience's job.
 */
class Painting {
    private val completedStrokes = mutableListOf<Stroke>()
    private var liveStroke: Stroke? = null

    // @spec CANVAS-PAINT-001, CANVAS-PAINT-002
    fun onPointerDown(point: Point, activeStrokeSettings: ActiveStrokeSettings) {
        liveStroke = activeStrokeSettings.getResolvedBrush().startStroke(point)
    }

    fun onPointerMove(point: Point) {
        liveStroke?.addPoint(point)
    }

    // @spec CANVAS-PAINT-003
    fun onPointerUp() {
        liveStroke?.let { completedStrokes.add(it) }
        liveStroke = null
    }

    // @spec CANVAS-PAINT-008
    fun isEmpty(): Boolean = completedStrokes.isEmpty() && liveStroke == null

    // @spec CANVAS-PAINT-010, CANVAS-PAINT-013
    fun clear() {
        val interrupted = liveStroke
        completedStrokes.clear()
        liveStroke = interrupted?.restart()
    }

    // @spec CANVAS-PAINT-004, CANVAS-PAINT-007
    fun DrawScope.render() {
        for (stroke in completedStrokes) {
            with(stroke) { render() }
        }
        liveStroke?.let { stroke -> with(stroke) { render() } }
    }
}
