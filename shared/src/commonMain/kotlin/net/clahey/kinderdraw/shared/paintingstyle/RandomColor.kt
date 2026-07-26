package net.clahey.kinderdraw.shared.paintingstyle

import androidx.compose.ui.graphics.Color

/**
 * Composes a color from three independent [Distribution] "dials," one per
 * HSV channel, sampled fresh on every [getNextColor] call — see the
 * Painting Style LLD's Color Sources section. [hue] is a `[0, 1]` fraction
 * of the full color wheel, scaled to degrees here; [saturation] and [value]
 * are clamped into `[0, 1]` regardless of what their Distributions return.
 */
class RandomColor(
    private val hue: Distribution,
    private val saturation: Distribution,
    private val value: Distribution,
) : ColorSource {
    // @spec CANVAS-STYLE-004, CANVAS-STYLE-010
    override fun getNextColor(): Color = Color.hsv(
        hue = hue.sample() * 360f,
        saturation = saturation.sample().coerceIn(0f, 1f),
        value = value.sample().coerceIn(0f, 1f),
    )
}
