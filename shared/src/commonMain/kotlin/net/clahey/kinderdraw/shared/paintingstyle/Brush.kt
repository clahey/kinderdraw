package net.clahey.kinderdraw.shared.paintingstyle

/**
 * A pluggable rendering strategy for strokes — see the Painting Style LLD's
 * Brushes section. Owns creating [Stroke] instances, so it's free to pair
 * itself with whatever internal stroke representation its own rendering
 * needs (e.g. a brush with per-point color, not just a flat point list);
 * [AbstractSimpleBrush] covers the common flat-point-list case.
 */
interface Brush {
    fun startStroke(point: Point): Stroke

    /**
     * Reconstructs a stroke from a previously saved [Stroke.save] map — see
     * the Painting Style LLD's Save and Restore (CANVAS-STYLE-017). Never
     * resolves a new color from this brush's `ColorSource`.
     */
    fun restore(saved: Map<String, Any?>): Stroke
}
