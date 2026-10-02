package net.clahey.kinderdraw.shared.painting

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.mandatorySystemGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.changedToDownIgnoreConsumed
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.toSize
import net.clahey.kinderdraw.shared.paintingstyle.toPoint
import net.clahey.kinderdraw.shared.userexperience.InteractionLock
import net.clahey.kinderdraw.shared.userexperience.swallowGesture

/**
 * Owns pointer input for a drawing — see the Painting LLD's Composable
 * Shape. Converts whatever pointer stream Compose delivers to it into calls
 * against [state]. It takes [lock] for the span of its own gesture: a refusal
 * means some other component is mid-gesture, and Painting simply starts
 * nothing (see the Painting LLD's Holding the Lock).
 *
 * [reservedInsets] is what the platform reserves for its own gesture
 * navigation; a stroke doesn't start inside any edge it reserves (see the
 * Painting LLD's System Gesture Coexistence). A caller wanting a different
 * region — a test, on a host that reserves nothing — passes its own.
 */
@Composable
fun Painting(
    state: PaintingState,
    lock: InteractionLock,
    modifier: Modifier = Modifier,
    reservedInsets: WindowInsets = WindowInsets.mandatorySystemGestures,
) {
    // Read through a State so the gesture loop always sees the current strip
    // without restarting — which mid-stroke would cancel the gesture and drop
    // the hold with it.
    val currentInsets by rememberUpdatedState(reservedInsets)
    // An inset's left and right are read with respect to this; the pointer
    // scope carries a density but no layout direction.
    val layoutDirection = LocalLayoutDirection.current
    Canvas(
        modifier = modifier
            .pointerInput(state, lock) {
                awaitEachGesture {
                    // One gesture spans however many pointers are
                    // concurrently down — see the Painting LLD's Composable
                    // Shape — and one hold covers all of it.
                    val trackedPointers = mutableSetOf<PointerId>()
                    // Pointers that touched down inside an edge the platform
                    // reserves. They draw nothing, but still count towards the
                    // gesture above, so this loop sees each of their lifts.
                    val suppressedPointers = mutableSetOf<PointerId>()
                    var hold: InteractionLock.Hold? = null
                    try {
                        do {
                            val event = awaitPointerEvent()
                            // Where a stroke starts is what's tested, so a stroke
                            // already under way draws on through a reserved edge.
                            // @spec CANVAS-PAINT-026, CANVAS-PAINT-029
                            // @spec CANVAS-PAINT-030, CANVAS-PAINT-031
                            for (change in event.changes) {
                                val reserved = currentInsets.reserves(
                                    change.position,
                                    size,
                                    this,
                                    layoutDirection,
                                )
                                if (change.changedToDownIgnoreConsumed() && reserved) {
                                    suppressedPointers += change.id
                                }
                            }
                            // Only a touch-down starts a gesture worth asking
                            // about: a hovering pointer must never take the
                            // lock, and a pointer joining a gesture already
                            // held needs no second request.
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
                                // A suppressed pointer still bounds the gesture,
                                // so it joins down and up as any pointer does; it
                                // just reaches no stroke and stays unconsumed.
                                // @spec CANVAS-PAINT-026, CANVAS-PAINT-027
                                val suppressed = change.id in suppressedPointers
                                when {
                                    change.changedToDownIgnoreConsumed() -> {
                                        down += change.id
                                        if (!suppressed) {
                                            change.consume()
                                            state.onPointerDown(change.id, change.position.toPoint(size.toSize()))
                                        }
                                    }
                                    change.changedToUpIgnoreConsumed() -> {
                                        up += change.id
                                        if (suppressed) {
                                            suppressedPointers -= change.id
                                        } else {
                                            change.consume()
                                            state.onPointerUp(change.id)
                                        }
                                    }
                                    // A pointer that isn't pressed and didn't
                                    // just lift is hovering — not a stroke,
                                    // and not ours to consume.
                                    !change.pressed -> Unit
                                    else -> {
                                        if (!suppressed) {
                                            change.consume()
                                            state.onPointerMove(change.id, change.position.toPoint(size.toSize()))
                                        }
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
