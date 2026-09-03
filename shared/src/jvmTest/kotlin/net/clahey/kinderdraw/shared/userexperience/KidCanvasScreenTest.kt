package net.clahey.kinderdraw.shared.userexperience

import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.v2.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.CompletableDeferred
import net.clahey.kinderdraw.shared.imagestorage.FakeImageStorage
import net.clahey.kinderdraw.shared.imagestorage.ImageStorage
import net.clahey.kinderdraw.shared.imagestorage.SavedDrawingEntry
import net.clahey.kinderdraw.shared.painting.PaintingState
import net.clahey.kinderdraw.shared.paintingstyle.FakeBrush
import net.clahey.kinderdraw.shared.paintingstyle.FakeStyleSettings
import net.clahey.kinderdraw.shared.paintingstyle.Point

/** Wraps [FakeImageStorage], suspending inside [create] until the test releases it. */
private class GatedImageStorage(private val delegate: FakeImageStorage) : ImageStorage by delegate {
    val createStarted = CompletableDeferred<Unit>()
    private val proceed = CompletableDeferred<Unit>()

    override suspend fun create(image: ImageBitmap, timestamp: Long?): Result<SavedDrawingEntry> {
        createStarted.complete(Unit)
        proceed.await()
        return delegate.create(image, timestamp)
    }

    fun release() = proceed.complete(Unit)
}

/**
 * Steps the clock frame by frame until [condition] holds, then leaves it
 * stopped there. The save feedback is transient, and with the clock advancing
 * on its own an animation can begin and end inside a single idle wait — so a
 * poll-based wait misses it entirely. Requires `mainClock.autoAdvance = false`.
 */
@OptIn(ExperimentalTestApi::class)
private fun ComposeUiTest.advanceUntil(frames: Int = 240, condition: () -> Boolean) {
    repeat(frames) {
        if (condition()) return
        mainClock.advanceTimeByFrame()
    }
    throw AssertionError("condition still not met after $frames frames")
}

@OptIn(ExperimentalTestApi::class)
private fun ComposeUiTest.hasNode(tag: String) = onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()

@OptIn(ExperimentalTestApi::class)
class KidCanvasScreenTest {
    private val p0 = Point(0.1f, 0.1f)
    private val pointerA = PointerId(0L)

    // @spec CANVAS-UX-001, CANVAS-UX-002, CANVAS-UX-009, CANVAS-UX-010, CANVAS-UX-011, CANVAS-UX-013
    @Test
    fun newPictureSavesThenClearsWhenTheDrawingIsNotEmpty() = runComposeUiTest {
        val settings = FakeStyleSettings(brush = FakeBrush())
        val state = PaintingState(settings)
        state.onPointerDown(pointerA, p0)
        state.onPointerUp(pointerA)
        val imageStorage = FakeImageStorage()

        setContent { KidCanvasScreen(imageStorage = imageStorage, state = state) }
        val buttonCenter = onNodeWithTag(NEW_PICTURE_TEST_TAG).fetchSemanticsNode().boundsInRoot.center
        onRoot().performTouchInput { down(0, buttonCenter); up(0) }
        waitForIdle()

        assertEquals(1, imageStorage.createCalls.size)
        assertTrue(state.isEmpty())
    }

    // @spec CANVAS-UX-012, CANVAS-UX-013
    @Test
    fun newPictureSkipsSaveWhenTheDrawingIsEmpty() = runComposeUiTest {
        val settings = FakeStyleSettings()
        val state = PaintingState(settings)
        val imageStorage = FakeImageStorage()

        setContent { KidCanvasScreen(imageStorage = imageStorage, state = state) }
        val buttonCenter = onNodeWithTag(NEW_PICTURE_TEST_TAG).fetchSemanticsNode().boundsInRoot.center
        onRoot().performTouchInput { down(0, buttonCenter); up(0) }
        waitForIdle()

        assertTrue(imageStorage.createCalls.isEmpty())
        assertTrue(imageStorage.updateCalls.isEmpty())
        // Constructed once, re-resolved once by clear() — proof clear() still ran.
        assertEquals(2, settings.backgroundQueryCount)
    }

    // @spec CANVAS-UX-003, CANVAS-UX-004
    @Test
    fun newPictureIsBlockedWhileAStrokeIsActiveOnPainting() = runComposeUiTest {
        val settings = FakeStyleSettings(brush = FakeBrush())
        val state = PaintingState(settings)
        val imageStorage = FakeImageStorage()

        setContent { KidCanvasScreen(imageStorage = imageStorage, state = state) }
        val buttonCenter = onNodeWithTag(NEW_PICTURE_TEST_TAG).fetchSemanticsNode().boundsInRoot.center

        onRoot().performTouchInput { down(0, Offset(5f, 5f)) } // starts a stroke, pointer stays down
        onRoot().performTouchInput { down(1, buttonCenter); up(1) } // attempted tap on New Picture mid-stroke
        onRoot().performTouchInput { up(0) } // finishes the stroke normally
        waitForIdle()

        assertTrue(imageStorage.createCalls.isEmpty()) // New Picture never activated
        assertFalse(state.isEmpty()) // the stroke itself completed, untouched by the refused tap
    }

    // @spec CANVAS-UX-024
    @Test
    fun aRecreatedScreenStartsWithTheInteractionUnheld() = runComposeUiTest {
        val settings = FakeStyleSettings(brush = FakeBrush())
        val state = PaintingState(settings)
        val imageStorage = FakeImageStorage()
        var generation by mutableStateOf(0)

        setContent {
            // A changing key rebuilds the screen the way an OS-driven
            // recreation does, discarding everything it held in `remember`.
            key(generation) { KidCanvasScreen(imageStorage = imageStorage, state = state) }
        }

        // Leave a stroke live, so the old screen's lock was held when it went away.
        onRoot().performTouchInput { down(0, Offset(5f, 5f)) }
        generation = 1
        waitForIdle()

        // The rebuilt screen accepts a fresh touch, which it could not do if a
        // hold had survived with nothing left alive to release it.
        onRoot().performTouchInput { down(1, Offset(20f, 20f)); up(1) }
        waitForIdle()
        assertFalse(state.isEmpty())
    }

    // @spec CANVAS-UX-005
    @Test
    fun aFingerHeldThroughTheNewPictureSequenceStaysInertAfterItCompletes() = runComposeUiTest {
        val settings = FakeStyleSettings(brush = FakeBrush())
        val state = PaintingState(settings)
        state.onPointerDown(pointerA, p0)
        state.onPointerUp(pointerA)
        val imageStorage = GatedImageStorage(FakeImageStorage())

        setContent { KidCanvasScreen(imageStorage = imageStorage, state = state) }
        val buttonCenter = onNodeWithTag(NEW_PICTURE_TEST_TAG).fetchSemanticsNode().boundsInRoot.center

        onRoot().performTouchInput { down(0, buttonCenter); up(0) }
        waitUntil { imageStorage.createStarted.isCompleted }

        // A finger lands on the canvas mid-sequence and stays down throughout.
        onRoot().performTouchInput { down(1, Offset(5f, 5f)) }
        imageStorage.release()
        waitForIdle()
        assertTrue(state.isEmpty()) // the sequence finished and cleared

        // That same finger, still down, must not spring into a stroke now
        // that the hold is gone — only a fresh touch-down is eligible.
        onRoot().performTouchInput { moveTo(1, Offset(20f, 20f)) }
        assertTrue(state.isEmpty())

        onRoot().performTouchInput { up(1) }
        onRoot().performTouchInput { down(2, Offset(30f, 30f)) }
        assertFalse(state.isEmpty())
    }

    // @spec CANVAS-UX-028
    @Test
    fun aFailedSaveIsRetriedExactlyOnce() = runComposeUiTest {
        val settings = FakeStyleSettings(brush = FakeBrush())
        val state = PaintingState(settings)
        state.onPointerDown(pointerA, p0)
        state.onPointerUp(pointerA)
        val imageStorage = FakeImageStorage()
        imageStorage.failNextCreates(count = 2, message = "disk full")

        setContent { KidCanvasScreen(imageStorage = imageStorage, state = state) }
        val buttonCenter = onNodeWithTag(NEW_PICTURE_TEST_TAG).fetchSemanticsNode().boundsInRoot.center
        onRoot().performTouchInput { down(0, buttonCenter); up(0) }
        waitForIdle()

        assertEquals(2, imageStorage.createCalls.size, "one attempt plus exactly one retry")
    }

    // @spec CANVAS-UX-028, CANVAS-UX-013
    @Test
    fun aRetryThatSucceedsSavesAndClears() = runComposeUiTest {
        val settings = FakeStyleSettings(brush = FakeBrush())
        val state = PaintingState(settings)
        state.onPointerDown(pointerA, p0)
        state.onPointerUp(pointerA)
        val imageStorage = FakeImageStorage()
        imageStorage.failNextCreates(count = 1, message = "transient I/O error")

        setContent { KidCanvasScreen(imageStorage = imageStorage, state = state) }
        val buttonCenter = onNodeWithTag(NEW_PICTURE_TEST_TAG).fetchSemanticsNode().boundsInRoot.center
        onRoot().performTouchInput { down(0, buttonCenter); up(0) }
        waitForIdle()

        assertEquals(2, imageStorage.createCalls.size)
        assertTrue(state.isEmpty(), "a save that succeeded on the retry still clears")
    }

    // @spec CANVAS-UX-029, CANVAS-UX-019
    @Test
    fun aDrawingThatCouldNotBeSavedIsLeftOnTheCanvas() = runComposeUiTest {
        val settings = FakeStyleSettings(brush = FakeBrush())
        val state = PaintingState(settings)
        state.onPointerDown(pointerA, p0)
        state.onPointerUp(pointerA)
        val imageStorage = FakeImageStorage()
        // Enough failures to cover both presses below, so neither can save.
        imageStorage.failNextCreates(count = 4, message = "disk full")

        setContent { KidCanvasScreen(imageStorage = imageStorage, state = state) }
        val buttonCenter = onNodeWithTag(NEW_PICTURE_TEST_TAG).fetchSemanticsNode().boundsInRoot.center
        onRoot().performTouchInput { down(0, buttonCenter); up(0) }
        waitForIdle()

        assertFalse(state.isEmpty(), "an unsaved drawing must survive the sequence that couldn't save it")

        // The hold ends with the sequence however it ended, so the control is
        // usable again — a second press runs the whole sequence over.
        onRoot().performTouchInput { down(1, buttonCenter); up(1) }
        waitForIdle()
        assertEquals(4, imageStorage.createCalls.size)
    }

    // @spec CANVAS-UX-030, CANVAS-UX-031, CANVAS-UX-041, CANVAS-UX-042, CANVAS-UX-013
    @Test
    fun aSavedDrawingGoesIntoTheButtonAndAFreshSheetArrives() = runComposeUiTest {
        val settings = FakeStyleSettings(brush = FakeBrush())
        val state = PaintingState(settings)
        state.onPointerDown(pointerA, p0)
        state.onPointerUp(pointerA)
        val imageStorage = FakeImageStorage()

        setContent { KidCanvasScreen(imageStorage = imageStorage, state = state) }
        val buttonCenter = onNodeWithTag(NEW_PICTURE_TEST_TAG).fetchSemanticsNode().boundsInRoot.center
        mainClock.autoAdvance = false
        onRoot().performTouchInput { down(0, buttonCenter); up(0) }

        advanceUntil { hasNode(SAVE_FLIGHT_TEST_TAG) }
        onNodeWithTag(SAVE_FLIGHT_COVER_TEST_TAG).assertExists()

        // The departing drawing goes away and a fresh sheet takes its place.
        advanceUntil { hasNode(SAVE_ARRIVAL_TEST_TAG) }
        onNodeWithTag(SAVE_FLIGHT_TEST_TAG).assertDoesNotExist()
        assertTrue(state.isEmpty(), "the clear committed before the sheet was photographed")

        mainClock.autoAdvance = true
        waitForIdle()
        onNodeWithTag(SAVE_ARRIVAL_TEST_TAG).assertDoesNotExist()
        onNodeWithTag(SAVE_FLIGHT_COVER_TEST_TAG).assertDoesNotExist()
        assertTrue(state.isEmpty())
    }

    // @spec CANVAS-UX-040
    @Test
    fun theDrawingWaitsAboveTheButtonWhileTheWriteIsStillRunning() = runComposeUiTest {
        val settings = FakeStyleSettings(brush = FakeBrush())
        val state = PaintingState(settings)
        state.onPointerDown(pointerA, p0)
        state.onPointerUp(pointerA)
        val imageStorage = GatedImageStorage(FakeImageStorage())

        setContent { KidCanvasScreen(imageStorage = imageStorage, state = state) }
        val buttonCenter = onNodeWithTag(NEW_PICTURE_TEST_TAG).fetchSemanticsNode().boundsInRoot.center
        mainClock.autoAdvance = false
        onRoot().performTouchInput { down(0, buttonCenter); up(0) }

        advanceUntil { hasNode(SAVE_FLIGHT_TEST_TAG) }
        // Long past the lift's own duration, with the write still outstanding.
        repeat(120) { mainClock.advanceTimeByFrame() }
        onNodeWithTag(SAVE_FLIGHT_TEST_TAG).assertExists()
        onNodeWithTag(SAVE_ARRIVAL_TEST_TAG).assertDoesNotExist()
        assertFalse(state.isEmpty(), "nothing has been cleared, because nothing has been written")

        imageStorage.release()
        mainClock.autoAdvance = true
        waitForIdle()
        onNodeWithTag(SAVE_FLIGHT_TEST_TAG).assertDoesNotExist()
        assertTrue(state.isEmpty())
    }

    // @spec CANVAS-UX-043
    @Test
    fun theDrawingHopsClearOfTheButtonOnceTheOutcomeArrives() = runComposeUiTest {
        val settings = FakeStyleSettings(brush = FakeBrush())
        val state = PaintingState(settings)
        state.onPointerDown(pointerA, p0)
        state.onPointerUp(pointerA)
        val imageStorage = GatedImageStorage(FakeImageStorage())

        setContent { KidCanvasScreen(imageStorage = imageStorage, state = state) }
        val buttonCenter = onNodeWithTag(NEW_PICTURE_TEST_TAG).fetchSemanticsNode().boundsInRoot.center
        mainClock.autoAdvance = false
        onRoot().performTouchInput { down(0, buttonCenter); up(0) }

        // Held at the button by the outstanding write, so this is where it rests.
        advanceUntil { hasNode(SAVE_FLIGHT_TEST_TAG) }
        repeat(60) { mainClock.advanceTimeByFrame() }
        val resting = onNodeWithTag(SAVE_FLIGHT_TEST_TAG).fetchSemanticsNode().boundsInRoot

        imageStorage.release()
        // The answer's first effect is upward, away from the button — the hop
        // that both ends the wait and clears the way for the descent.
        advanceUntil {
            hasNode(SAVE_FLIGHT_TEST_TAG) &&
                onNodeWithTag(SAVE_FLIGHT_TEST_TAG).fetchSemanticsNode().boundsInRoot.top < resting.top
        }
    }

    // @spec CANVAS-UX-038, CANVAS-UX-040
    @Test
    fun theTravellingDrawingStartsOutCoveringTheCanvasExactly() = runComposeUiTest {
        val settings = FakeStyleSettings(brush = FakeBrush())
        val state = PaintingState(settings)
        state.onPointerDown(pointerA, p0)
        state.onPointerUp(pointerA)
        val imageStorage = FakeImageStorage()

        setContent { KidCanvasScreen(imageStorage = imageStorage, state = state) }
        val rootBounds = onRoot().fetchSemanticsNode().boundsInRoot
        val buttonCenter = onNodeWithTag(NEW_PICTURE_TEST_TAG).fetchSemanticsNode().boundsInRoot.center

        // Step frame by frame so the flight's first frame can be inspected
        // before any of the animation has been applied to it.
        mainClock.autoAdvance = false
        onRoot().performTouchInput { down(0, buttonCenter); up(0) }
        advanceUntil { hasNode(SAVE_FLIGHT_TEST_TAG) }

        val first = onNodeWithTag(SAVE_FLIGHT_TEST_TAG).fetchSemanticsNode().boundsInRoot
        assertEquals(rootBounds, first, "the flight's first frame must be indistinguishable from the canvas")
    }

    // @spec CANVAS-UX-032, CANVAS-UX-029, CANVAS-UX-042
    @Test
    fun aFailedSaveReboundsWithNoSheetArrivingAndRestoresTheDrawing() = runComposeUiTest {
        val settings = FakeStyleSettings(brush = FakeBrush())
        val state = PaintingState(settings)
        state.onPointerDown(pointerA, p0)
        state.onPointerUp(pointerA)
        val imageStorage = FakeImageStorage()
        imageStorage.failNextCreates(count = 2, message = "disk full")

        setContent { KidCanvasScreen(imageStorage = imageStorage, state = state) }
        val buttonCenter = onNodeWithTag(NEW_PICTURE_TEST_TAG).fetchSemanticsNode().boundsInRoot.center
        mainClock.autoAdvance = false
        onRoot().performTouchInput { down(0, buttonCenter); up(0) }

        advanceUntil { hasNode(SAVE_FLIGHT_TEST_TAG) }
        onNodeWithTag(SAVE_FLIGHT_COVER_TEST_TAG).assertExists()
        assertFalse(state.isEmpty())

        mainClock.autoAdvance = true
        waitForIdle()
        onNodeWithTag(SAVE_FLIGHT_TEST_TAG).assertDoesNotExist()
        onNodeWithTag(SAVE_FLIGHT_COVER_TEST_TAG).assertDoesNotExist()
        // Nothing was taken away, so nothing arrives to replace it.
        onNodeWithTag(SAVE_ARRIVAL_TEST_TAG).assertDoesNotExist()
        assertFalse(state.isEmpty(), "the drawing is back, untouched")
    }

    // @spec CANVAS-UX-033
    @Test
    fun aFailedSaveFlashesRed() = runComposeUiTest {
        val settings = FakeStyleSettings(brush = FakeBrush())
        val state = PaintingState(settings)
        state.onPointerDown(pointerA, p0)
        state.onPointerUp(pointerA)
        val imageStorage = FakeImageStorage()
        imageStorage.failNextCreates(count = 2, message = "disk full")

        setContent { KidCanvasScreen(imageStorage = imageStorage, state = state) }
        val buttonCenter = onNodeWithTag(NEW_PICTURE_TEST_TAG).fetchSemanticsNode().boundsInRoot.center
        mainClock.autoAdvance = false
        onRoot().performTouchInput { down(0, buttonCenter); up(0) }

        advanceUntil { hasNode(SAVE_FAILURE_FLASH_TEST_TAG) }

        mainClock.autoAdvance = true
        waitForIdle()
        onNodeWithTag(SAVE_FAILURE_FLASH_TEST_TAG).assertDoesNotExist()
    }

    // @spec CANVAS-UX-036
    @Test
    fun anEmptyCanvasShowsNoSaveFeedback() = runComposeUiTest {
        val settings = FakeStyleSettings()
        val state = PaintingState(settings)
        val imageStorage = FakeImageStorage()

        setContent { KidCanvasScreen(imageStorage = imageStorage, state = state) }
        val buttonCenter = onNodeWithTag(NEW_PICTURE_TEST_TAG).fetchSemanticsNode().boundsInRoot.center

        mainClock.autoAdvance = false
        onRoot().performTouchInput { down(0, buttonCenter); up(0) }
        repeat(120) {
            mainClock.advanceTimeByFrame()
            assertTrue(
                !hasNode(SAVE_FLIGHT_TEST_TAG) && !hasNode(SAVE_ARRIVAL_TEST_TAG) &&
                    !hasNode(SAVE_FLIGHT_COVER_TEST_TAG) && !hasNode(SAVE_FAILURE_FLASH_TEST_TAG),
                "a sequence that wrote nothing has nothing to acknowledge",
            )
        }
    }

    // @spec CANVAS-UX-037
    @Test
    fun reducedMotionKeepsTheFlashAndDropsTheFlight() = runComposeUiTest {
        val settings = FakeStyleSettings(brush = FakeBrush())
        val state = PaintingState(settings)
        state.onPointerDown(pointerA, p0)
        state.onPointerUp(pointerA)
        val imageStorage = FakeImageStorage()
        imageStorage.failNextCreates(count = 2, message = "disk full")

        setContent {
            KidCanvasScreen(imageStorage = imageStorage, state = state, reduceMotion = true)
        }
        val buttonCenter = onNodeWithTag(NEW_PICTURE_TEST_TAG).fetchSemanticsNode().boundsInRoot.center
        mainClock.autoAdvance = false
        onRoot().performTouchInput { down(0, buttonCenter); up(0) }

        // The failure still says something — it is the outcome with nothing
        // else to fall back on, since no canvas clears to speak for it.
        advanceUntil { hasNode(SAVE_FAILURE_FLASH_TEST_TAG) }
        onNodeWithTag(SAVE_FLIGHT_TEST_TAG).assertDoesNotExist()
        onNodeWithTag(SAVE_FLIGHT_COVER_TEST_TAG).assertDoesNotExist()

        mainClock.autoAdvance = true
        waitForIdle()
        assertFalse(state.isEmpty())
    }

    // @spec CANVAS-UX-035, CANVAS-UX-004
    @Test
    fun noStrokeStartsWhileTheDrawingIsTravelling() = runComposeUiTest {
        val settings = FakeStyleSettings(brush = FakeBrush())
        val state = PaintingState(settings)
        state.onPointerDown(pointerA, p0)
        state.onPointerUp(pointerA)
        val imageStorage = FakeImageStorage()

        setContent { KidCanvasScreen(imageStorage = imageStorage, state = state) }
        val buttonCenter = onNodeWithTag(NEW_PICTURE_TEST_TAG).fetchSemanticsNode().boundsInRoot.center
        mainClock.autoAdvance = false
        onRoot().performTouchInput { down(0, buttonCenter); up(0) }

        advanceUntil { hasNode(SAVE_FLIGHT_TEST_TAG) }
        // A touch landing on the travelling drawing reaches neither it nor
        // Painting underneath: it takes no input, and the hold covers the flight.
        onRoot().performTouchInput { down(1, Offset(5f, 5f)); up(1) }

        mainClock.autoAdvance = true
        waitForIdle()
        assertTrue(state.isEmpty(), "the touch started no stroke on the freshly cleared canvas")
    }

    // @spec CANVAS-UX-004, CANVAS-UX-009, CANVAS-UX-019
    @Test
    fun newPictureSequenceBlocksNewStrokesOnPaintingUntilItCompletes() = runComposeUiTest {
        val settings = FakeStyleSettings(brush = FakeBrush())
        val state = PaintingState(settings)
        state.onPointerDown(pointerA, p0)
        state.onPointerUp(pointerA)
        val queryCountBeforeAttempt = settings.brushQueryCount
        val imageStorage = GatedImageStorage(FakeImageStorage())

        setContent { KidCanvasScreen(imageStorage = imageStorage, state = state) }
        val buttonCenter = onNodeWithTag(NEW_PICTURE_TEST_TAG).fetchSemanticsNode().boundsInRoot.center

        onRoot().performTouchInput { down(0, buttonCenter); up(0) }
        waitUntil { imageStorage.createStarted.isCompleted }

        // Attempt a new stroke while the sequence still holds the arbiter, mid-save.
        onRoot().performTouchInput { down(1, Offset(5f, 5f)); up(1) }
        waitForIdle()
        assertEquals(queryCountBeforeAttempt, settings.brushQueryCount) // never reached Painting

        imageStorage.release()
        waitForIdle()

        assertTrue(state.isEmpty()) // clear() ran once the sequence was allowed to finish
    }
}
