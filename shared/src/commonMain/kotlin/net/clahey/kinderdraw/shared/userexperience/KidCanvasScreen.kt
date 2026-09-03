package net.clahey.kinderdraw.shared.userexperience

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import net.clahey.kinderdraw.shared.imagestorage.ImageStorage
import net.clahey.kinderdraw.shared.painting.Painting
import net.clahey.kinderdraw.shared.painting.PaintingState
import net.clahey.kinderdraw.shared.painting.paintingStateSaver
import net.clahey.kinderdraw.shared.widgets.KidButton

/** [KidButton.onActivate]'s test tag for the New Picture control — see [KidCanvasScreenTest]. */
const val NEW_PICTURE_TEST_TAG = "new-picture"

/** The drawing travelling to or from the New Picture button. */
const val SAVE_FLIGHT_TEST_TAG = "save-flight"

/** The fresh canvas arriving to replace a drawing that was put away. */
const val SAVE_ARRIVAL_TEST_TAG = "save-arrival"

/** The bare surface between the two sheets. */
const val SAVE_FLIGHT_COVER_TEST_TAG = "save-flight-cover"

/** The failed save's red flash. */
const val SAVE_FAILURE_FLASH_TEST_TAG = "save-failure-flash"

/** Brand yellow — see `docs/brand.md`. Used as a highlight on the icon only; the button's own chrome stays neutral gray. */
private val BrandYellow = Color(0xFFFCD214)

private val NewPictureBackground = Color(0xFFF2F2F2)
private val NewPicturePressedBackground = Color(0xFFE0E0E0)
private val NewPictureBorderColor = Color(0xFFBDBDBD)
private val NewPictureShadowColor = Color.Gray

private val NewPictureShape = RoundedCornerShape(topStart = 20.dp, bottomStart = 20.dp)

/**
 * The bare surface between two sheets — not a stand-in for the canvas, which
 * is why it can be white whatever background `clear()` goes on to resolve.
 * The new background arrives on the sheet carrying it instead.
 */
private val SaveFlightCover = Color.White
private val SaveFlightBorder = Color(0xFF9E9E9E)
private val SaveFailureFlash = Color(0xFFD32F2F)
private val SaveFlightBorderWidth = 2.dp

/**
 * The kid canvas's composition root — see the User Experience LLD. Composes
 * Painting full-bleed with New Picture anchored as edge chrome on top of it,
 * and owns the [InteractionLock] both of them take, so at most one of them
 * holds a gesture at a time (see Input Arbitration). The drawing survives an
 * OS-driven recreation (rotation, brief backgrounding, process death) via
 * [paintingStateSaver] — see the Painting LLD's Lifecycle Survival.
 */
@Composable
fun KidCanvasScreen(
    imageStorage: ImageStorage,
    state: PaintingState = run {
        val styleSettings = remember { DefaultStyleSettings() }
        rememberSaveable(saver = paintingStateSaver(styleSettings)) { PaintingState(styleSettings) }
    },
    reduceMotion: Boolean = false,
) {
    // Deliberately not rememberSaveable: a restored hold would stand for a
    // pointer that no longer exists, and nothing left alive could release it.
    // @spec CANVAS-UX-024
    val lock = remember { InteractionLock() }
    val feedback = remember { SaveFeedbackState() }

    // Where the drawing travels from and to. Both are this screen's own
    // layout, so no control has to report its position outward.
    var canvasSize by remember { mutableStateOf(Size.Zero) }
    var buttonBounds by remember { mutableStateOf(Rect.Zero) }

    Box(Modifier.fillMaxSize().onGloballyPositioned { canvasSize = it.size.toSize() }) {
        // @spec CANVAS-UX-001
        Painting(
            state = state,
            lock = lock,
            modifier = Modifier.fillMaxSize(),
        )

        // Between Painting and the chrome. Nothing here takes pointer input.
        // @spec CANVAS-UX-001, CANVAS-UX-035
        SaveFeedbackBelowButton(feedback, canvasSize, buttonBounds)

        // @spec CANVAS-UX-001, CANVAS-UX-009, CANVAS-UX-019
        KidButton(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .onGloballyPositioned { buttonBounds = it.boundsInParent() }
                .testTag(NEW_PICTURE_TEST_TAG),
            lock = lock,
            onActivate = {
                // @spec CANVAS-UX-010, CANVAS-UX-012
                if (state.isEmpty()) {
                    // Nothing was put away, so there is nothing to acknowledge.
                    // @spec CANVAS-UX-013, CANVAS-UX-036
                    state.clear()
                } else {
                    // Taken before the save, since a save that succeeds clears
                    // the drawing this is a picture of.
                    // @spec CANVAS-UX-030
                    val snapshot = if (reduceMotion) null else state.snapshot()
                    try {
                        coroutineScope {
                            // The lift commits to no outcome, so it runs while
                            // the write is still going rather than after it.
                            // @spec CANVAS-UX-040
                            val lifting = snapshot?.let { launch { feedback.lift(it) } }

                            // Retrying a failed save can't duplicate the drawing:
                            // a failed create leaves no entry behind (IMAGES-019).
                            // @spec CANVAS-UX-011, CANVAS-UX-028
                            val saved = if (state.save(imageStorage).isSuccess) {
                                true
                            } else {
                                state.save(imageStorage).isSuccess
                            }
                            // A drawing that couldn't be saved stays on the
                            // canvas — clearing it would destroy the only copy.
                            // Clearing as soon as the write lands, rather than
                            // when the flight ends, keeps the write-to-clear
                            // window exactly the width it always was.
                            // @spec CANVAS-UX-013, CANVAS-UX-029
                            if (saved) state.clear()

                            // A write slower than the lift leaves the drawing
                            // waiting above the button, which is a pose rather
                            // than a stall.
                            lifting?.join()

                            if (snapshot == null) {
                                // @spec CANVAS-UX-037
                                if (!saved) feedback.flashOnce()
                            } else if (saved) {
                                // The hop exists so the descent can pass
                                // behind the button unseen.
                                // @spec CANVAS-UX-043, CANVAS-UX-031, CANVAS-UX-041
                                feedback.poise()
                                feedback.enter()
                                feedback.arrive(state.snapshot())
                            } else {
                                // @spec CANVAS-UX-032
                                feedback.rebound()
                            }
                        }
                    } finally {
                        // However the sequence ended, including cancelled.
                        // @spec CANVAS-UX-025
                        feedback.dismiss()
                    }
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

        // The sheet outranks the chrome only while it is over at the button,
        // by which point it is too small to hide it. Both ends of that span
        // fall where the two don't overlap, so the change isn't visible.
        // @spec CANVAS-UX-043
        val departing = feedback.departing
        if (departing != null && feedback.aboveButton) {
            Sheet(
                departing,
                feedback.placement(canvasSize, buttonBounds),
                feedback.borderAlpha(),
                SAVE_FLIGHT_TEST_TAG,
            )
        }
    }
}

/**
 * Everything the save feedback draws beneath the New Picture button — see the
 * User Experience LLD's Putting the Drawing Away. Nothing here takes pointer
 * input: a touch during the flight reaches Painting underneath, which the
 * sequence's own hold then refuses.
 */
@Composable
private fun BoxScope.SaveFeedbackBelowButton(
    state: SaveFeedbackState,
    canvasSize: Size,
    buttonBounds: Rect,
) {
    if (state.covered) {
        // @spec CANVAS-UX-042
        Box(Modifier.fillMaxSize().background(SaveFlightCover).testTag(SAVE_FLIGHT_COVER_TEST_TAG))
    }

    val departing = state.departing
    if (departing != null && !state.aboveButton) {
        // @spec CANVAS-UX-040, CANVAS-UX-031, CANVAS-UX-032, CANVAS-UX-043
        Sheet(departing, state.placement(canvasSize, buttonBounds), state.borderAlpha(), SAVE_FLIGHT_TEST_TAG)
    }

    val arriving = state.arriving
    if (arriving != null) {
        // @spec CANVAS-UX-041
        Sheet(arriving, state.placement(canvasSize, buttonBounds), state.borderAlpha(), SAVE_ARRIVAL_TEST_TAG)
    }

    val flash = state.flash.value
    if (flash > 0f) {
        // Grown from the button rather than washed over the screen, so the
        // failure is attributed to the control that was pressed. Scaling a
        // full-screen rect about any interior point only grows it, so a scale
        // of one already reaches every edge.
        // @spec CANVAS-UX-033
        val origin = if (buttonBounds.isEmpty || canvasSize.width <= 0f || canvasSize.height <= 0f) {
            TransformOrigin.Center
        } else {
            TransformOrigin(buttonBounds.center.x / canvasSize.width, buttonBounds.center.y / canvasSize.height)
        }
        val spread = (flash / FlashSpreadFraction).coerceAtMost(1f)
        // Brightens over the spread, then fades once it has arrived.
        val brightness = if (flash < FlashSpreadFraction) spread else 1f - (flash - FlashSpreadFraction) / (1f - FlashSpreadFraction)
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    transformOrigin = origin
                    scaleX = spread
                    scaleY = spread
                    alpha = brightness * FlashPeakAlpha
                }
                .background(SaveFailureFlash, RoundedCornerShape(SaveFlashCorner))
                .testTag(SAVE_FAILURE_FLASH_TEST_TAG)
        )
    }
}

/** Where a sheet sits: its size, where its centre is, and how far it is turned. */
private data class Placement(
    val scale: Float,
    val centerX: Float,
    val centerY: Float,
    val rotation: Float,
)

private fun lerp(from: Placement, to: Placement, t: Float) = Placement(
    scale = from.scale + (to.scale - from.scale) * t,
    centerX = from.centerX + (to.centerX - from.centerX) * t,
    centerY = from.centerY + (to.centerY - from.centerY) * t,
    rotation = from.rotation + (to.rotation - from.rotation) * t,
)

@Composable
private fun BoxScope.Sheet(image: ImageBitmap, placement: Placement, borderAlpha: Float, tag: String) {
    Image(
        bitmap = image,
        contentDescription = null,
        contentScale = ContentScale.FillBounds,
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                // About the centre, so a placement's centre is exactly where
                // the sheet ends up however far it is scaled or turned.
                transformOrigin = TransformOrigin(0.5f, 0.5f)
                scaleX = placement.scale
                scaleY = placement.scale
                rotationZ = placement.rotation
                translationX = placement.centerX - size.width / 2f
                translationY = placement.centerY - size.height / 2f
            }
            // Absent wherever a sheet rests at full size, so the frame the live
            // canvas is handed back through matches it exactly.
            // @spec CANVAS-UX-034, CANVAS-UX-038
            .border(SaveFlightBorderWidth, SaveFlightBorder.copy(alpha = borderAlpha))
            .testTag(tag),
    )
}

/** Where the sheet currently on screen sits, given which movement is running. */
private fun SaveFeedbackState.placement(canvas: Size, button: Rect): Placement {
    val p = progress.value
    val full = Placement(1f, canvas.width / 2f, canvas.height / 2f, 0f)
    if (canvas.width <= 0f || canvas.height <= 0f || button.isEmpty) return full

    val inside = Placement(
        scale = minOf(button.width / canvas.width, button.height / canvas.height),
        centerX = button.center.x,
        centerY = button.center.y,
        rotation = 0f,
    )
    // Small enough, where it already stands, that its right edge clears the
    // button — which is what lets the next movement draw over the button
    // without hiding it.
    val cleared = Placement(
        scale = (2f * button.left / canvas.width - 1f).coerceIn(0.2f, 0.8f) * ClearMargin,
        centerX = canvas.width / 2f,
        centerY = canvas.height / 2f,
        rotation = 0f,
    )
    // Narrower than the button, so the overlap reads as the sheet meeting an
    // opening rather than straddling it.
    val hoverScale = button.width * HoverWidthOfButton / canvas.width
    val hover = Placement(
        scale = hoverScale,
        centerX = button.center.x,
        // Overlapping the button's top edge — at the opening rather than
        // resting on a shape.
        centerY = button.top + button.height * HoverOverlap - canvas.height * hoverScale / 2f,
        rotation = 0f,
    )
    // Clear of the button again, so the layer change either movement out of
    // here makes cannot be seen.
    val poised = hover.copy(
        centerY = button.top - button.height * PoiseClearance - canvas.height * hoverScale / 2f,
    )
    // Wholly off the far side from the button, at the size a leaving sheet
    // shrinks to: the old drawing goes out to the right, the new one comes in
    // from the left, and both pass through `cleared` on the way. Started
    // anywhere on screen it would read as a zoom rather than an arrival.
    val offstage = cleared.copy(
        centerX = canvas.width * -0.65f,
        rotation = -10f,
    )

    return when (leg) {
        SaveFlightLeg.Shrink -> lerp(full, cleared, p)
        SaveFlightLeg.Approach -> lerp(cleared, hover, p)
        SaveFlightLeg.Poise -> lerp(hover, poised, p)
        SaveFlightLeg.Enter -> lerp(poised, inside, p)
        SaveFlightLeg.Recede -> lerp(hover, cleared, p)
        SaveFlightLeg.SlideIn -> lerp(offstage, cleared, p)
        SaveFlightLeg.Grow -> lerp(cleared, full, p)
    }
}

private fun SaveFeedbackState.borderAlpha(): Float = when (leg) {
    SaveFlightLeg.Shrink -> (progress.value * 4f).coerceAtMost(1f)
    SaveFlightLeg.Approach, SaveFlightLeg.Poise, SaveFlightLeg.Enter -> 1f
    SaveFlightLeg.Recede, SaveFlightLeg.SlideIn -> 1f
    SaveFlightLeg.Grow -> 1f - progress.value
}

/** The hovering sheet's width, as a fraction of the button's — under one, so it can meet an opening. */
private const val HoverWidthOfButton = 0.78f

/** How far down the button's own height the hovering sheet's lower edge reaches. */
private const val HoverOverlap = 0.15f

/** How far above the button the poised sheet sits, as a fraction of the button's height. */
private const val PoiseClearance = 0.12f

/** Shrunk a little further than bare clearance, so the gap is unambiguous. */
private const val ClearMargin = 0.92f

/** How much of the burst's duration is spent expanding, the rest fading. */
private const val FlashSpreadFraction = 0.3f

private const val FlashPeakAlpha = 0.72f

/** Rounded, so the burst reads as a shape arriving rather than the screen washing over. */
private val SaveFlashCorner = 56.dp
