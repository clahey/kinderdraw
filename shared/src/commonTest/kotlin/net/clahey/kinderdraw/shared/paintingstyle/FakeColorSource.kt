package net.clahey.kinderdraw.shared.paintingstyle

import androidx.compose.ui.graphics.Color

/** Returns a scripted sequence of colors, one per [getNextColor] call. */
class FakeColorSource(private val colors: List<Color>) : ColorSource {
    private var index = 0
    override fun getNextColor(): Color = colors[index++]
}
