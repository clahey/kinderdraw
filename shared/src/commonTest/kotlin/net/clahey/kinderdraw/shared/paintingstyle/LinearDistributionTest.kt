package net.clahey.kinderdraw.shared.paintingstyle

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class LinearDistributionTest {
    // @spec CANVAS-STYLE-007
    @Test
    fun pureIncreasingRampSamplesTheIncreasingBranch() {
        // weightAtMax alone (default weightAtMin = 0) always selects the increasing branch.
        val distribution = LinearDistribution(random = FakeRandom(listOf(0.9f, 0.16f)))

        assertEquals(0.4f, distribution.sample())
    }

    // @spec CANVAS-STYLE-007
    @Test
    fun pureDecreasingRampSamplesTheDecreasingBranch() {
        val distribution = LinearDistribution(weightAtMin = 1f, weightAtMax = 0f, random = FakeRandom(listOf(0.9f, 0.16f)))

        assertEquals(0.6f, distribution.sample())
    }

    // @spec CANVAS-STYLE-007
    @Test
    fun equalWeightsSplitBetweenBothBranchesByTheBranchDraw() {
        val increasing = LinearDistribution(weightAtMin = 1f, weightAtMax = 1f, random = FakeRandom(listOf(0.3f, 0.16f)))
        val decreasing = LinearDistribution(weightAtMin = 1f, weightAtMax = 1f, random = FakeRandom(listOf(0.7f, 0.16f)))

        assertEquals(0.4f, increasing.sample())
        assertEquals(0.6f, decreasing.sample())
    }

    // @spec CANVAS-STYLE-007
    @Test
    fun scalesTheSampledFractionIntoTheConfiguredRange() {
        val distribution = LinearDistribution(min = 10f, max = 20f, random = FakeRandom(listOf(0.9f, 0.25f)))

        assertEquals(15f, distribution.sample())
    }

    // @spec CANVAS-STYLE-008
    @Test
    fun rejectsANegativeWeight() {
        assertFailsWith<IllegalArgumentException> {
            LinearDistribution(weightAtMin = -1f, weightAtMax = 1f)
        }
    }

    // @spec CANVAS-STYLE-008
    @Test
    fun rejectsBothWeightsZero() {
        assertFailsWith<IllegalArgumentException> {
            LinearDistribution(weightAtMin = 0f, weightAtMax = 0f)
        }
    }

    // @spec CANVAS-STYLE-009
    @Test
    fun rejectsMinGreaterThanMax() {
        assertFailsWith<IllegalArgumentException> {
            LinearDistribution(min = 1f, max = 0f)
        }
    }
}
