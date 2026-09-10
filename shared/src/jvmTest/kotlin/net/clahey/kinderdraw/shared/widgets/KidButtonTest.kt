package net.clahey.kinderdraw.shared.widgets

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.coroutines.CompletableDeferred
import net.clahey.kinderdraw.shared.userexperience.InteractionLock
import net.clahey.kinderdraw.shared.userexperience.isHeld

private const val BUTTON_TAG = "kid-button"
private const val LEFT_TAG = "left-button"
private const val RIGHT_TAG = "right-button"

@OptIn(ExperimentalTestApi::class)
class KidButtonTest {
    // @spec CANVAS-WIDGETS-018, CANVAS-WIDGETS-019, CANVAS-WIDGETS-023
    @Test
    fun aRefusedPressNeitherActivatesNorShowsFeedback() = runComposeUiTest {
        val lock = InteractionLock()
        val heldElsewhere = assertNotNull(lock.tryAcquire())
        var activations = 0
        val pressedStates = mutableListOf<Boolean>()

        setContent {
            KidButton(onActivate = { activations++ }, lock = lock, modifier = Modifier.testTag(BUTTON_TAG)) { pressed ->
                pressedStates.add(pressed)
                Box(Modifier.size(64.dp))
            }
        }
        val center = onNodeWithTag(BUTTON_TAG).fetchSemanticsNode().boundsInRoot.center

        onRoot().performTouchInput { down(center) }
        assertFalse(pressedStates.contains(true), "a refused pointer must show no press feedback")

        onRoot().performTouchInput { up() }
        waitForIdle()
        assertEquals(0, activations)
        assertFalse(pressedStates.contains(true), "nor at the release that ends a refused gesture")

        // The refusal holds for the rest of that gesture; a fresh press works.
        heldElsewhere.release()
        onRoot().performTouchInput { down(center) }
        waitForIdle()
        assertTrue(pressedStates.last(), "a press the lock now grants shows feedback")

        onRoot().performTouchInput { up() }
        waitForIdle()
        assertEquals(1, activations)
        assertFalse(pressedStates.last(), "and clears it at the release")
    }

    // @spec CANVAS-WIDGETS-002
    @Test
    fun claimsNothingForAPointerThatWentDownOutsideEveryHitRegion() = runComposeUiTest {
        val lock = InteractionLock()
        var activations = 0

        setContent {
            KidButton(onActivate = { activations++ }, lock = lock, modifier = Modifier.testTag(BUTTON_TAG)) {
                Box(Modifier.size(64.dp))
            }
        }
        val bounds = onNodeWithTag(BUTTON_TAG).fetchSemanticsNode().boundsInRoot

        onRoot().performTouchInput { down(Offset(bounds.right + 100f, bounds.bottom + 100f)) }
        assertFalse(lock.isHeld(), "a down outside every hit region claims nothing")

        // Dragged onto the KidWidget and lifted dead centre — the position a
        // pointer that had been claimed would activate from.
        onRoot().performTouchInput { moveTo(bounds.center) }
        onRoot().performTouchInput { up() }
        waitForIdle()

        assertEquals(0, activations, "only a down inside a region claims a pointer")
    }

    // @spec CANVAS-UX-027
    @Test
    fun takesNoHoldForAPointerMerelyHoveringOverIt() = runComposeUiTest {
        val lock = InteractionLock()
        var activations = 0
        var everPressed = false

        setContent {
            KidButton(onActivate = { activations++ }, lock = lock, modifier = Modifier.testTag(BUTTON_TAG)) { pressed ->
                if (pressed) everPressed = true
                Box(Modifier.size(64.dp))
            }
        }
        val bounds = onNodeWithTag(BUTTON_TAG).fetchSemanticsNode().boundsInRoot

        // A mouse crossing the KidWidget with no button down is a pointer that
        // is over the hit region without ever touching down in it.
        onRoot().performMouseInput { moveTo(bounds.center) }
        onRoot().performMouseInput { moveTo(bounds.center + Offset(5f, 5f)) }
        waitForIdle()

        assertFalse(lock.isHeld(), "a hovering pointer must not take the lock")
        assertFalse(everPressed, "a hovering pointer must not claim the KidWidget")
        assertEquals(0, activations)
    }

    // @spec CANVAS-WIDGETS-003
    @Test
    fun keepsAClaimedPointerWhenItDragsIntoAnotherKidWidgetsRegion() = runComposeUiTest {
        val lock = InteractionLock()
        var leftActivations = 0
        var rightActivations = 0

        setContent {
            Row {
                KidButton(onActivate = { leftActivations++ }, lock = lock, modifier = Modifier.testTag(LEFT_TAG)) {
                    Box(Modifier.size(64.dp))
                }
                KidButton(onActivate = { rightActivations++ }, lock = lock, modifier = Modifier.testTag(RIGHT_TAG)) {
                    Box(Modifier.size(64.dp))
                }
            }
        }
        val left = onNodeWithTag(LEFT_TAG).fetchSemanticsNode().boundsInRoot
        val right = onNodeWithTag(RIGHT_TAG).fetchSemanticsNode().boundsInRoot

        // Lifted over the right button, but soon enough after leaving the left
        // one that the left button's own stray tolerance forgives the drift.
        onRoot().performTouchInput { down(left.center) }
        onRoot().performTouchInput { advanceEventTime(200); moveTo(right.center) }
        onRoot().performTouchInput { up() }
        waitForIdle()

        assertEquals(1, leftActivations, "a claimed pointer activates the KidWidget that claimed it")
        assertEquals(0, rightActivations, "and never the one it was dragged into")
    }

    // @spec CANVAS-WIDGETS-008
    @Test
    fun measuresStrayTimeAgainstTheClaimingKidWidgetsOwnRegion() = runComposeUiTest {
        val lock = InteractionLock()
        var leftActivations = 0
        var rightActivations = 0

        setContent {
            Row {
                KidButton(onActivate = { leftActivations++ }, lock = lock, modifier = Modifier.testTag(LEFT_TAG)) {
                    Box(Modifier.size(64.dp))
                }
                KidButton(onActivate = { rightActivations++ }, lock = lock, modifier = Modifier.testTag(RIGHT_TAG)) {
                    Box(Modifier.size(64.dp))
                }
            }
        }
        val left = onNodeWithTag(LEFT_TAG).fetchSemanticsNode().boundsInRoot
        val right = onNodeWithTag(RIGHT_TAG).fetchSemanticsNode().boundsInRoot

        // Parked on the right button well past the tolerance. Against the left
        // button's own region the pointer is outside for all of it; against
        // whichever region it currently sits in it would count as inside.
        onRoot().performTouchInput { down(left.center) }
        assertTrue(lock.isHeld(), "the left button has to have claimed the pointer")
        onRoot().performTouchInput { moveTo(right.center) }
        onRoot().performTouchInput { advanceEventTime(500); up() }
        waitForIdle()

        assertEquals(0, leftActivations, "a long stray isn't rescued by landing on another KidWidget")
        assertEquals(0, rightActivations, "and the KidWidget strayed into doesn't activate either")
    }

    // @spec CANVAS-WIDGETS-028
    @Test
    fun hitTestsAgainstTheSizeTheKidWidgetCurrentlyHas() = runComposeUiTest {
        val lock = InteractionLock()
        var activations = 0

        setContent {
            KidButton(onActivate = { activations++ }, lock = lock, modifier = Modifier.testTag(BUTTON_TAG)) { pressed ->
                Box(Modifier.size(if (pressed) 128.dp else 64.dp))
            }
        }
        val atRest = onNodeWithTag(BUTTON_TAG).fetchSemanticsNode().boundsInRoot

        onRoot().performTouchInput { down(atRest.center) }
        waitForIdle()

        val whilePressed = onNodeWithTag(BUTTON_TAG).fetchSemanticsNode().boundsInRoot
        assertTrue(whilePressed.width > atRest.width, "the press feedback has to actually grow the KidWidget")

        // Inside the grown KidWidget, outside the bounds it had at the claim, and
        // held there far too long for the stray tolerance to rescue it.
        onRoot().performTouchInput { moveTo(Offset(atRest.right + 16f, atRest.center.y)) }
        onRoot().performTouchInput { advanceEventTime(500); up() }
        waitForIdle()

        assertEquals(1, activations, "a KidWidget that grew under the finger is judged by its new bounds")
    }

    // @spec CANVAS-WIDGETS-021
    @Test
    fun releasesTheHoldAtAReleaseThatActivatesNothing() = runComposeUiTest {
        val lock = InteractionLock()
        var activations = 0

        setContent {
            KidButton(onActivate = { activations++ }, lock = lock, modifier = Modifier.testTag(BUTTON_TAG)) {
                Box(Modifier.size(64.dp))
            }
        }
        val bounds = onNodeWithTag(BUTTON_TAG).fetchSemanticsNode().boundsInRoot

        // Down inside, then dragged well away and held there long enough that
        // the release can't qualify for the stray tolerance.
        onRoot().performTouchInput { down(bounds.center) }
        assertTrue(lock.isHeld())
        onRoot().performTouchInput { moveTo(Offset(bounds.right + 200f, bounds.bottom + 200f)) }
        onRoot().performTouchInput { advanceEventTime(500); up() }
        waitForIdle()

        assertEquals(0, activations)
        assertFalse(lock.isHeld())
    }

    // @spec CANVAS-WIDGETS-022, CANVAS-WIDGETS-024
    @Test
    fun keepsTheHoldUntilASuspendingActivationCompletes() = runComposeUiTest {
        val lock = InteractionLock()
        val started = CompletableDeferred<Unit>()
        val proceed = CompletableDeferred<Unit>()

        setContent {
            KidButton(
                onActivate = {
                    started.complete(Unit)
                    proceed.await()
                },
                lock = lock,
                modifier = Modifier.testTag(BUTTON_TAG),
            ) { Box(Modifier.size(64.dp)) }
        }
        val center = onNodeWithTag(BUTTON_TAG).fetchSemanticsNode().boundsInRoot.center

        onRoot().performTouchInput { down(center); up() }
        waitUntil { started.isCompleted }
        waitForIdle()

        // The pointer is long gone, but the activation is still running.
        assertTrue(lock.isHeld())

        proceed.complete(Unit)
        waitForIdle()
        assertFalse(lock.isHeld())
    }

    // @spec CANVAS-WIDGETS-022
    @Test
    fun releasesTheHoldWhenTheActivationDoesNotFinishNormally() = runComposeUiTest {
        val lock = InteractionLock()
        val started = CompletableDeferred<Unit>()
        var shown by mutableStateOf(true)

        setContent {
            if (shown) {
                KidButton(
                    onActivate = {
                        started.complete(Unit)
                        CompletableDeferred<Unit>().await() // never completes
                    },
                    lock = lock,
                    modifier = Modifier.testTag(BUTTON_TAG),
                ) { Box(Modifier.size(64.dp)) }
            }
        }
        val center = onNodeWithTag(BUTTON_TAG).fetchSemanticsNode().boundsInRoot.center

        onRoot().performTouchInput { down(center); up() }
        waitUntil { started.isCompleted }
        waitForIdle()
        assertTrue(lock.isHeld())

        // Leaving composition cancels the still-running activation. An
        // activation that throws instead takes the same `finally`, but the
        // throw itself propagates to the caller's scope by design, so it
        // isn't observable here without swallowing it.
        shown = false
        waitForIdle()

        assertFalse(lock.isHeld(), "an activation that never completed must not strand the hold")
    }

    // @spec CANVAS-WIDGETS-025
    @Test
    fun releasesTheHoldWhenItsOwnGestureIsCancelledMidPress() = runComposeUiTest {
        val lock = InteractionLock()
        var shown by mutableStateOf(true)

        setContent {
            if (shown) {
                KidButton(onActivate = {}, lock = lock, modifier = Modifier.testTag(BUTTON_TAG)) {
                    Box(Modifier.size(64.dp))
                }
            }
        }
        val center = onNodeWithTag(BUTTON_TAG).fetchSemanticsNode().boundsInRoot.center

        // Pointer stays down — the press is still live, and the hold with it.
        onRoot().performTouchInput { down(center) }
        assertTrue(lock.isHeld())

        // Leaving composition cancels the gesture before any release.
        shown = false
        waitForIdle()

        assertFalse(lock.isHeld(), "a cancelled press must not strand the hold")
    }

    // @spec CANVAS-WIDGETS-026
    @Test
    fun clearsPressFeedbackWhenItsPointerInputIsResetMidPress() = runComposeUiTest {
        var lock by mutableStateOf(InteractionLock())
        val pressedStates = mutableListOf<Boolean>()

        setContent {
            KidButton(onActivate = {}, lock = lock, modifier = Modifier.testTag(BUTTON_TAG)) { pressed ->
                pressedStates.add(pressed)
                Box(Modifier.size(64.dp))
            }
        }
        val center = onNodeWithTag(BUTTON_TAG).fetchSemanticsNode().boundsInRoot.center

        onRoot().performTouchInput { down(center) }
        waitForIdle()
        assertTrue(pressedStates.last(), "a claimed pointer shows press feedback")

        // A fresh lock instance re-keys `pointerInput`, resetting it under a
        // control that stays on screen — the cancellation a control outlives,
        // where nothing else clears the feedback on its way out.
        lock = InteractionLock()
        waitForIdle()

        assertFalse(pressedStates.last(), "a cancelled press must not stay lit")
    }

    // @spec CANVAS-WIDGETS-005
    @Test
    fun activatesWhenThePointerLiftsInsideAfterStrayingOutsideEarlier() = runComposeUiTest {
        val lock = InteractionLock()
        var activations = 0

        setContent {
            KidButton(onActivate = { activations++ }, lock = lock, modifier = Modifier.testTag(BUTTON_TAG)) {
                Box(Modifier.size(64.dp))
            }
        }
        val bounds = onNodeWithTag(BUTTON_TAG).fetchSemanticsNode().boundsInRoot

        onRoot().performTouchInput { down(0, bounds.center) }
        // Strays outside and stays there far longer than the tolerance allows...
        onRoot().performTouchInput { moveTo(0, Offset(bounds.right + 200f, bounds.center.y)) }
        // ...then returns and lifts inside. `updatePointerTo` repositions the
        // pointer without emitting a move of its own, so the return is carried
        // by the up event itself — where a real finger's last position lives.
        // The wait has to share this block with the up it delays; on its own it
        // applies to no event and the stray never grows.
        onRoot().performTouchInput { advanceEventTime(500); updatePointerTo(0, bounds.center); up(0) }
        waitForIdle()

        assertEquals(1, activations, "a pointer that lifts inside activates, whatever it did beforehand")
    }

    // @spec CANVAS-WIDGETS-020
    @Test
    fun ignoresASecondPointerArrivingWhileItHoldsTheInteraction() = runComposeUiTest {
        val lock = InteractionLock()
        var activations = 0

        setContent {
            KidButton(onActivate = { activations++ }, lock = lock, modifier = Modifier.testTag(BUTTON_TAG)) {
                Box(Modifier.size(64.dp))
            }
        }
        val center = onNodeWithTag(BUTTON_TAG).fetchSemanticsNode().boundsInRoot.center

        onRoot().performTouchInput { down(0, center) }
        // A second finger lands on the same control while it's tracking the first.
        onRoot().performTouchInput { down(1, center); up(1) }
        waitForIdle()
        assertEquals(0, activations, "the second pointer must not activate anything of its own")

        onRoot().performTouchInput { up(0) }
        waitForIdle()
        assertEquals(1, activations, "the claimed pointer's own release still activates")
        assertFalse(lock.isHeld())
    }
}
