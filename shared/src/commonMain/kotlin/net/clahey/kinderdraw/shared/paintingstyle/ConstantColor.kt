package net.clahey.kinderdraw.shared.paintingstyle

import androidx.compose.ui.graphics.Color

/** Always returns the same color — see the Painting Style LLD's Color Sources section. */
class ConstantColor(private val color: Color) : ColorSource {
    // @spec CANVAS-STYLE-003
    override fun getNextColor(): Color = color
}
