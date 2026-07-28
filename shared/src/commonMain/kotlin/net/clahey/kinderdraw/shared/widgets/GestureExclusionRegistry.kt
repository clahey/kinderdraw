package net.clahey.kinderdraw.shared.widgets

import androidx.compose.ui.geometry.Rect

/**
 * Aggregates every currently-mounted control's on-screen bounds into the
 * single combined list a platform's gesture-exclusion API expects — see the
 * Widgets LLD's System Gesture Coexistence. The platform API (e.g. Android's
 * `View.setSystemGestureExclusionRects`) takes one list for the whole View,
 * so controls can't each set their own independently without clobbering one
 * another; this holds the per-control state that makes it safe for more than
 * one control to register at once.
 */
internal class GestureExclusionRegistry {
    private val rects = mutableMapOf<Any, Rect>()

    // @spec CANVAS-WIDGETS-017
    fun set(key: Any, rect: Rect): List<Rect> {
        rects[key] = rect
        return rects.values.toList()
    }

    // @spec CANVAS-WIDGETS-017
    fun remove(key: Any): List<Rect> {
        rects.remove(key)
        return rects.values.toList()
    }
}
