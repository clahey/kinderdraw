package net.clahey.kinderdraw.shared.userexperience

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import net.clahey.kinderdraw.shared.painting.Painting
import net.clahey.kinderdraw.shared.painting.PaintingState
import net.clahey.kinderdraw.shared.painting.paintingStateSaver

/**
 * The kid canvas screen's current minimal slice: a full-bleed drawing
 * surface with no Widgets chrome yet (see the User Experience LLD's Open
 * Questions on deferred Widgets/Config wiring). The drawing survives an
 * OS-driven recreation (rotation, brief backgrounding, process death) via
 * [paintingStateSaver] — see the Painting LLD's Lifecycle Survival.
 */
@Composable
fun KidCanvasScreen(
    state: PaintingState = run {
        val styleSettings = remember { DefaultStyleSettings() }
        rememberSaveable(saver = paintingStateSaver(styleSettings)) { PaintingState(styleSettings) }
    },
) {
    Painting(state = state, modifier = Modifier.fillMaxSize())
}
