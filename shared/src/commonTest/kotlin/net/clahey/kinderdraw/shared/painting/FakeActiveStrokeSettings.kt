package net.clahey.kinderdraw.shared.painting

import androidx.compose.ui.graphics.Color

/** A settable, call-counting test double for [ActiveStrokeSettings]. */
class FakeActiveStrokeSettings(
    var brush: Brush = FakeBrush(),
    var background: Color = Color.White,
) : ActiveStrokeSettings {
    var brushQueryCount = 0
        private set
    var backgroundQueryCount = 0
        private set

    override fun getResolvedBrush(): Brush {
        brushQueryCount++
        return brush
    }

    override fun getResolvedBackground(): Color {
        backgroundQueryCount++
        return background
    }
}
