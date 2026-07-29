package net.clahey.kinderdraw.shared.widgets

import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize

private val MIN_TOUCH_TARGET_DP = 64.dp

/**
 * KidWidgets' base control — see the Widgets LLD's Hit-Testing and
 * Activation. Reads raw pointer events directly via [PressState] rather
 * than `Modifier.clickable`'s adult-tuned, zero-tolerance release
 * semantics. [content] is given the current pressed state so a caller can
 * render its own visual feedback (see the LLD's Open Questions — exact
 * appearance isn't fixed here); [KidButton] itself only owns hit-testing,
 * timing, and callback dispatch. Its hit region is also registered with the
 * platform as excluded from system gesture navigation (see the LLD's System
 * Gesture Coexistence), regardless of where a caller places it.
 */
@Composable
fun KidButton(
    onActivate: () -> Unit,
    modifier: Modifier = Modifier,
    onPressedChange: (Boolean) -> Unit = {},
    content: @Composable (pressed: Boolean) -> Unit,
) {
    var pressed by remember { mutableStateOf(false) }
    val pressState = remember(onActivate, onPressedChange) {
        PressState(
            onPressedChange = { isPressed ->
                pressed = isPressed
                onPressedChange(isPressed)
            },
            onActivate = onActivate,
        )
    }

    Box(
        modifier = modifier
            .defaultMinSize(minWidth = MIN_TOUCH_TARGET_DP, minHeight = MIN_TOUCH_TARGET_DP)
            .excludeFromSystemGestures()
            .pointerInput(pressState) {
                awaitPointerEventScope {
                    while (true) {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val bounds = Rect(Offset.Zero, size.toSize())
                        pressState.onClaim(now = down.uptimeMillis)
                        val pointerId = down.id
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == pointerId } ?: continue
                            if (!change.pressed) {
                                pressState.onRelease(now = change.uptimeMillis)
                                break
                            }
                            val inside = bounds.contains(change.position)
                            pressState.onPositionChanged(inside, now = change.uptimeMillis)
                        }
                    }
                }
            },
    ) {
        content(pressed)
    }
}
