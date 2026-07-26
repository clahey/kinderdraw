package net.clahey.kinderdraw.shared.paintingstyle

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class PowerDistributionTest {
    // @spec CANVAS-STYLE-013
    @Test
    fun exponentTwoMatchesTheSquareRootRamp() {
        val distribution = PowerDistribution(exponent = 2f, random = FakeRandom(listOf(0.25f)))

        assertEquals(0.5f, distribution.sample())
    }

    // @spec CANVAS-STYLE-013
    @Test
    fun exponentOneIsUniform() {
        val distribution = PowerDistribution(exponent = 1f, random = FakeRandom(listOf(0.6f)))

        assertEquals(0.6f, distribution.sample())
    }

    // @spec CANVAS-STYLE-013
    @Test
    fun higherExponentsBiasFurtherTowardMax() {
        val distribution = PowerDistribution(exponent = 3f, random = FakeRandom(listOf(0.125f)))

        assertEquals(0.5f, distribution.sample())
    }

    // @spec CANVAS-STYLE-013
    @Test
    fun scalesTheSampledFractionIntoTheConfiguredRange() {
        val distribution = PowerDistribution(min = 10f, max = 20f, exponent = 2f, random = FakeRandom(listOf(0.25f)))

        assertEquals(15f, distribution.sample())
    }

    // @spec CANVAS-STYLE-014
    @Test
    fun rejectsAZeroExponent() {
        assertFailsWith<IllegalArgumentException> {
            PowerDistribution(exponent = 0f)
        }
    }

    // @spec CANVAS-STYLE-014
    @Test
    fun rejectsANegativeExponent() {
        assertFailsWith<IllegalArgumentException> {
            PowerDistribution(exponent = -1f)
        }
    }

    // @spec CANVAS-STYLE-009
    @Test
    fun rejectsMinGreaterThanMax() {
        assertFailsWith<IllegalArgumentException> {
            PowerDistribution(min = 1f, max = 0f)
        }
    }
}
