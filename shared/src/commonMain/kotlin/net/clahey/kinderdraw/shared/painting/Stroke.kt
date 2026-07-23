package net.clahey.kinderdraw.shared.painting

import androidx.compose.ui.graphics.drawscope.DrawScope

/**
 * One live pointer's down-to-up sequence — see the Painting LLD's Stroke
 * Model. Created by a [Brush] (see [Brush.startStroke]), which fixes
 * whatever settings (color included) the stroke renders with; a concrete
 * implementation owns its own internal representation of the points
 * captured so far.
 */
interface Stroke {
    fun addPoint(point: Point)

    fun DrawScope.render()

    /**
     * Produces the stroke that continues from this one's current end
     * point with the same settings — see the Painting LLD's mid-stroke
     * `clear()` behavior (CANVAS-PAINT-013).
     */
    fun restart(): Stroke
}
