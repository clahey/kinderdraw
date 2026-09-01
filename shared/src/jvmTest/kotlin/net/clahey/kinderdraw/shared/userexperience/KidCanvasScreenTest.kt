package net.clahey.kinderdraw.shared.userexperience

import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.test.ExperimentalTestApi
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
