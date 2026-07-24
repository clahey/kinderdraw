package net.clahey.kinderdraw.shared.painting

import androidx.compose.ui.graphics.Color

/**
 * Painting's resolved brush and background source — see the Painting LLD's
 * Active Stroke Settings section. The returned brush already carries
 * whatever color it should render with; color isn't tracked or resolved
 * separately. Owned and implemented by User Experience; Painting holds a
 * reference to this interface from its own construction. [getResolvedBrush]
 * is queried once per stroke; [getResolvedBackground] is queried once when
 * Painting is constructed and again each time the clear operation is
 * called, reused in between.
 */
interface ActiveStrokeSettings {
    fun getResolvedBrush(): Brush

    fun getResolvedBackground(): Color
}
