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
import net.clahey.kinderdraw.shared.userexperience.InteractionLock
import net.clahey.kinderdraw.shared.userexperience.swallowGesture

/**
 * Owns pointer input for a drawing — see the Painting LLD's Composable
 * Shape. Converts whatever pointer stream Compose delivers to it into calls
 * against [state]. It takes [lock] for the span of its own gesture and
 * observes nothing else about the screen: a refusal means some other
 * component is mid-gesture, and Painting simply starts nothing (see the
 * Painting LLD's Holding the Interaction).
 */
@Composable
fun Painting(
    state: PaintingState,
    lock: InteractionLock,
    modifier: Modifier = Modifier,
) {
    Canvas(
        modifier = modifier
            .pointerInput(state, lock) {
                awaitEachGesture {
                    // One gesture spans however many pointers are
                    // concurrently down — see the Painting LLD's Composable
                    // Shape — and one hold covers all of it.
                    val trackedPointers = mutableSetOf<PointerId>()
                    var hold: InteractionLock.Hold? = null
                    try {
                        do {
                            val event = awaitPointerEvent()
                            // Only a touch-down starts a gesture worth asking
                            // about: a hovering pointer must never take the
                            // interaction, and a pointer joining a gesture
                            // already held needs no second request.
                            // @spec CANVAS-PAINT-018, CANVAS-PAINT-022
                            if (hold == null && event.changes.any { it.changedToDownIgnoreConsumed() }) {
                                hold = lock.tryAcquire()
                                if (hold == null) {
                                    event.changes.forEach { it.consume() }
                                    swallowGesture()
                                    return@awaitEachGesture
                                }
                            }
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
                                    // A pointer that isn't pressed and didn't
                                    // just lift is hovering — not a stroke,
                                    // and not ours to consume.
                                    !change.pressed -> Unit
                                    else -> {
                                        change.consume()
                                        state.onPointerMove(change.id, change.position.toPoint(size.toSize()))
                                    }
                                }
                            }
                        } while (applyGestureChanges(trackedPointers, down, up))
                    } finally {
                        // @spec CANVAS-PAINT-023
                        hold?.release()
                    }
                }
            },
    ) {
        with(state) { render() }
    }
}
