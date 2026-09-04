package net.clahey.kinderdraw.shared.widgets

import androidx.compose.ui.geometry.Rect
import kotlin.test.Test
import kotlin.test.assertEquals

class GestureExclusionRegistryTest {
    private val rectA = Rect(0f, 0f, 10f, 10f)
    private val rectB = Rect(20f, 20f, 30f, 30f)

    // @spec CANVAS-WIDGETS-017
    @Test
    fun setWithOneKeyReturnsJustThatRect() {
        val registry = GestureExclusionRegistry()

        val result = registry.set("a", rectA)

        assertEquals(listOf(rectA), result)
    }

    // @spec CANVAS-WIDGETS-017
    @Test
    fun setWithTwoDifferentKeysReturnsBothRectsWithoutClobberingEitherOne() {
        val registry = GestureExclusionRegistry()

        registry.set("a", rectA)
        val result = registry.set("b", rectB)

        assertEquals(setOf(rectA, rectB), result.toSet())
    }

    // @spec CANVAS-WIDGETS-017
    @Test
    fun setTwiceWithTheSameKeyUpdatesRatherThanDuplicating() {
        val registry = GestureExclusionRegistry()
        val movedRectA = Rect(1f, 1f, 11f, 11f)

        registry.set("a", rectA)
        val result = registry.set("a", movedRectA)

        assertEquals(listOf(movedRectA), result)
    }

    // @spec CANVAS-WIDGETS-017
    @Test
    fun removeDropsOnlyThatKeysRectLeavingOthersRegistered() {
        val registry = GestureExclusionRegistry()
        registry.set("a", rectA)
        registry.set("b", rectB)

        val result = registry.remove("a")

        assertEquals(listOf(rectB), result)
    }

    // @spec CANVAS-WIDGETS-017
    @Test
    fun removingAnUnregisteredKeyIsANoOp() {
        val registry = GestureExclusionRegistry()
        registry.set("a", rectA)

        val result = registry.remove("never-registered")

        assertEquals(listOf(rectA), result)
    }
}
