package net.clahey.kinderdraw.shared.paintingstyle

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.clahey.kinderdraw.shared.painting.testDrawScope

/** Records the point list and color passed to [render] on each call, instead of actually drawing anything. */
private class RecordingSimpleBrush(colorSource: ColorSource) : AbstractSimpleBrush(colorSource) {
    val renderCalls = mutableListOf<Pair<List<Point>, Color>>()

    override fun DrawScope.render(points: List<Point>, color: Color) {
        renderCalls.add(points to color)
    }
}

class AbstractSimpleBrushTest {
    // @spec CANVAS-STYLE-001
    @Test
    fun addPointExtendsWhatTheStrokeRenders() {
        val brush = RecordingSimpleBrush(ConstantColor(Color.Red))
        val stroke = brush.startStroke(Point(0.1f, 0.5f))
        testDrawScope { with(stroke) { render() } }

        stroke.addPoint(Point(0.9f, 0.5f))
        testDrawScope { with(stroke) { render() } }

        assertEquals(
            listOf(
                listOf(Point(0.1f, 0.5f)) to Color.Red,
                listOf(Point(0.1f, 0.5f), Point(0.9f, 0.5f)) to Color.Red,
            ),
            brush.renderCalls,
        )
    }

    // @spec CANVAS-STYLE-012
    @Test
    fun eachStrokeResolvesItsOwnColorFromTheColorSource() {
        val colorSource = FakeColorSource(listOf(Color.Red, Color.Blue))
        val brush = RecordingSimpleBrush(colorSource)

        val first = brush.startStroke(Point(0.1f, 0.5f))
        val second = brush.startStroke(Point(0.2f, 0.5f))
        testDrawScope { with(first) { render() } }
        testDrawScope { with(second) { render() } }

        assertEquals(Color.Red, brush.renderCalls[0].second)
        assertEquals(Color.Blue, brush.renderCalls[1].second)
    }

    // @spec CANVAS-STYLE-011
    @Test
    fun restartProducesAStrokeContinuingFromTheLastPoint() {
        val brush = RecordingSimpleBrush(ConstantColor(Color.Red))
        val stroke = brush.startStroke(Point(0.1f, 0.5f))
        stroke.addPoint(Point(0.5f, 0.5f))

        val restarted = stroke.restart()
        restarted.addPoint(Point(0.9f, 0.5f))
        testDrawScope { with(restarted) { render() } }

        // Continues from the interrupted stroke's last point, not its first.
        assertEquals(listOf(Point(0.5f, 0.5f), Point(0.9f, 0.5f)), brush.renderCalls.single().first)
    }

    // @spec CANVAS-STYLE-011, CANVAS-STYLE-012
    @Test
    fun restartReusesTheInterruptedStrokesColorWithoutQueryingTheColorSourceAgain() {
        val colorSource = FakeColorSource(listOf(Color.Red, Color.Blue))
        val brush = RecordingSimpleBrush(colorSource)
        val stroke = brush.startStroke(Point(0.1f, 0.5f))
        stroke.addPoint(Point(0.5f, 0.5f))

        val restarted = stroke.restart()
        restarted.addPoint(Point(0.9f, 0.5f))
        testDrawScope { with(restarted) { render() } }

        // If restart() had queried the ColorSource again, this would be Color.Blue.
        assertEquals(Color.Red, brush.renderCalls.single().second)
    }

    // @spec CANVAS-STYLE-016
    @Test
    fun saveExposesPointsAndColorByName() {
        val brush = RecordingSimpleBrush(ConstantColor(Color.Blue))
        val stroke = brush.startStroke(Point(0.2f, 0.4f))

        val saved = stroke.save()

        assertTrue(saved.containsKey("color"))
        assertTrue(saved.containsKey("points"))
    }

    // @spec CANVAS-STYLE-016, CANVAS-STYLE-017
    @Test
    fun restoringASavedStrokeReproducesTheSamePointsAndColor() {
        val brush = RecordingSimpleBrush(ConstantColor(Color.Red))
        val original = brush.startStroke(Point(0.1f, 0.5f))
        original.addPoint(Point(0.9f, 0.5f))

        val restored = brush.restore(original.save())
        testDrawScope { with(restored) { render() } }

        assertEquals(
            listOf(Point(0.1f, 0.5f), Point(0.9f, 0.5f)) to Color.Red,
            brush.renderCalls.single(),
        )
    }

    // @spec CANVAS-STYLE-017
    @Test
    fun restoreDoesNotQueryTheColorSourceAgain() {
        val colorSource = FakeColorSource(listOf(Color.Red, Color.Blue))
        val brush = RecordingSimpleBrush(colorSource)
        val original = brush.startStroke(Point(0.1f, 0.5f))
        val saved = original.save()

        // If restore() queried the ColorSource again, this would consume Color.Blue.
        val restored = brush.restore(saved)
        testDrawScope { with(restored) { render() } }

        assertEquals(Color.Red, brush.renderCalls.single().second)
    }
}
