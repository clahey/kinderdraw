package net.clahey.kinderdraw.shared.userexperience

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import net.clahey.kinderdraw.shared.painting.Painting
import net.clahey.kinderdraw.shared.painting.PaintingState

/**
 * The kid canvas screen's current minimal slice: a full-bleed drawing
 * surface with no Widgets chrome yet (see the User Experience LLD's Open
 * Questions on deferred Widgets/Config wiring).
 */
@Composable
fun KidCanvasScreen(state: PaintingState = remember { PaintingState(DefaultActiveStrokeSettings()) }) {
    Painting(state = state, modifier = Modifier.fillMaxSize())
}
