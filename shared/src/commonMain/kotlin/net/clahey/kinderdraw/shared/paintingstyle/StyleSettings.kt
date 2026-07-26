package net.clahey.kinderdraw.shared.paintingstyle

import androidx.compose.ui.graphics.Color

/**
 * Painting's resolved brush and background source — see this LLD's Style
 * Settings section. The returned brush already carries whatever color it
 * should render with; color isn't tracked or resolved separately. Defined
 * here but implemented outside Painting Style — today, by User Experience.
 * Painting holds a reference to this interface from its own construction.
 * [getResolvedBrush] is queried once per stroke; [getResolvedBackground] is
 * queried once when Painting is constructed and again each time the clear
 * operation is called, reused in between (see the Painting LLD's Style
 * Settings section).
 */
interface StyleSettings {
    fun getResolvedBrush(): Brush

    fun getResolvedBackground(): Color
}
