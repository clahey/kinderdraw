package net.clahey.kinderdraw.shared.userexperience

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import net.clahey.kinderdraw.shared.imagestorage.ImageStorage
import net.clahey.kinderdraw.shared.painting.Painting
import net.clahey.kinderdraw.shared.painting.PaintingState
import net.clahey.kinderdraw.shared.painting.paintingStateSaver
import net.clahey.kinderdraw.shared.widgets.KidButton

/** [KidButton.onActivate]'s test tag for the New Picture control — see [KidCanvasScreenTest]. */
const val NEW_PICTURE_TEST_TAG = "new-picture"

/** Brand yellow — see `docs/brand.md`. Used as a highlight on the icon only; the button's own chrome stays neutral gray. */
private val BrandYellow = Color(0xFFFCD214)

private val NewPictureBackground = Color(0xFFF2F2F2)
private val NewPicturePressedBackground = Color(0xFFE0E0E0)
private val NewPictureBorderColor = Color(0xFFBDBDBD)
private val NewPictureShadowColor = Color.Gray

private val NewPictureShape = RoundedCornerShape(topStart = 20.dp, bottomStart = 20.dp)

/**
 * The kid canvas's composition root — see the User Experience LLD. Composes
 * Painting full-bleed with New Picture anchored as edge chrome on top of it,
 * and arbitrates input between the two so at most one gesture is live at a
 * time (see Input Arbitration). The drawing survives an OS-driven
 * recreation (rotation, brief backgrounding, process death) via
 * [paintingStateSaver] — see the Painting LLD's Lifecycle Survival.
 */
@Composable
fun KidCanvasScreen(
    imageStorage: ImageStorage,
    state: PaintingState = run {
        val styleSettings = remember { DefaultStyleSettings() }
        rememberSaveable(saver = paintingStateSaver(styleSettings)) { PaintingState(styleSettings) }
    },
) {
    // @spec CANVAS-UX-004
    var paintingActive by remember { mutableStateOf(false) }
    // @spec CANVAS-UX-009, CANVAS-UX-019
    var newPictureHeld by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Box(Modifier.fillMaxSize()) {
        // @spec CANVAS-UX-001
        Painting(
            state = state,
            modifier = Modifier.fillMaxSize(),
            onStrokeActiveChange = { paintingActive = it },
        )

        // @spec CANVAS-UX-001
        KidButton(
            modifier = Modifier.align(Alignment.CenterEnd).testTag(NEW_PICTURE_TEST_TAG),
            onPressedChange = { pressed -> newPictureHeld = pressed },
            onActivate = {
                // Re-affirm the hold immediately: onPressedChange(false) already
                // fired for this same release, and the hold must extend through
                // this sequence's own completion rather than lapse in between.
                newPictureHeld = true
                scope.launch {
                    // @spec CANVAS-UX-010, CANVAS-UX-011, CANVAS-UX-012
                    if (!state.isEmpty()) {
                        state.save(imageStorage)
                    }
                    // @spec CANVAS-UX-013
                    state.clear()
                    newPictureHeld = false
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

        // @spec CANVAS-UX-004, CANVAS-UX-009, CANVAS-UX-019
        // A separate, topmost layer — not a modifier chained onto Painting or
        // KidButton — so it wins hit-testing outright rather than racing
        // either one's own pointerInput for the same event. A pointer already
        // claimed by Painting before this layer appears keeps going straight
        // to Painting regardless, since Compose fixes a pointer's target at
        // its own down event.
        if (paintingActive || newPictureHeld) {
            Box(Modifier.fillMaxSize().consumeAllTouches())
        }
    }
}

/** The transparent, pointer-consuming blocking layer from Input Arbitration. */
private fun Modifier.consumeAllTouches(): Modifier = pointerInput(Unit) {
    awaitEachGesture {
        while (true) {
            val event = awaitPointerEvent()
            event.changes.forEach { it.consume() }
            if (event.changes.all { it.changedToUpIgnoreConsumed() }) break
        }
    }
}
