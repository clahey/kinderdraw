package net.clahey.kinderdraw.shared.painting

import androidx.compose.ui.graphics.Color

/**
 * One live pointer's down-to-up sequence — see the Painting LLD's Stroke
 * Model. Color and brush are fixed at construction and never change; points
 * accumulate as the pointer moves.
 */
class Stroke(val color: Color, val brush: Brush) {
    private val mutablePoints = mutableListOf<Point>()

    val points: List<Point> get() = mutablePoints

    fun addPoint(point: Point) {
        mutablePoints.add(point)
    }
}
