package net.clahey.kinderdraw.shared.userexperience

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.toSize
import net.clahey.kinderdraw.shared.painting.Painting
import net.clahey.kinderdraw.shared.painting.toPoint

/**
 * The kid canvas screen's current minimal slice: a full-bleed drawing
 * surface with no Widgets chrome yet (see the User Experience LLD's Open
 * Questions on deferred Widgets/Config wiring). [Painting] doesn't observe
 * Compose state internally (see its LLD's Open Questions #15), so this
 * redraws by bumping [redrawTrigger] on every pointer event and reading it
 * inside the [Canvas]'s own draw scope — a stopgap until Painting hoists
 * its own state.
 */
@Composable
fun KidCanvasScreen(painting: Painting = remember { Painting(DefaultActiveStrokeSettings()) }) {
    var redrawTrigger by remember { mutableIntStateOf(0) }

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(painting) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    painting.onPointerDown(down.position.toPoint(size.toSize()))
                    redrawTrigger++

                    val pointerId = down.id
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == pointerId } ?: break
                        change.consume()
                        if (change.changedToUpIgnoreConsumed()) {
                            painting.onPointerUp()
                            redrawTrigger++
                            break
                        }
                        painting.onPointerMove(change.position.toPoint(size.toSize()))
                        redrawTrigger++
                    }
                }
            },
    ) {
        // Reading redrawTrigger here is what ties this draw scope to it.
        redrawTrigger
        with(painting) { render() }
    }
}
