package net.clahey.kinderdraw.shared.paintingstyle

import androidx.compose.ui.graphics.Color
import kotlin.test.Test
import kotlin.test.assertEquals

class RandomColorTest {
    // @spec CANVAS-STYLE-004
    @Test
    fun composesAColorFromItsThreeChannelDistributions() {
        val colorSource = RandomColor(
            hue = FakeDistribution(listOf(0.5f)),
            saturation = FakeDistribution(listOf(1f)),
            value = FakeDistribution(listOf(1f)),
        )

        assertEquals(Color.hsv(180f, 1f, 1f), colorSource.getNextColor())
    }

    // @spec CANVAS-STYLE-004
    @Test
    fun samplesFreshValuesOnEveryQuery() {
        val colorSource = RandomColor(
            hue = FakeDistribution(listOf(0f, 0.5f)),
            saturation = FakeDistribution(listOf(1f, 1f)),
            value = FakeDistribution(listOf(1f, 1f)),
        )

        assertEquals(Color.hsv(0f, 1f, 1f), colorSource.getNextColor())
        assertEquals(Color.hsv(180f, 1f, 1f), colorSource.getNextColor())
    }

    // @spec CANVAS-STYLE-010
    @Test
    fun clampsSaturationAboveOneIntoRange() {
        val colorSource = RandomColor(
            hue = FakeDistribution(listOf(0f)),
            saturation = FakeDistribution(listOf(1.5f)),
            value = FakeDistribution(listOf(1f)),
        )

        assertEquals(Color.hsv(0f, 1f, 1f), colorSource.getNextColor())
    }

    // @spec CANVAS-STYLE-010
    @Test
    fun clampsValueBelowZeroIntoRange() {
        val colorSource = RandomColor(
            hue = FakeDistribution(listOf(0f)),
            saturation = FakeDistribution(listOf(1f)),
            value = FakeDistribution(listOf(-0.5f)),
        )

        assertEquals(Color.hsv(0f, 1f, 0f), colorSource.getNextColor())
    }
}
