package net.clahey.kinderdraw.shared.widgets

import android.graphics.Rect as AndroidRect
import android.view.View
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalView
import java.util.WeakHashMap
import kotlin.math.roundToInt

/**
 * One [GestureExclusionRegistry] per hosting [View] — `setSystemGestureExclusionRects`
 * takes one list for the whole View, so every control sharing that View
 * shares a registry rather than overwriting each other's rects.
 */
private val registriesByView = WeakHashMap<View, GestureExclusionRegistry>()

private fun registryFor(view: View): GestureExclusionRegistry =
    registriesByView.getOrPut(view) { GestureExclusionRegistry() }

internal fun Rect.toAndroidRect(): AndroidRect =
    AndroidRect(left.roundToInt(), top.roundToInt(), right.roundToInt(), bottom.roundToInt())

// @spec CANVAS-WIDGETS-017
internal actual fun Modifier.excludeFromSystemGestures(): Modifier = composed {
    val view = LocalView.current
    val key = remember { Any() }
    val registry = remember(view) { registryFor(view) }

    DisposableEffect(registry, key) {
        onDispose {
            view.systemGestureExclusionRects = registry.remove(key).map { it.toAndroidRect() }
        }
    }

    onGloballyPositioned { coordinates ->
        view.systemGestureExclusionRects = registry.set(key, coordinates.boundsInRoot()).map { it.toAndroidRect() }
    }
}
