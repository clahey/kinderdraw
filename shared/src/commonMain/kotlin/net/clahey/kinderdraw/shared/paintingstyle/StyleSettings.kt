package net.clahey.kinderdraw.shared.paintingstyle

/**
 * Painting's active brush and background source — see this LLD's Style
 * Settings section. Defined here but implemented outside Painting Style —
 * today, by User Experience. Painting holds a reference to this interface
 * from its own construction. [getActiveBrush] returns a long-lived
 * [Brush] instance the caller starts a stroke on directly; [getActiveBrush]
 * is called once per stroke, [getActiveBackground] once when Painting is
 * constructed and again each time the clear operation is called, with the
 * caller resolving and caching a [androidx.compose.ui.graphics.Color] from
 * it each time (see the Painting LLD's Style Settings section).
 */
interface StyleSettings {
    fun getActiveBrush(): Brush

    fun getActiveBackground(): ColorSource
}
