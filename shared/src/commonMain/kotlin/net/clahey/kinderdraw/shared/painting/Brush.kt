package net.clahey.kinderdraw.shared.painting

/**
 * A pluggable rendering strategy for strokes — see the Painting LLD's
 * Brushes section. Owns creating [Stroke] instances, so it's free to pair
 * itself with whatever internal stroke representation its own rendering
 * needs (e.g. a brush with per-point color, not just a flat point list);
 * [AbstractSimpleBrush] covers the common flat-point-list case.
 */
interface Brush {
    fun startStroke(point: Point): Stroke
}
