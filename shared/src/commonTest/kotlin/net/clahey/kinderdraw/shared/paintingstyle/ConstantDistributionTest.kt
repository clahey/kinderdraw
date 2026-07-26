package net.clahey.kinderdraw.shared.paintingstyle

import kotlin.test.Test
import kotlin.test.assertEquals

class ConstantDistributionTest {
    // @spec CANVAS-STYLE-005
    @Test
    fun alwaysReturnsTheConstructedValue() {
        val distribution = ConstantDistribution(0.42f)

        assertEquals(0.42f, distribution.sample())
        assertEquals(0.42f, distribution.sample())
    }
}
