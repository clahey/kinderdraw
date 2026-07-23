package net.clahey.kinderdraw.shared.painting

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope

/**
 * A pluggable rendering strategy for a stroke's captured points — see the
 * Painting LLD's Brushes section. Called with the stroke's full point list
 * so far, every time that list grows, rather than being told only the
 * newest point: a brush that needs prior points to interpolate (e.g. a
 * future smoothing brush) can do so without keeping its own state across
 * calls.
 */
interface Brush {
    fun DrawScope.render(points: List<Point>, color: Color)
}
