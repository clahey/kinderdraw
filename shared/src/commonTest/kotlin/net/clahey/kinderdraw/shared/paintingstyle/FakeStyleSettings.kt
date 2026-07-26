package net.clahey.kinderdraw.shared.paintingstyle

import androidx.compose.ui.graphics.Color

/** A settable, call-counting test double for [StyleSettings]. */
class FakeStyleSettings(
    var brush: Brush = FakeBrush(),
    var background: Color = Color.White,
) : StyleSettings {
    var brushQueryCount = 0
        private set
    var backgroundQueryCount = 0
        private set

    override fun getActiveBrush(): Brush {
        brushQueryCount++
        return brush
    }

    override fun getActiveBackground(): ColorSource {
        backgroundQueryCount++
        return ConstantColor(background)
    }
}
