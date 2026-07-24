package net.clahey.kinderdraw.shared.userexperience

import androidx.compose.ui.graphics.Color
import net.clahey.kinderdraw.shared.painting.ActiveStrokeSettings
import net.clahey.kinderdraw.shared.painting.Brush
import net.clahey.kinderdraw.shared.painting.DefaultBrush

/**
 * Fixed placeholder [ActiveStrokeSettings] — no Widgets control writes into
 * either value yet (see the User Experience LLD's Open Questions), so every
 * stroke resolves to the same black brush on a white background.
 */
class DefaultActiveStrokeSettings : ActiveStrokeSettings {
    override fun getResolvedBrush(): Brush = DefaultBrush(Color.Black)

    override fun getResolvedBackground(): Color = Color.White
}
