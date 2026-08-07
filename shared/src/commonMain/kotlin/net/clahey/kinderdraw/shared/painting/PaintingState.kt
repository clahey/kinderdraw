package net.clahey.kinderdraw.shared.painting

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import net.clahey.kinderdraw.shared.imagestorage.ImageStorage
import net.clahey.kinderdraw.shared.paintingstyle.Point
import net.clahey.kinderdraw.shared.paintingstyle.Stroke
import net.clahey.kinderdraw.shared.paintingstyle.StyleSettings

/**
 * A drawing's Compose-observable state — see the Painting LLD's Composable
 * Shape. Holds the completed strokes and every currently-live stroke, keyed
 * by its own originating pointer so several pointers can each hold their own
 * independent live stroke at once; a stroke's own captured points are
 * Compose-observable within the stroke implementation itself (see the
 * Painting Style LLD's Decisions & Alternatives). Pointer events are
 * forwarded into this state by the [Painting] composable, which owns the
 * actual pointer input.
 *
 * [savedState] exists only for [paintingStateSaver]'s restore path —
 * ordinary construction leaves it unset, resolving the background from
 * [styleSettings] and starting with no strokes, exactly as if the
 * parameter didn't exist (see the Painting LLD's Lifecycle Survival).
 */
class PaintingState(
    private val styleSettings: StyleSettings,
    savedState: Map<String, Any?>? = null,
) {
    private val completedStrokes = mutableStateListOf<Stroke>().apply {
        val savedStrokes = savedState?.getValue("strokes") as List<Map<String, Any?>>?
        savedStrokes?.forEach { add(styleSettings.getActiveBrush().restore(it)) }
    }
    private val liveStrokes = mutableStateMapOf<Any, Stroke>()
    private var lastRenderSize: Size = Size.Zero

    // @spec CANVAS-PAINT-016, CANVAS-PAINT-019
    private var background: Color = savedState
        ?.let { Color(it.getValue("background") as Int) }
        ?: styleSettings.getActiveBackground().getNextColor()

    // @spec CANVAS-PAINT-001, CANVAS-PAINT-002, CANVAS-PAINT-020
    fun onPointerDown(pointerId: Any, point: Point) {
        liveStrokes[pointerId] = styleSettings.getActiveBrush().startStroke(point)
    }

    // @spec CANVAS-PAINT-020
    fun onPointerMove(pointerId: Any, point: Point) {
        liveStrokes[pointerId]?.addPoint(point)
    }

    // @spec CANVAS-PAINT-003, CANVAS-PAINT-020
    fun onPointerUp(pointerId: Any) {
        liveStrokes.remove(pointerId)?.let { completedStrokes.add(it) }
    }

    // @spec CANVAS-PAINT-008
    fun isEmpty(): Boolean = completedStrokes.isEmpty() && liveStrokes.isEmpty()

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
        val interrupted = liveStrokes.toMap()
        completedStrokes.clear()
        liveStrokes.clear()
        interrupted.forEach { (pointerId, stroke) -> liveStrokes[pointerId] = stroke.restart() }
        background = styleSettings.getActiveBackground().getNextColor()
    }

    // @spec CANVAS-PAINT-004, CANVAS-PAINT-007, CANVAS-PAINT-016
    fun DrawScope.render() {
        lastRenderSize = size
        drawRect(color = background, size = size)
        for (stroke in completedStrokes) {
            with(stroke) { render() }
        }
        for (stroke in liveStrokes.values) {
            with(stroke) { render() }
        }
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

    /**
     * This drawing's state as an opaque, self-describing map — see the
     * Painting LLD's Lifecycle Survival. Every completed stroke is
     * included, plus every currently-live stroke too, each via its own
     * [Stroke.save]; passing the result back into the constructor as
     * [savedState] reconstructs an equivalent [PaintingState].
     */
    fun toSavedState(): Map<String, Any?> = mapOf(
        "background" to background.toArgb(),
        "strokes" to (completedStrokes + liveStrokes.values).map { it.save() },
    )
}

/**
 * Builds the [Saver] that lets [PaintingState] survive an OS-driven
 * recreation via `rememberSaveable` — see the Painting LLD's Lifecycle
 * Survival (CANVAS-PAINT-011, CANVAS-PAINT-019). [styleSettings] is
 * captured by closure since [Saver.restore] is a pure function of only the
 * saved data, but reconstructing [PaintingState] needs a live reference.
 */
fun paintingStateSaver(styleSettings: StyleSettings): Saver<PaintingState, Map<String, Any?>> = Saver(
    save = { state -> state.toSavedState() },
    restore = { saved -> PaintingState(styleSettings, savedState = saved) },
)
