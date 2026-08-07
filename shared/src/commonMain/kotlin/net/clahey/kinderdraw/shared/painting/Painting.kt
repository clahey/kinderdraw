package net.clahey.kinderdraw.shared.painting

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.changedToDownIgnoreConsumed
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.toSize
import net.clahey.kinderdraw.shared.paintingstyle.toPoint

/**
 * Owns pointer input for a drawing — see the Painting LLD's Composable
 * Shape. Converts whatever pointer stream Compose delivers to it into
 * calls against [state]; reports whether it's currently holding any live
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
                    // One gesture now spans however many pointers are
                    // concurrently down — see the Painting LLD's Composable
                    // Shape — rather than exactly one.
                    val trackedPointers = mutableSetOf<PointerId>()
                    do {
                        val event = awaitPointerEvent()
                        val down = mutableListOf<PointerId>()
                        val up = mutableListOf<PointerId>()
                        for (change in event.changes) {
                            when {
                                change.changedToDownIgnoreConsumed() -> {
                                    change.consume()
                                    down += change.id
                                    state.onPointerDown(change.id, change.position.toPoint(size.toSize()))
                                }
                                change.changedToUpIgnoreConsumed() -> {
                                    change.consume()
                                    up += change.id
                                    state.onPointerUp(change.id)
                                }
                                else -> {
                                    change.consume()
                                    state.onPointerMove(change.id, change.position.toPoint(size.toSize()))
                                }
                            }
                        }
                        // @spec CANVAS-PAINT-018
                        when (applyGestureChanges(trackedPointers, down, up)) {
                            GestureEdge.STARTED -> onStrokeActiveChange(true)
                            GestureEdge.ENDED -> onStrokeActiveChange(false)
                            GestureEdge.UNCHANGED -> {}
                        }
                    } while (trackedPointers.isNotEmpty())
                }
            },
    ) {
        with(state) { render() }
    }
}
