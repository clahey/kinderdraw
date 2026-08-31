package net.clahey.kinderdraw.shared.widgets

import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.CompletableDeferred
import net.clahey.kinderdraw.shared.userexperience.InteractionLock

private const val BUTTON_TAG = "kid-button"

@OptIn(ExperimentalTestApi::class)
class KidButtonTest {
    // @spec CANVAS-WIDGETS-018, CANVAS-WIDGETS-019
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

        // The refusal holds for the rest of that gesture; a fresh press works.
        heldElsewhere.release()
        onRoot().performTouchInput { down(center); up() }
        waitForIdle()
        assertEquals(1, activations)
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
        assertNull(lock.tryAcquire())
        onRoot().performTouchInput { moveTo(Offset(bounds.right + 200f, bounds.bottom + 200f)) }
        onRoot().performTouchInput { advanceEventTime(500); up() }
        waitForIdle()

        assertEquals(0, activations)
        assertNotNull(lock.tryAcquire())
    }

    // @spec CANVAS-WIDGETS-022
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

        // The pointer is long gone, but the activation is still running.
        assertNull(lock.tryAcquire())

        proceed.complete(Unit)
        waitForIdle()
        assertNotNull(lock.tryAcquire())
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
        assertNull(lock.tryAcquire())

        // Leaving composition cancels the still-running activation. An
        // activation that throws instead takes the same `finally`, but the
        // throw itself propagates to the caller's scope by design, so it
        // isn't observable here without swallowing it.
        shown = false
        waitForIdle()

        assertNotNull(lock.tryAcquire(), "an activation that never completed must not strand the interaction")
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
        assertTrue(lock.tryAcquire() != null)
    }
}
