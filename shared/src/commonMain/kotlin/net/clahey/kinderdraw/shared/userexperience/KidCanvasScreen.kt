package net.clahey.kinderdraw.shared.userexperience

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import net.clahey.kinderdraw.shared.painting.Painting
import net.clahey.kinderdraw.shared.painting.PaintingState
import net.clahey.kinderdraw.shared.painting.paintingStateSaver
import net.clahey.kinderdraw.shared.paintingstyle.StyleSettings

/** The name separating stroke color's generator from every other one — see [namedRandom]. */
private const val STROKE_COLOR_SOURCE = "strokeColor"

/**
 * Counts how many times the canvas has been recreated: saved as it stands and
 * restored one higher, so each recreation moves every named source onto the
 * next stream its seed generator yields (CANVAS-UX-048, CANVAS-UX-049). This
 * single count is the whole of what crosses a recreation — no generator state
 * is preserved, and none can be, since [kotlin.random.Random] exposes none.
 */
private val generationSaver: Saver<Int, Int> = Saver(
    save = { it },
    restore = { it + 1 },
)

/**
 * The kid canvas screen's current minimal slice: a full-bleed drawing
 * surface with no Widgets chrome yet (see the User Experience LLD's Open
 * Questions on deferred Widgets/Config wiring). The drawing survives an
 * OS-driven recreation (rotation, brief backgrounding, process death) via
 * [paintingStateSaver] — see the Painting LLD's Lifecycle Survival.
 *
 * [seed] fixes the colors this canvas draws, so that the same seed and the
 * same actions reproduce the same drawing. Null — every launch that didn't
 * ask for a particular drawing — leaves colors fresh each time. See the User
 * Experience LLD's Seeding the Sampled Colors.
 */
// @spec CANVAS-UX-045, CANVAS-UX-046, CANVAS-UX-047, CANVAS-UX-048, CANVAS-UX-049, CANVAS-UX-050
@Composable
fun KidCanvasScreen(
    seed: Long? = null,
    styleSettings: StyleSettings = run {
        val generation = rememberSaveable(saver = generationSaver) { 0 }
        remember(seed, generation) {
            DefaultStyleSettings(namedRandom(seed, STROKE_COLOR_SOURCE, generation))
        }
    },
    state: PaintingState = rememberSaveable(saver = paintingStateSaver(styleSettings)) {
        PaintingState(styleSettings)
    },
) {
    Painting(state = state, modifier = Modifier.fillMaxSize())
}
