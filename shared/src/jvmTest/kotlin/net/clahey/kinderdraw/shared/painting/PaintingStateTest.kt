package net.clahey.kinderdraw.shared.painting

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import net.clahey.kinderdraw.shared.imagestorage.FakeImageStorage
import net.clahey.kinderdraw.shared.paintingstyle.FakeBrush
import net.clahey.kinderdraw.shared.paintingstyle.FakeStyleSettings
import net.clahey.kinderdraw.shared.paintingstyle.Point

class PaintingStateTest {
    private val p0 = Point(0.1f, 0.1f)
    private val p1 = Point(0.2f, 0.3f)
    private val p2 = Point(0.4f, 0.5f)
    private val pointerA = "pointer-a"
    private val pointerB = "pointer-b"

    // @spec CANVAS-PAINT-001
    @Test
    fun pointerDownQueriesActiveStrokeSettingsExactlyOnce() {
        val brush = FakeBrush()
        val settings = FakeStyleSettings(brush = brush)
        val painting = PaintingState(settings)

        painting.onPointerDown(pointerA, p0)
        painting.onPointerMove(pointerA, p1)
        painting.onPointerUp(pointerA)

        assertEquals(1, settings.brushQueryCount)
    }

    // @spec CANVAS-PAINT-001
    @Test
    fun strokeBrushStaysFixedForItsDurationDespiteLaterSettingsChanges() {
        val originalBrush = FakeBrush()
        val laterBrush = FakeBrush()
        val settings = FakeStyleSettings(brush = originalBrush)
        val painting = PaintingState(settings)

        painting.onPointerDown(pointerA, p0)
        settings.brush = laterBrush
        painting.onPointerMove(pointerA, p1)
        painting.onPointerUp(pointerA)
        testDrawScope { with(painting) { render() } }

        assertEquals(1, originalBrush.renderCalls.size)
        assertTrue(laterBrush.renderCalls.isEmpty())
    }

    // @spec CANVAS-PAINT-001, CANVAS-PAINT-020
    @Test
    fun aSecondPointersDownStartsItsOwnStrokeWithoutDisturbingTheFirst() {
        val brush = FakeBrush()
        val settings = FakeStyleSettings(brush = brush)
        val painting = PaintingState(settings)

        painting.onPointerDown(pointerA, p0)
        painting.onPointerDown(pointerB, p1)

        assertEquals(2, settings.brushQueryCount)
        testDrawScope { with(painting) { render() } }
        // Both pointers are still live; render order between concurrently-live
        // strokes isn't guaranteed, only that each pointer got its own stroke.
        assertEquals(setOf(listOf(p0), listOf(p1)), brush.renderCalls.toSet())
    }

    // @spec CANVAS-PAINT-020
    @Test
    fun eachConcurrentPointersMovementOnlyExtendsItsOwnStroke() {
        val brush = FakeBrush()
        val settings = FakeStyleSettings(brush = brush)
        val painting = PaintingState(settings)

        painting.onPointerDown(pointerA, p0)
        painting.onPointerDown(pointerB, p1)
        painting.onPointerMove(pointerA, p2)
        testDrawScope { with(painting) { render() } }

        assertEquals(setOf(listOf(p0, p2), listOf(p1)), brush.renderCalls.toSet())
    }

    // @spec CANVAS-PAINT-020
    @Test
    fun oneConcurrentPointersLiftOnlyCompletesItsOwnStrokeLeavingTheOtherLive() {
        val brush = FakeBrush()
        val settings = FakeStyleSettings(brush = brush)
        val painting = PaintingState(settings)

        painting.onPointerDown(pointerA, p0)
        painting.onPointerDown(pointerB, p1)
        painting.onPointerUp(pointerA)

        assertFalse(painting.isEmpty())

        painting.onPointerMove(pointerB, p2)
        painting.onPointerUp(pointerB)
        testDrawScope { with(painting) { render() } }

        assertEquals(listOf(listOf(p0), listOf(p1, p2)), brush.renderCalls)
    }

    // @spec CANVAS-PAINT-021
    @Test
    fun noUpperLimitIsImposedOnConcurrentLiveStrokes() {
        val brush = FakeBrush()
        val settings = FakeStyleSettings(brush = brush)
        val painting = PaintingState(settings)
        val pointers = (1..12).map { "pointer-$it" }

        pointers.forEach { painting.onPointerDown(it, p0) }
        assertEquals(pointers.size, settings.brushQueryCount)

        pointers.forEach { painting.onPointerUp(it) }
        testDrawScope { with(painting) { render() } }
        assertEquals(pointers.size, brush.renderCalls.size)
    }

    // @spec CANVAS-PAINT-002
    @Test
    fun tapWithNoMovementIsRecordedAsASinglePointStroke() {
        val brush = FakeBrush()
        val settings = FakeStyleSettings(brush = brush)
        val painting = PaintingState(settings)

        painting.onPointerDown(pointerA, p0)
        painting.onPointerUp(pointerA)

        assertFalse(painting.isEmpty())
        testDrawScope { with(painting) { render() } }
        assertEquals(listOf(p0), brush.renderCalls.single())
    }

    // @spec CANVAS-PAINT-003
    @Test
    fun drawingIsTheOrderedSetOfStrokesRecordedSinceLastClear() {
        val brush = FakeBrush()
        val settings = FakeStyleSettings(brush = brush)
        val painting = PaintingState(settings)

        painting.onPointerDown(pointerA, p0)
        painting.onPointerUp(pointerA)
        painting.onPointerDown(pointerA, p1)
        painting.onPointerMove(pointerA, p2)
        painting.onPointerUp(pointerA)
        testDrawScope { with(painting) { render() } }

        assertEquals(listOf(listOf(p0), listOf(p1, p2)), brush.renderCalls)
    }

    // @spec CANVAS-PAINT-004
    @Test
    fun paintingPassesCapturedPointsToTheBrushUnconverted() {
        val brush = FakeBrush()
        val settings = FakeStyleSettings(brush = brush)
        val painting = PaintingState(settings)

        painting.onPointerDown(pointerA, p0)
        painting.onPointerMove(pointerA, p1)
        painting.onPointerUp(pointerA)
        testDrawScope { with(painting) { render() } }

        assertEquals(listOf(p0, p1), brush.renderCalls.single())
    }

    // @spec CANVAS-PAINT-007
    @Test
    fun newlyCapturedPointsExtendTheLiveStrokesRenderingImmediately() {
        val brush = FakeBrush()
        val settings = FakeStyleSettings(brush = brush)
        val painting = PaintingState(settings)

        painting.onPointerDown(pointerA, p0)
        testDrawScope { with(painting) { render() } }
        painting.onPointerMove(pointerA, p1)
        testDrawScope { with(painting) { render() } }

        assertEquals(listOf(listOf(p0), listOf(p0, p1)), brush.renderCalls)
    }

    // @spec CANVAS-PAINT-008
    @Test
    fun isEmptyReportsTrueOnlyWhenNoStrokesRecordedSinceLastClear() {
        val settings = FakeStyleSettings()
        val painting = PaintingState(settings)

        assertTrue(painting.isEmpty())

        painting.onPointerDown(pointerA, p0)
        painting.onPointerUp(pointerA)
        assertFalse(painting.isEmpty())

        painting.clear()
        assertTrue(painting.isEmpty())
    }

    // @spec CANVAS-PAINT-010
    @Test
    fun clearDiscardsAllStrokesAndResetsToBlank() {
        val brush = FakeBrush()
        val settings = FakeStyleSettings(brush = brush)
        val painting = PaintingState(settings)

        painting.onPointerDown(pointerA, p0)
        painting.onPointerUp(pointerA)
        painting.onPointerDown(pointerA, p1)
        painting.onPointerUp(pointerA)

        painting.clear()

        assertTrue(painting.isEmpty())
        testDrawScope { with(painting) { render() } }
        assertTrue(brush.renderCalls.isEmpty())
    }

    // @spec CANVAS-PAINT-016
    @Test
    fun renderFillsTheDrawingSurfaceWithTheResolvedBackgroundBeforeStrokes() {
        val settings = FakeStyleSettings(background = Color.Red)
        val painting = PaintingState(settings)

        val image = testDrawScope(width = 4, height = 4) { with(painting) { render() } }

        assertEquals(Color.Red, image.toPixelMap()[0, 0])
    }

    // @spec CANVAS-PAINT-016
    @Test
    fun backgroundIsResolvedAtConstructionAndReusedUntilClear() {
        val settings = FakeStyleSettings(background = Color.Red)
        val painting = PaintingState(settings)

        assertEquals(1, settings.backgroundQueryCount)

        settings.background = Color.Blue
        val beforeClear = testDrawScope(width = 4, height = 4) { with(painting) { render() } }

        // Still red — a render() call alone doesn't re-query.
        assertEquals(Color.Red, beforeClear.toPixelMap()[0, 0])
        assertEquals(1, settings.backgroundQueryCount)
    }

    // @spec CANVAS-PAINT-010, CANVAS-PAINT-016
    @Test
    fun clearReResolvesTheBackground() {
        val settings = FakeStyleSettings(background = Color.Red)
        val painting = PaintingState(settings)
        settings.background = Color.Blue

        painting.clear()

        assertEquals(2, settings.backgroundQueryCount)
        val image = testDrawScope(width = 4, height = 4) { with(painting) { render() } }
        assertEquals(Color.Blue, image.toPixelMap()[0, 0])
    }

    // @spec CANVAS-PAINT-009
    @Test
    fun saveRasterizesTheDrawingAtItsLastRenderedSizeAndWritesItToImageStorage() = runBlocking {
        val brush = FakeBrush()
        val settings = FakeStyleSettings(brush = brush)
        val painting = PaintingState(settings)
        val imageStorage = FakeImageStorage()

        painting.onPointerDown(pointerA, p0)
        painting.onPointerUp(pointerA)
        testDrawScope(width = 40, height = 24) { with(painting) { render() } }

        val result = painting.save(imageStorage)

        assertTrue(result.isSuccess)
        val image = imageStorage.createCalls.single()
        assertEquals(40, image.width)
        assertEquals(24, image.height)
    }

    // @spec CANVAS-PAINT-009
    @Test
    fun saveBeforeAnyRenderCallDoesNotCrash() = runBlocking {
        val settings = FakeStyleSettings(brush = FakeBrush())
        val painting = PaintingState(settings)
        val imageStorage = FakeImageStorage()

        painting.onPointerDown(pointerA, p0)
        painting.onPointerUp(pointerA)
        // No render() call before save() — the drawing surface's size is unknown.

        val result = painting.save(imageStorage)

        assertTrue(result.isSuccess)
    }

    // @spec CANVAS-PAINT-009
    @Test
    fun saveRerendersEveryStrokeThroughItsBrushRatherThanCapturingOnScreenPixels() = runBlocking {
        val brush = FakeBrush()
        val settings = FakeStyleSettings(brush = brush)
        val painting = PaintingState(settings)
        val imageStorage = FakeImageStorage()

        painting.onPointerDown(pointerA, p0)
        painting.onPointerMove(pointerA, p1)
        painting.onPointerUp(pointerA)
        testDrawScope { with(painting) { render() } }

        painting.save(imageStorage)

        // The on-screen render() call above already recorded one entry;
        // save()'s own off-screen rasterization replays the same render path.
        assertEquals(2, brush.renderCalls.size)
        assertEquals(listOf(p0, p1), brush.renderCalls.last())
    }

    // @spec CANVAS-PAINT-016
    @Test
    fun saveIncludesTheResolvedBackgroundMatchingOnScreenRendering() = runBlocking {
        val settings = FakeStyleSettings(background = Color.Red)
        val painting = PaintingState(settings)
        val imageStorage = FakeImageStorage()

        painting.onPointerDown(pointerA, p0)
        painting.onPointerUp(pointerA)
        testDrawScope(width = 4, height = 4) { with(painting) { render() } }

        painting.save(imageStorage)

        // Reuses the same render() path as on-screen, so the saved image's
        // background matches by construction, not by keeping two paths in sync.
        val savedImage = imageStorage.createCalls.single()
        assertEquals(Color.Red, savedImage.toPixelMap()[0, 0])
    }

    // @spec CANVAS-PAINT-009
    @Test
    fun saveWithoutAnIdCreatesANewEntryAndReturnsItsId() = runBlocking {
        val settings = FakeStyleSettings()
        val painting = PaintingState(settings)
        val imageStorage = FakeImageStorage()

        painting.onPointerDown(pointerA, p0)
        painting.onPointerUp(pointerA)
        testDrawScope { with(painting) { render() } }

        val result = painting.save(imageStorage)

        assertEquals(Result.success("id"), result)
        assertEquals(1, imageStorage.createCalls.size)
        assertTrue(imageStorage.updateCalls.isEmpty())
    }

    // @spec CANVAS-PAINT-017
    @Test
    fun saveWithAnIdUpdatesTheExistingEntryAndReturnsTheSameId() = runBlocking {
        val settings = FakeStyleSettings()
        val painting = PaintingState(settings)
        val imageStorage = FakeImageStorage()

        painting.onPointerDown(pointerA, p0)
        painting.onPointerUp(pointerA)
        testDrawScope { with(painting) { render() } }

        val result = painting.save(imageStorage, id = "existing-id")

        assertEquals(Result.success("existing-id"), result)
        assertEquals("existing-id", imageStorage.updateCalls.single().first)
        assertTrue(imageStorage.createCalls.isEmpty())
    }

    // @spec CANVAS-PAINT-012, CANVAS-PAINT-017
    @Test
    fun saveWithAnIdReportsImageStorageFailureToItsOwnCaller() = runBlocking {
        val settings = FakeStyleSettings()
        val painting = PaintingState(settings)
        val imageStorage = FakeImageStorage()
        imageStorage.failNextUpdate("no such entry")

        painting.onPointerDown(pointerA, p0)
        painting.onPointerUp(pointerA)
        testDrawScope { with(painting) { render() } }

        val result = painting.save(imageStorage, id = "missing-id")

        assertTrue(result.isFailure)
        assertEquals("no such entry", result.exceptionOrNull()?.message)
    }

    // @spec CANVAS-PAINT-012
    @Test
    fun saveReportsImageStorageFailureToItsOwnCallerRatherThanTreatingTheDrawingAsSaved() = runBlocking {
        val brush = FakeBrush()
        val settings = FakeStyleSettings(brush = brush)
        val painting = PaintingState(settings)
        val imageStorage = FakeImageStorage()
        imageStorage.failNextCreate("disk full")

        painting.onPointerDown(pointerA, p0)
        painting.onPointerUp(pointerA)
        testDrawScope { with(painting) { render() } }

        val result = painting.save(imageStorage)

        assertTrue(result.isFailure)
        assertEquals("disk full", result.exceptionOrNull()?.message)
    }

    // @spec CANVAS-PAINT-013
    @Test
    fun clearWhileAStrokeIsLiveFinalizesItAndReplacesItInheritingSettings() {
        val brush = FakeBrush()
        val settings = FakeStyleSettings(brush = brush)
        val painting = PaintingState(settings)

        painting.onPointerDown(pointerA, p0)
        painting.onPointerMove(pointerA, p1)
        painting.clear()

        // The replacement stroke carries the interrupted stroke's own brush
        // forward rather than asking Active Stroke Settings again.
        assertEquals(1, settings.brushQueryCount)
        assertFalse(painting.isEmpty())

        painting.onPointerMove(pointerA, p2)
        painting.onPointerUp(pointerA)
        testDrawScope { with(painting) { render() } }

        // Continues from the interrupted stroke's last point (p1), not p0 or a fresh start.
        assertEquals(listOf(listOf(p1, p2)), brush.renderCalls)
    }

    // @spec CANVAS-PAINT-013
    @Test
    fun clearWhileTwoStrokesAreLiveFinalizesAndReplacesEachIndependently() {
        val brush = FakeBrush()
        val settings = FakeStyleSettings(brush = brush)
        val painting = PaintingState(settings)

        painting.onPointerDown(pointerA, p0)
        painting.onPointerDown(pointerB, p1)
        painting.clear()

        // Each replacement stroke carries its own interrupted stroke's brush
        // forward rather than asking Active Stroke Settings again.
        assertEquals(2, settings.brushQueryCount)
        assertFalse(painting.isEmpty())

        painting.onPointerMove(pointerA, p2)
        painting.onPointerUp(pointerA)
        painting.onPointerUp(pointerB)
        testDrawScope { with(painting) { render() } }

        // Pointer A continues from p0 (its own last point), pointer B from p1 — never confused.
        assertEquals(listOf(listOf(p0, p2), listOf(p1)), brush.renderCalls)
    }
}
