package net.clahey.kinderdraw.shared.painting

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import net.clahey.kinderdraw.shared.imagestorage.ImageStorage
import net.clahey.kinderdraw.shared.paintingstyle.Stroke
import net.clahey.kinderdraw.shared.paintingstyle.StyleSettings

/**
 * A drawing's Compose-observable state — see the Painting LLD's Composable
 * Shape. Holds the completed strokes and any live stroke; a stroke's own
 * captured points are Compose-observable within the stroke implementation
 * itself (see the Painting Style LLD's Decisions & Alternatives). Pointer
 * events are forwarded into this state by the [Painting] composable, which
 * owns the actual pointer input.
 */
class PaintingState(private val styleSettings: StyleSettings) {
    private val completedStrokes = mutableStateListOf<Stroke>()
    private var liveStroke by mutableStateOf<Stroke?>(null)
    private var lastRenderSize: Size = Size.Zero

    // @spec CANVAS-PAINT-016
    private var background: Color = styleSettings.getResolvedBackground()

    // @spec CANVAS-PAINT-001, CANVAS-PAINT-002
    fun onPointerDown(point: Point) {
        liveStroke = styleSettings.getResolvedBrush().startStroke(point)
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

    // @spec CANVAS-PAINT-009, CANVAS-PAINT-012, CANVAS-PAINT-017
    suspend fun save(imageStorage: ImageStorage, id: String? = null): Result<String> {
        val image = rasterize()
        val result = if (id == null) {
            imageStorage.create(image)
        } else {
            imageStorage.update(id, image)
        }
        return result.map { it.id }
    }

    // @spec CANVAS-PAINT-010, CANVAS-PAINT-013, CANVAS-PAINT-016
    fun clear() {
        val interrupted = liveStroke
        completedStrokes.clear()
        liveStroke = interrupted?.restart()
        background = styleSettings.getResolvedBackground()
    }

    // @spec CANVAS-PAINT-004, CANVAS-PAINT-007, CANVAS-PAINT-016
    fun DrawScope.render() {
        lastRenderSize = size
        drawRect(color = background, size = size)
        for (stroke in completedStrokes) {
            with(stroke) { render() }
        }
        liveStroke?.let { stroke -> with(stroke) { render() } }
    }

    /** Rasterizes the drawing off-screen, at the size last seen in [render] — see the Painting LLD's Save and Clear. */
    private fun rasterize(): ImageBitmap {
        val width = lastRenderSize.width.toInt().coerceAtLeast(1)
        val height = lastRenderSize.height.toInt().coerceAtLeast(1)
        val image = ImageBitmap(width, height)
        CanvasDrawScope().draw(
            Density(1f),
            LayoutDirection.Ltr,
            Canvas(image),
            Size(width.toFloat(), height.toFloat()),
        ) {
            with(this@PaintingState) { render() }
        }
        return image
    }
}
