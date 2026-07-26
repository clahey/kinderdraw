package net.clahey.kinderdraw.shared.paintingstyle

import kotlin.random.Random

/** Returns a scripted sequence of values from [nextFloat], for deterministic Distribution tests. */
class FakeRandom(private val values: List<Float>) : Random() {
    private var index = 0

    override fun nextBits(bitCount: Int): Int = error("FakeRandom only supports nextFloat()")

    override fun nextFloat(): Float = values[index++]
}
