package net.clahey.kinderdraw.shared.widgets

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import kotlinx.coroutines.launch
import kotlinx.coroutines.launch
import net.clahey.kinderdraw.shared.userexperience.InteractionLock
import net.clahey.kinderdraw.shared.userexperience.swallowGesture

private val MIN_TOUCH_TARGET_DP = 64.dp

/**
 * KidWidgets' base control — see the Widgets LLD's Hit-Testing and
 * Activation. Reads raw pointer events directly via [PressState] rather
 * than `Modifier.clickable`'s adult-tuned, zero-tolerance release
 * semantics. [content] is given the current pressed state so a caller can
 * render its own visual feedback (see the LLD's Open Questions — exact
 * appearance isn't fixed here); the press state goes nowhere else.
 *
 * The control asks [lock] for the interaction before claiming a pointer and
 * holds it until its own gesture is over — through [onActivate]'s own work
 * for a release that activates (see the LLD's Interaction Arbitration
 * Contract). Its hit region is also registered with the platform as excluded
 * from system gesture navigation (see the LLD's System Gesture Coexistence),
 * regardless of where a caller places it.
 */
@Composable
fun KidButton(
    onActivate: suspend () -> Unit,
    lock: InteractionLock,
    modifier: Modifier = Modifier,
    content: @Composable (pressed: Boolean) -> Unit,
) {
    var pressed by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    // Keyed on lock alone below, so a caller's freshly-allocated onActivate
    // lambda can't restart pointer input — and cancel a running activation —
    // on every recomposition.
    val currentOnActivate by rememberUpdatedState(onActivate)

    Box(
        modifier = modifier
            .defaultMinSize(minWidth = MIN_TOUCH_TARGET_DP, minHeight = MIN_TOUCH_TARGET_DP)
            .excludeFromSystemGestures()
            .pointerInput(lock) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    // @spec CANVAS-WIDGETS-018, CANVAS-WIDGETS-019
                    val hold = lock.tryAcquire()
                    if (hold == null) {
                        down.consume()
                        swallowGesture()
                        return@awaitEachGesture
                    }

                    var activated = false
                    val pressState = PressState(
                        onPressedChange = { pressed = it },
                        onActivate = { activated = true },
                    )
                    var releaseOnExit = true
                    try {
                        val bounds = Rect(Offset.Zero, size.toSize())
                        pressState.onClaim(now = down.uptimeMillis)
                        while (true) {
                            val event = awaitPointerEvent()
                            // @spec CANVAS-WIDGETS-020
                            val change = event.changes.firstOrNull { it.id == down.id } ?: continue
                            if (!change.pressed) {
                                // Compose signals a cancelled gesture by
                                // delivering the change already consumed, which
                                // is what `changedToUp` excludes. By timing
                                // alone the two are identical, so the activation
                                // rule only ever sees a genuine lift.
                                // @spec CANVAS-WIDGETS-027
                                if (change.changedToUp()) {
                                    pressState.onRelease(now = change.uptimeMillis)
                                } else {
                                    pressState.onCancel()
                                }
                                break
                            }
                            pressState.onPositionChanged(bounds.contains(change.position), now = change.uptimeMillis)
                        }
                        // The activation runs in the composition's scope, not
                        // this pointer-event one, and keeps the interaction
                        // until it finishes however it finishes.
                        // @spec CANVAS-WIDGETS-022, CANVAS-WIDGETS-024
                        if (activated) {
                            releaseOnExit = false
                            // Released from the job's completion rather than
                            // from inside it: a coroutine whose scope is
                            // already cancelled never runs its body at all,
                            // and a `finally` in a body that never ran would
                            // strand the interaction.
                            scope.launch { currentOnActivate() }
                                .invokeOnCompletion { hold.release() }
                        }
                    } finally {
                        // @spec CANVAS-WIDGETS-021, CANVAS-WIDGETS-025
                        if (releaseOnExit) hold.release()
                    }
                }
            },
    ) {
        content(pressed)
    }
}
