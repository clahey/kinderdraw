package net.clahey.kinderdraw.shared.userexperience

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

private const val SHRINK_MILLIS = 160
private const val APPROACH_MILLIS = 200
private const val POISE_MILLIS = 110
private const val ENTER_MILLIS = 180
private const val SLIDE_IN_MILLIS = 240
private const val RECEDE_MILLIS = 220
private const val GROW_MILLIS = 240
private const val FLASH_MILLIS = 260

/**
 * Which movement is running. Each runs [SaveFeedbackState.progress] from 0 to 1.
 *
 * The order matters for layering as much as for motion: the sheet is drawn
 * above the button only during [Approach] and [Poise], and both boundaries of
 * that span fall where the sheet doesn't overlap the button, so neither change
 * of layer is visible. See [aboveButton].
 */
enum class SaveFlightLeg {
    /** Shrinking where it stands, until it no longer reaches the button. */
    Shrink,

    /** Crossing to the button and coming to rest overlapping its top edge. */
    Approach,

    /** A small hop clear of the button, so the descent can pass behind it. */
    Poise,

    /** Dropping into the button, which swallows it. */
    Enter,

    /** Going back the way it came, to where it last cleared the button. */
    Recede,

    /** A fresh sheet coming in from off screen, to where a departing one shrinks to. */
    SlideIn,

    /** Growing from there to fill the screen — [Shrink] run backwards. */
    Grow,
}

/**
 * The New Picture sequence's save feedback — see the User Experience LLD's
 * Putting the Drawing Away. Each movement is its own suspending call, so the
 * screen keeps the order and can run [lift] alongside the save it doesn't yet
 * have an answer for.
 *
 * Every call suspends for its movement's duration, which is what keeps the
 * interaction hold over the whole flight (CANVAS-UX-019) without any further
 * arrangement.
 */
class SaveFeedbackState {
    /** The drawing being put away, or null once it has gone in or come back. */
    var departing by mutableStateOf<ImageBitmap?>(null)
        private set

    /** The fresh canvas arriving to replace it, on the saved path only. */
    var arriving by mutableStateOf<ImageBitmap?>(null)
        private set

    /** Whether the canvas underneath is hidden by the cover. */
    var covered by mutableStateOf(false)
        private set

    var leg by mutableStateOf(SaveFlightLeg.Shrink)
        private set

    /** How far through [leg] the movement is. */
    val progress = Animatable(0f)

    /** How far through the failure burst, 0 to 1. Zero means nothing is showing. */
    val flash = Animatable(0f)

    /**
     * The sheet is in front of the button only while it is over there, and
     * both ends of that span fall on a movement boundary where it doesn't
     * overlap the button — so neither change of layer can be seen. Shrinking
     * first is what buys the near end: at full size the sheet would cover the
     * button, and drawing over it then would take it off screen.
     */
    // @spec CANVAS-UX-043
    val aboveButton: Boolean
        get() = leg == SaveFlightLeg.Approach ||
            leg == SaveFlightLeg.Poise ||
            leg == SaveFlightLeg.Recede

    /**
     * Picks the drawing up and carries it to the button. Commits to no
     * outcome, so it runs while the save is still going — and if the save
     * outruns it, the drawing waits at the button rather than the screen
     * sitting still.
     */
    // @spec CANVAS-UX-040, CANVAS-UX-042
    suspend fun lift(image: ImageBitmap) {
        departing = image
        covered = true
        // Paired easings: the first accelerates and ends moving, the second
        // picks that speed up and decelerates. Giving both a curve that ends
        // at rest puts a standstill in the middle of one gesture.
        run(SaveFlightLeg.Shrink, SHRINK_MILLIS, FastOutLinearInEasing)
        run(SaveFlightLeg.Approach, APPROACH_MILLIS, LinearOutSlowInEasing)
    }

    /**
     * The hop before going in. Its purpose is the layer change: with the
     * sheet clear of the button for that moment, the descent can pass behind
     * it without the change being visible. Only the saved path needs it —
     * a failure goes back the way it came, which clears the button on its own.
     */
    // @spec CANVAS-UX-043
    suspend fun poise() {
        run(SaveFlightLeg.Poise, POISE_MILLIS, FastOutSlowInEasing)
    }

    /** Puts it in. The button occludes it on the way down. */
    // @spec CANVAS-UX-031
    suspend fun enter() {
        run(SaveFlightLeg.Enter, ENTER_MILLIS, FastOutLinearInEasing)
        departing = null
    }

    /**
     * Brings the fresh canvas in to replace it, along the departing drawing's
     * path in reverse: in from off screen to the place a leaving sheet shrinks
     * to, then out to fill the screen from there. The two exchange at one spot
     * rather than passing each other in different parts of the screen.
     */
    // @spec CANVAS-UX-041
    suspend fun arrive(image: ImageBitmap) {
        arriving = image
        run(SaveFlightLeg.SlideIn, SLIDE_IN_MILLIS, FastOutLinearInEasing)
        run(SaveFlightLeg.Grow, GROW_MILLIS, LinearOutSlowInEasing)
    }

    /**
     * Takes it back the way it came and puts it down where it was, flashing
     * as it starts turning back so color and motion say the same thing at the
     * same moment. Retracing the path is also what clears the button partway
     * through, which is where the sheet passes behind it again.
     */
    // @spec CANVAS-UX-032, CANVAS-UX-033, CANVAS-UX-043
    suspend fun rebound() {
        leg = SaveFlightLeg.Recede
        progress.snapTo(0f)
        coroutineScope {
            launch { flashOnce() }
            // Paired with Grow the same way the outbound legs are paired.
            progress.animateTo(1f, tween(RECEDE_MILLIS, easing = FastOutLinearInEasing))
        }
        run(SaveFlightLeg.Grow, GROW_MILLIS, LinearOutSlowInEasing)
    }

    /**
     * The failure burst, which also stands alone when motion is reduced and
     * nothing travels. Runs 0 to 1; the screen derives both how far it has
     * spread from the button and how bright it is from that one value.
     */
    // @spec CANVAS-UX-033, CANVAS-UX-037
    suspend fun flashOnce() {
        try {
            flash.snapTo(0f)
            flash.animateTo(1f, tween(FLASH_MILLIS, easing = LinearEasing))
        } finally {
            flash.snapTo(0f)
        }
    }

    /**
     * Takes everything off the screen. Called however the sequence ended,
     * including cancellation — the flight is presentation, and a screen that
     * comes back resumes no part of it.
     */
    // @spec CANVAS-UX-025
    fun dismiss() {
        departing = null
        arriving = null
        covered = false
    }

    private suspend fun run(
        leg: SaveFlightLeg,
        millis: Int,
        easing: androidx.compose.animation.core.Easing,
    ) {
        this.leg = leg
        progress.snapTo(0f)
        progress.animateTo(1f, tween(millis, easing = easing))
    }
}
