package net.clahey.kinderdraw.shared.painting

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.toSize

/**
 * Owns pointer input for a drawing — see the Painting LLD's Composable
 * Shape. Converts whatever pointer stream Compose delivers to it into
 * calls against [state]; reports whether it's currently holding a live
 * stroke via [onStrokeActiveChange], mirroring the Widgets LLD's
 * `onPressedChange` (see the Painting LLD's Reporting Stroke State).
 */
@Composable
fun Painting(
    state: PaintingState,
    modifier: Modifier = Modifier,
    onStrokeActiveChange: (Boolean) -> Unit = {},
) {
    Canvas(
        modifier = modifier
            .pointerInput(state) {
                awaitEachGesture {
                    // requireUnconsumed defaults to true: a touch some
                    // other component (e.g. a Widgets control) already
                    // consumed never starts a stroke here.
                    val down = awaitFirstDown()
                    state.onPointerDown(down.position.toPoint(size.toSize()))
                    // @spec CANVAS-PAINT-018
                    onStrokeActiveChange(true)

                    val pointerId = down.id
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == pointerId } ?: break
                        change.consume()
                        if (change.changedToUpIgnoreConsumed()) {
                            state.onPointerUp()
                            // @spec CANVAS-PAINT-018
                            onStrokeActiveChange(false)
                            break
                        }
                        state.onPointerMove(change.position.toPoint(size.toSize()))
                    }
                }
            },
    ) {
        with(state) { render() }
    }
}
