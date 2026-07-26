package net.clahey.kinderdraw.shared.paintingstyle

import androidx.compose.ui.graphics.Color
import kotlin.test.Test
import kotlin.test.assertEquals

class ConstantColorTest {
    // @spec CANVAS-STYLE-003
    @Test
    fun alwaysReturnsTheConstructedColor() {
        val colorSource = ConstantColor(Color.Red)

        assertEquals(Color.Red, colorSource.getNextColor())
        assertEquals(Color.Red, colorSource.getNextColor())
    }
}
