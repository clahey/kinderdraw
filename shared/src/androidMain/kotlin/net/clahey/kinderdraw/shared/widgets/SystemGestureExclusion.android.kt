package net.clahey.kinderdraw.shared.widgets

import android.graphics.Rect as AndroidRect
import android.view.View
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.GlobalPositionAwareModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.LocalView
import java.util.WeakHashMap
import kotlin.math.roundToInt

/**
 * One [GestureExclusionRegistry] per hosting [View] — `setSystemGestureExclusionRects`
 * takes one list for the whole View, so every control sharing that View
 * shares a registry rather than overwriting each other's rects.
 *
 * Reached only from [GestureExclusionNode]'s positioning and detach callbacks, which
 * Compose delivers on the UI thread, so this unsynchronized map has no concurrent
 * writers.
 */
private val registriesByView = WeakHashMap<View, GestureExclusionRegistry<GestureExclusionNode>>()

private fun registryFor(view: View): GestureExclusionRegistry<GestureExclusionNode> =
    registriesByView.getOrPut(view) { GestureExclusionRegistry() }

internal fun Rect.toAndroidRect(): AndroidRect =
    AndroidRect(left.roundToInt(), top.roundToInt(), right.roundToInt(), bottom.roundToInt())

/**
 * Keeps this control's own entry in its View's registry equal to its current
 * bounds, and drops the entry when the control leaves the tree — without which
 * a control removed by feature gating would leave a rect suppressing the
 * system's gesture over empty screen.
 *
 * The node is its own registry key: one node exists per control per composition,
 * so its identity already distinguishes controls and no separate handle is needed.
 */
private class GestureExclusionNode :
    Modifier.Node(),
    GlobalPositionAwareModifierNode,
    CompositionLocalConsumerModifierNode {

    /** Held from the last positioning, since [onDetach] can no longer read a composition local. */
    private var view: View? = null

    override fun onGloballyPositioned(coordinates: LayoutCoordinates) {
        val host = currentValueOf(LocalView)
        view = host
        host.systemGestureExclusionRects =
            registryFor(host).set(this, coordinates.boundsInRoot()).map { it.toAndroidRect() }
    }

    override fun onDetach() {
        // Null when the control was never positioned, in which case it registered nothing.
        val host = view ?: return
        host.systemGestureExclusionRects = registryFor(host).remove(this).map { it.toAndroidRect() }
        view = null
    }
}

/**
 * Carries no parameters, so one instance serves every use and an update is never needed.
 *
 * [ModifierNodeElement] declares [equals] and [hashCode] abstract — it compares elements
 * across recomposition to choose between [create] and [update] — so a singleton has to
 * spell out the identity comparison it would otherwise inherit, and can't reach
 * `super.hashCode()` to do it.
 */
private object GestureExclusionElement : ModifierNodeElement<GestureExclusionNode>() {
    override fun create(): GestureExclusionNode = GestureExclusionNode()

    override fun update(node: GestureExclusionNode) = Unit

    override fun InspectorInfo.inspectableProperties() {
        name = "excludeFromSystemGestures"
    }

    override fun equals(other: Any?): Boolean = other === this

    override fun hashCode(): Int = System.identityHashCode(this)
}

// @spec CANVAS-WIDGETS-017
internal actual fun Modifier.excludeFromSystemGestures(): Modifier = this then GestureExclusionElement
