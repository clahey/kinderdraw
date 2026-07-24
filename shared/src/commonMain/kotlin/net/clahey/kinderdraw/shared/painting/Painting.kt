package net.clahey.kinderdraw.shared.painting

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import net.clahey.kinderdraw.shared.imagestorage.ImageStorage

/**
 * Converts a pointer stream into stroke data and renders it — see the
 * Painting LLD. Receives one pointer's down/move/up sequence at a time;
 * arbitrating which pointer reaches Painting is User Experience's job.
 */
class Painting {
    private val completedStrokes = mutableListOf<Stroke>()
    private var liveStroke: Stroke? = null
    private var lastRenderSize: Size = Size.Zero

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

    // @spec CANVAS-PAINT-009, CANVAS-PAINT-012
    suspend fun save(imageStorage: ImageStorage): Result<Unit> =
        imageStorage.create(rasterize()).map { }

    // @spec CANVAS-PAINT-010, CANVAS-PAINT-013
    fun clear() {
        val interrupted = liveStroke
        completedStrokes.clear()
        liveStroke = interrupted?.restart()
    }

    // @spec CANVAS-PAINT-004, CANVAS-PAINT-007
    fun DrawScope.render() {
        lastRenderSize = size
        for (stroke in completedStrokes) {
            with(stroke) { render() }
        }
        liveStroke?.let { stroke -> with(stroke) { render() } }
    }

    /** Rasterizes the drawing off-screen, at the size last seen in [render] — see the Painting LLD's Save and Clear. */
    private fun rasterize(): ImageBitmap {
        val width = lastRenderSize.width.toInt()
        val height = lastRenderSize.height.toInt()
        val image = ImageBitmap(width, height)
        CanvasDrawScope().draw(
            Density(1f),
            LayoutDirection.Ltr,
            Canvas(image),
            Size(width.toFloat(), height.toFloat()),
        ) {
            with(this@Painting) { render() }
        }
        return image
    }
}
