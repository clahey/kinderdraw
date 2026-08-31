package net.clahey.kinderdraw.shared.widgets

import kotlin.test.Test
import kotlin.test.assertEquals

/** Records every `onPressedChange`/`onActivate` call, in order, instead of acting on them. */
private class RecordingCallbacks {
    val pressedChanges = mutableListOf<Boolean>()
    var activateCount = 0

    val onPressedChange: (Boolean) -> Unit = { pressedChanges.add(it) }
    val onActivate: () -> Unit = { activateCount++ }
}

class PressStateTest {
    // @spec CANVAS-WIDGETS-001, CANVAS-WIDGETS-004
    @Test
    fun claimFiresPressedChangeTrueImmediately() {
        val callbacks = RecordingCallbacks()
        val state = PressState(callbacks.onPressedChange, callbacks.onActivate)

        state.onClaim(now = 0)

        assertEquals(listOf(true), callbacks.pressedChanges)
        assertEquals(0, callbacks.activateCount)
    }

    // @spec CANVAS-WIDGETS-005
    @Test
    fun releaseWhileStillInsideActivates() {
        val callbacks = RecordingCallbacks()
        val state = PressState(callbacks.onPressedChange, callbacks.onActivate)

        state.onClaim(now = 0)
        state.onRelease(now = 50)

        assertEquals(1, callbacks.activateCount)
    }

    // @spec CANVAS-WIDGETS-006
    @Test
    fun releaseAfterABriefStrayShorterThanThePrecedingInsideTimeStillActivates() {
        val callbacks = RecordingCallbacks()
        val state = PressState(callbacks.onPressedChange, callbacks.onActivate)

        state.onClaim(now = 0)
        // Held comfortably inside for 200ms, then drifted out right at lift-off.
        state.onPositionChanged(insideRegion = false, now = 200)
        state.onRelease(now = 250) // 50ms stray, well under the 100ms cap and under the 200ms inside time.

        assertEquals(1, callbacks.activateCount)
    }

    // @spec CANVAS-WIDGETS-007
    @Test
    fun releaseAfterAStrayAtOrAboveTheToleranceCapDoesNotActivate() {
        val callbacks = RecordingCallbacks()
        val state = PressState(callbacks.onPressedChange, callbacks.onActivate)

        state.onClaim(now = 0)
        state.onPositionChanged(insideRegion = false, now = 200)
        state.onRelease(now = 360) // 160ms stray - over the 100ms cap, even though inside time (200ms) was longer.

        assertEquals(0, callbacks.activateCount)
    }

    // @spec CANVAS-WIDGETS-007
    @Test
    fun releaseAfterAStrayNotShorterThanThePrecedingInsideTimeDoesNotActivate() {
        val callbacks = RecordingCallbacks()
        val state = PressState(callbacks.onPressedChange, callbacks.onActivate)

        state.onClaim(now = 0)
        // Only inside for 30ms before straying - a graze, not a settled press.
        state.onPositionChanged(insideRegion = false, now = 30)
        state.onRelease(now = 90) // 60ms stray - under the 100ms cap, but not shorter than the 30ms inside time.

        assertEquals(0, callbacks.activateCount)
    }

    // @spec CANVAS-WIDGETS-006, CANVAS-WIDGETS-007
    @Test
    fun onlyTheMostRecentEntryAndExitMatterAtRelease() {
        val callbacks = RecordingCallbacks()
        val state = PressState(callbacks.onPressedChange, callbacks.onActivate)

        state.onClaim(now = 0)
        state.onPositionChanged(insideRegion = false, now = 10) // An early, long-since-abandoned stray...
        state.onPositionChanged(insideRegion = true, now = 500) // ...but the pointer came back and settled in.
        state.onPositionChanged(insideRegion = false, now = 700) // Brief stray right at lift-off.
        state.onRelease(now = 750) // 50ms stray, shorter than the 200ms spent inside since re-entering at 500.

        assertEquals(1, callbacks.activateCount)
    }

    // @spec CANVAS-WIDGETS-026
    @Test
    fun pressedChangeFalseFiresOnReleaseRegardlessOfActivation() {
        val activating = RecordingCallbacks()
        PressState(activating.onPressedChange, activating.onActivate).apply {
            onClaim(now = 0)
            onRelease(now = 50)
        }
        assertEquals(listOf(true, false), activating.pressedChanges)

        val nonActivating = RecordingCallbacks()
        PressState(nonActivating.onPressedChange, nonActivating.onActivate).apply {
            onClaim(now = 0)
            onPositionChanged(insideRegion = false, now = 200)
            onRelease(now = 360)
        }
        assertEquals(listOf(true, false), nonActivating.pressedChanges)
    }

    // @spec CANVAS-WIDGETS-012, CANVAS-WIDGETS-013
    @Test
    fun activateFiresExactlyOnceOnlyWhenTheReleaseActivates() {
        val callbacks = RecordingCallbacks()
        val state = PressState(callbacks.onPressedChange, callbacks.onActivate)

        state.onClaim(now = 0)
        state.onRelease(now = 50)

        assertEquals(1, callbacks.activateCount)
    }

    // @spec CANVAS-WIDGETS-014
    @Test
    fun independentInstancesNeverInterfereWithEachOther() {
        val first = RecordingCallbacks()
        val second = RecordingCallbacks()
        val firstState = PressState(first.onPressedChange, first.onActivate)
        val secondState = PressState(second.onPressedChange, second.onActivate)

        firstState.onClaim(now = 0)
        firstState.onRelease(now = 50)

        assertEquals(1, first.activateCount)
        assertEquals(0, second.activateCount)
        assertEquals(emptyList(), second.pressedChanges)
    }
}
