package net.clahey.kinderdraw.shared.painting

import androidx.compose.ui.graphics.Color

/** A settable, call-counting test double for [ActiveStrokeSettings]. */
class FakeActiveStrokeSettings(
    var color: Color = Color.Black,
    var brush: Brush = FakeBrush(),
) : ActiveStrokeSettings {
    var colorQueryCount = 0
        private set
    var brushQueryCount = 0
        private set

    override fun getResolvedColor(): Color {
        colorQueryCount++
        return color
    }

    override fun getResolvedBrush(): Brush {
        brushQueryCount++
        return brush
    }
}
