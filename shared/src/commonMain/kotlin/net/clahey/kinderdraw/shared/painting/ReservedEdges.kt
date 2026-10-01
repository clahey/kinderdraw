package net.clahey.kinderdraw.shared.painting

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection

/**
 * Whether [position] on a drawing surface of [size] falls within an edge this
 * reserves — see the Painting LLD's System Gesture Coexistence. Each of the
 * four edges is read independently, so an edge reserving nothing matches no
 * position along it however the platform reports the others.
 */
// @spec CANVAS-PAINT-030, CANVAS-PAINT-031
internal fun WindowInsets.reserves(
    position: Offset,
    size: IntSize,
    density: Density,
    layoutDirection: LayoutDirection,
): Boolean {
    val left = getLeft(density, layoutDirection)
    val top = getTop(density)
    val right = getRight(density, layoutDirection)
    val bottom = getBottom(density)
    // The zero checks are what keep an unreserved edge from matching the row or
    // column of pixels that sits exactly on it.
    return left > 0 && position.x < left ||
        top > 0 && position.y < top ||
        right > 0 && position.x >= size.width - right ||
        bottom > 0 && position.y >= size.height - bottom
}
