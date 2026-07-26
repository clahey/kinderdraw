package net.clahey.kinderdraw.shared.userexperience

import androidx.compose.ui.graphics.Color
import net.clahey.kinderdraw.shared.paintingstyle.Brush
import net.clahey.kinderdraw.shared.paintingstyle.ColorSource
import net.clahey.kinderdraw.shared.paintingstyle.ConstantColor
import net.clahey.kinderdraw.shared.paintingstyle.ConstantDistribution
import net.clahey.kinderdraw.shared.paintingstyle.DefaultBrush
import net.clahey.kinderdraw.shared.paintingstyle.PowerDistribution
import net.clahey.kinderdraw.shared.paintingstyle.RandomColor
import net.clahey.kinderdraw.shared.paintingstyle.StyleSettings
import net.clahey.kinderdraw.shared.paintingstyle.UniformDistribution

/**
 * Today's placeholder [StyleSettings] — no swatch-tap wiring exists yet
 * (see the User Experience LLD's Open Questions), so every stroke resolves
 * to a fresh [RandomColor] sample and the background stays a fixed white.
 * See the User Experience LLD's Interaction Feedback for why brightness is
 * biased toward the bright end rather than sampled uniformly.
 */
class DefaultStyleSettings : StyleSettings {
    private val brush: Brush = DefaultBrush(
        colorSource = RandomColor(
            hue = UniformDistribution(),
            saturation = ConstantDistribution(1f),
            value = PowerDistribution(exponent = STROKE_BRIGHTNESS_EXPONENT),
        ),
    )
    private val backgroundColor: ColorSource = ConstantColor(Color.White)

    override fun getActiveBrush(): Brush = brush

    override fun getActiveBackground(): ColorSource = backgroundColor

    companion object {
        private const val STROKE_BRIGHTNESS_EXPONENT = 3f
    }
}
