package net.clahey.kinderdraw.shared.painting

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The edge geometry behind System Gesture Coexistence, at density 1 so an
 * inset's dp value is its pixel value. The surface is deliberately not square,
 * so an edge read off the wrong axis fails rather than coincidentally passing.
 */
class ReservedEdgesTest {
    private val density = Density(1f)
    private val size = IntSize(width = 100, height = 200)

    /**
     * One of the drawing surface's four edges: how to reserve [band] along it,
     * a point that lies within that band, and a point just past it.
     */
    private data class Edge(
        val name: String,
        val insets: WindowInsets,
        val inside: Offset,
        val outside: Offset,
    )

    private val band = 20.dp

    private val edges = listOf(
        Edge("left", WindowInsets(left = band), Offset(19f, 100f), Offset(20f, 100f)),
        Edge("top", WindowInsets(top = band), Offset(50f, 19f), Offset(50f, 20f)),
        Edge("right", WindowInsets(right = band), Offset(80f, 100f), Offset(79f, 100f)),
        Edge("bottom", WindowInsets(bottom = band), Offset(50f, 180f), Offset(50f, 179f)),
    )

    private fun WindowInsets.reserves(point: Offset) =
        reserves(point, size, density, LayoutDirection.Ltr)

    // @spec CANVAS-PAINT-030
    @Test
    fun reservesTheBandAlongEachEdgeItIsGivenFor() {
        for (edge in edges) {
            assertTrue(
                edge.insets.reserves(edge.inside),
                "${edge.name}: ${edge.inside} should fall inside a reserved ${edge.name} edge",
            )
            assertFalse(
                edge.insets.reserves(edge.outside),
                "${edge.name}: ${edge.outside} is past the band and should not",
            )
        }
    }

    // @spec CANVAS-PAINT-030
    @Test
    fun reservesNothingAlongTheEdgesItIsNotGivenFor() {
        for (edge in edges) {
            for (other in edges - edge) {
                assertFalse(
                    edge.insets.reserves(other.inside),
                    "a reserved ${edge.name} edge must not reserve ${other.name}'s band at ${other.inside}",
                )
            }
        }
    }

    // @spec CANVAS-PAINT-031
    @Test
    fun reservesNoEdgeThatIsZeroEvenAtItsOutermostPixel() {
        val nothing = WindowInsets(0.dp, 0.dp, 0.dp, 0.dp)

        // The extremes a comparison without a zero check would wrongly match.
        val extremes = listOf(
            Offset(0f, 100f) to "left column",
            Offset(50f, 0f) to "top row",
            Offset(99f, 100f) to "right column",
            Offset(50f, 199f) to "bottom row",
        )
        for ((point, name) in extremes) {
            assertFalse(nothing.reserves(point), "$name must be open when nothing is reserved")
        }
    }

    // @spec CANVAS-PAINT-030
    @Test
    fun reservesEveryEdgeGivenAtOnce() {
        val all = WindowInsets(left = band, top = band, right = band, bottom = band)

        for (edge in edges) {
            assertTrue(all.reserves(edge.inside), "${edge.name} is reserved here too")
        }
        assertFalse(all.reserves(Offset(50f, 100f)), "the interior is reserved by nothing")
    }

    // @spec CANVAS-PAINT-030
    @Test
    fun readsEachEdgeOffItsOwnAxis() {
        // A 20dp band on a 100x200 surface: swapping width for height would put
        // the bottom edge at y=80 instead of y=180.
        val bottom = WindowInsets(bottom = band)

        assertFalse(bottom.reserves(Offset(50f, 80f)), "y=80 is interior on a 200-tall surface")
        assertTrue(bottom.reserves(Offset(50f, 180f)))

        val right = WindowInsets(right = band)

        assertFalse(right.reserves(Offset(79f, 100f)), "x=79 is interior on a 100-wide surface")
        assertTrue(right.reserves(Offset(85f, 100f)))
    }

    // @spec CANVAS-PAINT-030
    @Test
    fun coversAnEdgeUpToButNotPastItsInset() {
        val top = WindowInsets(top = band)

        // The band is the rows strictly above the inset, so the inset's own row
        // is the first that draws.
        assertTrue(top.reserves(Offset(50f, 19f)))
        assertFalse(top.reserves(Offset(50f, 20f)))
        assertEquals(20, WindowInsets(top = band).getTop(density), "the band really is 20px here")
    }
}
