package net.clahey.kinderdraw.shared.paintingstyle

import kotlin.math.sqrt
import kotlin.random.Random

/**
 * Density increases or decreases linearly across [min, max], with relative
 * density [weightAtMin] at one end and [weightAtMax] at the other — see the
 * Painting Style LLD's Distributions section for the mixture-sampling
 * algorithm this implements.
 */
class LinearDistribution(
    private val min: Float = 0f,
    private val max: Float = 1f,
    private val weightAtMin: Float = 0f,
    private val weightAtMax: Float = 1f,
    private val random: Random = Random.Default,
) : Distribution {
    // @spec CANVAS-STYLE-008, CANVAS-STYLE-009
    init {
        require(min <= max) { "min ($min) must be <= max ($max)" }
        require(weightAtMin >= 0f && weightAtMax >= 0f) { "weightAtMin and weightAtMax must each be non-negative" }
        require(weightAtMin > 0f || weightAtMax > 0f) { "weightAtMin and weightAtMax must not both be zero" }
    }

    // @spec CANVAS-STYLE-007
    override fun sample(): Float {
        val probabilityIncreasing = weightAtMax / (weightAtMin + weightAtMax)
        val fraction = if (random.nextFloat() < probabilityIncreasing) {
            sqrt(random.nextFloat())
        } else {
            1f - sqrt(random.nextFloat())
        }
        return min + fraction * (max - min)
    }
}
