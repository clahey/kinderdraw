package net.clahey.kinderdraw.shared.painting

/**
 * Painting's resolved brush source — see the Painting LLD's Active Stroke
 * Settings section. The returned brush already carries whatever color it
 * should render with; color isn't tracked or resolved separately. Owned
 * and implemented by User Experience; Painting only ever depends on this
 * interface, queried once per stroke.
 */
interface ActiveStrokeSettings {
    fun getResolvedBrush(): Brush
}
