package net.clahey.kinderdraw.shared.painting

/**
 * One live pointer's down-to-up sequence — see the Painting LLD's Stroke
 * Model. The brush (including whatever color it renders with) is fixed at
 * construction and never changes; points accumulate as the pointer moves.
 */
class Stroke(val brush: Brush) {
    private val mutablePoints = mutableListOf<Point>()

    val points: List<Point> get() = mutablePoints

    fun addPoint(point: Point) {
        mutablePoints.add(point)
    }
}
