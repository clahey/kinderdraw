package net.clahey.kinderdraw.shared.painting

import androidx.compose.ui.graphics.Color
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PaintingTest {
    private val p0 = Point(0.1f, 0.1f)
    private val p1 = Point(0.2f, 0.3f)
    private val p2 = Point(0.4f, 0.5f)

    // @spec CANVAS-PAINT-001
    @Test
    fun pointerDownQueriesActiveStrokeSettingsExactlyOnce() {
        val brush = FakeBrush()
        val settings = FakeActiveStrokeSettings(color = Color.Red, brush = brush)
        val painting = Painting()

        painting.onPointerDown(p0, settings)
        painting.onPointerMove(p1)
        painting.onPointerUp()

        assertEquals(1, settings.colorQueryCount)
        assertEquals(1, settings.brushQueryCount)
    }

    // @spec CANVAS-PAINT-001
    @Test
    fun strokeColorAndBrushStayFixedForItsDurationDespiteLaterSettingsChanges() {
        val originalBrush = FakeBrush()
        val laterBrush = FakeBrush()
        val settings = FakeActiveStrokeSettings(color = Color.Red, brush = originalBrush)
        val painting = Painting()

        painting.onPointerDown(p0, settings)
        settings.color = Color.Blue
        settings.brush = laterBrush
        painting.onPointerMove(p1)
        painting.onPointerUp()
        testDrawScope { with(painting) { render() } }

        assertEquals(1, originalBrush.renderCalls.size)
        assertEquals(Color.Red, originalBrush.renderCalls.single().color)
        assertTrue(laterBrush.renderCalls.isEmpty())
    }

    // @spec CANVAS-PAINT-002
    @Test
    fun tapWithNoMovementIsRecordedAsASinglePointStroke() {
        val brush = FakeBrush()
        val settings = FakeActiveStrokeSettings(brush = brush)
        val painting = Painting()

        painting.onPointerDown(p0, settings)
        painting.onPointerUp()

        assertFalse(painting.isEmpty())
        testDrawScope { with(painting) { render() } }
        assertEquals(listOf(p0), brush.renderCalls.single().points)
    }

    // @spec CANVAS-PAINT-003
    @Test
    fun drawingIsTheOrderedSetOfStrokesRecordedSinceLastClear() {
        val brush = FakeBrush()
        val settings = FakeActiveStrokeSettings(brush = brush)
        val painting = Painting()

        painting.onPointerDown(p0, settings)
        painting.onPointerUp()
        painting.onPointerDown(p1, settings)
        painting.onPointerMove(p2)
        painting.onPointerUp()
        testDrawScope { with(painting) { render() } }

        assertEquals(
            listOf(listOf(p0), listOf(p1, p2)),
            brush.renderCalls.map { it.points },
        )
    }

    // @spec CANVAS-PAINT-004
    @Test
    fun paintingPassesCapturedPointsToTheBrushUnconverted() {
        val brush = FakeBrush()
        val settings = FakeActiveStrokeSettings(brush = brush)
        val painting = Painting()

        painting.onPointerDown(p0, settings)
        painting.onPointerMove(p1)
        painting.onPointerUp()
        testDrawScope { with(painting) { render() } }

        assertEquals(listOf(p0, p1), brush.renderCalls.single().points)
    }

    // @spec CANVAS-PAINT-007
    @Test
    fun newlyCapturedPointsExtendTheLiveStrokesRenderingImmediately() {
        val brush = FakeBrush()
        val settings = FakeActiveStrokeSettings(brush = brush)
        val painting = Painting()

        painting.onPointerDown(p0, settings)
        testDrawScope { with(painting) { render() } }
        painting.onPointerMove(p1)
        testDrawScope { with(painting) { render() } }

        assertEquals(
            listOf(listOf(p0), listOf(p0, p1)),
            brush.renderCalls.map { it.points },
        )
    }

    // @spec CANVAS-PAINT-008
    @Test
    fun isEmptyReportsTrueOnlyWhenNoStrokesRecordedSinceLastClear() {
        val settings = FakeActiveStrokeSettings()
        val painting = Painting()

        assertTrue(painting.isEmpty())

        painting.onPointerDown(p0, settings)
        painting.onPointerUp()
        assertFalse(painting.isEmpty())

        painting.clear()
        assertTrue(painting.isEmpty())
    }

    // @spec CANVAS-PAINT-010
    @Test
    fun clearDiscardsAllStrokesAndResetsToBlank() {
        val brush = FakeBrush()
        val settings = FakeActiveStrokeSettings(brush = brush)
        val painting = Painting()

        painting.onPointerDown(p0, settings)
        painting.onPointerUp()
        painting.onPointerDown(p1, settings)
        painting.onPointerUp()

        painting.clear()

        assertTrue(painting.isEmpty())
        testDrawScope { with(painting) { render() } }
        assertTrue(brush.renderCalls.isEmpty())
    }

    // @spec CANVAS-PAINT-013
    @Test
    fun clearWhileAStrokeIsLiveFinalizesItAndReplacesItInheritingSettings() {
        val brush = FakeBrush()
        val settings = FakeActiveStrokeSettings(color = Color.Green, brush = brush)
        val painting = Painting()

        painting.onPointerDown(p0, settings)
        painting.onPointerMove(p1)
        painting.clear()

        // The replacement stroke carries the interrupted stroke's own settings
        // forward rather than asking Active Stroke Settings again.
        assertEquals(1, settings.colorQueryCount)
        assertEquals(1, settings.brushQueryCount)
        assertFalse(painting.isEmpty())

        painting.onPointerMove(p2)
        painting.onPointerUp()
        testDrawScope { with(painting) { render() } }

        // Continues from the interrupted stroke's last point (p1), not p0 or a fresh start.
        assertEquals(1, brush.renderCalls.size)
        assertEquals(listOf(p1, p2), brush.renderCalls.single().points)
        assertEquals(Color.Green, brush.renderCalls.single().color)
    }
}
