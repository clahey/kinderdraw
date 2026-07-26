package net.clahey.kinderdraw.shared.paintingstyle

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class UniformDistributionTest {
    // @spec CANVAS-STYLE-006
    @Test
    fun scalesTheRandomFractionIntoTheConfiguredRange() {
        val distribution = UniformDistribution(min = 10f, max = 20f, random = FakeRandom(listOf(0.25f)))

        assertEquals(12.5f, distribution.sample())
    }

    // @spec CANVAS-STYLE-006
    @Test
    fun defaultsToTheZeroToOneRange() {
        val distribution = UniformDistribution(random = FakeRandom(listOf(0.6f)))

        assertEquals(0.6f, distribution.sample())
    }

    // @spec CANVAS-STYLE-009
    @Test
    fun rejectsMinGreaterThanMax() {
        assertFailsWith<IllegalArgumentException> {
            UniformDistribution(min = 1f, max = 0f)
        }
    }
}
