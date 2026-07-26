package net.clahey.kinderdraw.shared.paintingstyle

import kotlin.random.Random

/** Every value in [min, max] is equally likely — see the Painting Style LLD's Distributions section. */
class UniformDistribution(
    private val min: Float = 0f,
    private val max: Float = 1f,
    private val random: Random = Random.Default,
) : Distribution {
    // @spec CANVAS-STYLE-009
    init {
        require(min <= max) { "min ($min) must be <= max ($max)" }
    }

    // @spec CANVAS-STYLE-006
    override fun sample(): Float = min + random.nextFloat() * (max - min)
}
