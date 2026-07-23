package net.clahey.kinderdraw.shared.painting

import androidx.compose.ui.graphics.Color

/**
 * Painting's resolved color/brush source — see the Painting LLD's Active
 * Stroke Settings section. Owned and implemented by User Experience;
 * Painting only ever depends on this interface, queried once per stroke.
 */
interface ActiveStrokeSettings {
    fun getResolvedColor(): Color
    fun getResolvedBrush(): Brush
}
