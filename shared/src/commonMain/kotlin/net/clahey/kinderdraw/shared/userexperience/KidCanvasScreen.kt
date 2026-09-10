package net.clahey.kinderdraw.shared.userexperience

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import net.clahey.kinderdraw.shared.imagestorage.ImageStorage
import net.clahey.kinderdraw.shared.painting.Painting
import net.clahey.kinderdraw.shared.painting.PaintingState
import net.clahey.kinderdraw.shared.painting.paintingStateSaver
import net.clahey.kinderdraw.shared.paintingstyle.StyleSettings
import net.clahey.kinderdraw.shared.brand.BrandYellow
import net.clahey.kinderdraw.shared.widgets.KidButton

/** [KidButton.onActivate]'s test tag for the New Picture control — see [KidCanvasScreenTest]. */
const val NEW_PICTURE_TEST_TAG = "new-picture"

// New Picture's chrome is deliberately neutral: the brand yellow is a
// highlight on the sun icon, not a fill.
private val NewPictureBackground = Color(0xFFF2F2F2)
private val NewPicturePressedBackground = Color(0xFFE0E0E0)
private val NewPictureBorderColor = Color(0xFFBDBDBD)
private val NewPictureShadowColor = Color.Gray

private val NewPictureShape = RoundedCornerShape(topStart = 20.dp, bottomStart = 20.dp)

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
 * The kid canvas's composition root — see the User Experience LLD. Composes
 * Painting full-bleed with New Picture anchored as edge chrome on top of it,
 * and owns the [InteractionLock] both of them take, so at most one of them
 * holds a gesture at a time (see Input Arbitration). The drawing survives an
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
    imageStorage: ImageStorage,
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
    // Deliberately not rememberSaveable: a restored hold would stand for a
    // pointer that no longer exists, and nothing left alive could release it.
    // @spec CANVAS-UX-024
    val lock = remember { InteractionLock() }

    Box(Modifier.fillMaxSize()) {
        // @spec CANVAS-UX-001
        Painting(
            state = state,
            lock = lock,
            modifier = Modifier.fillMaxSize(),
        )

        // @spec CANVAS-UX-001, CANVAS-UX-019
        KidButton(
            modifier = Modifier.align(Alignment.CenterEnd).testTag(NEW_PICTURE_TEST_TAG),
            lock = lock,
            onActivate = {
                // @spec CANVAS-UX-010, CANVAS-UX-012
                if (state.isEmpty()) {
                    // @spec CANVAS-UX-013
                    state.clear()
                } else {
                    // Retrying a failed save can't duplicate the drawing: a
                    // failed create leaves no entry behind (see IMAGES-019).
                    // @spec CANVAS-UX-011, CANVAS-UX-028
                    val saved = state.save(imageStorage).isSuccess || state.save(imageStorage).isSuccess
                    // A drawing that couldn't be saved stays on the canvas —
                    // clearing it would destroy the only copy.
                    // @spec CANVAS-UX-013, CANVAS-UX-029
                    if (saved) state.clear()
                }
            },
        ) { pressed ->
            Box(
                modifier = Modifier
                    .size(width = 64.dp, height = 96.dp)
                    .shadow(
                        elevation = 4.dp,
                        shape = NewPictureShape,
                        ambientColor = NewPictureShadowColor,
                        spotColor = NewPictureShadowColor,
                    )
                    .background(if (pressed) NewPicturePressedBackground else NewPictureBackground, NewPictureShape)
                    .border(1.5.dp, NewPictureBorderColor, NewPictureShape),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    imageVector = Icons.Filled.WbSunny,
                    contentDescription = "New Picture",
                    modifier = Modifier.size(40.dp),
                    colorFilter = ColorFilter.tint(BrandYellow),
                )
            }
        }
    }
}
