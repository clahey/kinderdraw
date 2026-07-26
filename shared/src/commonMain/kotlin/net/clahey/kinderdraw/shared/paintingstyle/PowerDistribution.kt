package net.clahey.kinderdraw.shared.paintingstyle

import kotlin.math.pow
import kotlin.random.Random

/**
 * Density proportional to `y^(exponent - 1)` across `[min, max]` — see the
 * Painting Style LLD's Distributions section. `exponent = 1` is uniform;
 * `exponent > 1` biases toward `max`, steepening as `exponent` grows;
 * `exponent < 1` biases toward `min` the same way. `exponent = 2`
 * reproduces `LinearDistribution`'s pure-increasing-ramp case exactly.
 */
class PowerDistribution(
    private val min: Float = 0f,
    private val max: Float = 1f,
    private val exponent: Float = 2f,
    private val random: Random = Random.Default,
) : Distribution {
    // @spec CANVAS-STYLE-009, CANVAS-STYLE-014
    init {
        require(min <= max) { "min ($min) must be <= max ($max)" }
        require(exponent > 0f) { "exponent ($exponent) must be strictly positive" }
    }

    // @spec CANVAS-STYLE-013
    override fun sample(): Float {
        val fraction = random.nextFloat().pow(1f / exponent)
        return min + fraction * (max - min)
    }
}
