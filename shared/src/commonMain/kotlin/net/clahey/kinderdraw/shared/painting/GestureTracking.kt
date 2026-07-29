package net.clahey.kinderdraw.shared.painting

/** Whether one input event's net pointer changes started, ended, or left unchanged the current gesture. */
internal enum class GestureEdge { STARTED, ENDED, UNCHANGED }

/**
 * Applies one input event's net pointer changes to [trackedPointers] and
 * reports whether the gesture as a whole started or ended — decided once per
 * event, from the net effect of every change in it, not per individual
 * change within it (see the Painting LLD's Composable Shape).
 */
// @spec CANVAS-PAINT-018
internal fun <T> applyGestureChanges(trackedPointers: MutableSet<T>, down: List<T>, up: List<T>): GestureEdge {
    val wasLive = trackedPointers.isNotEmpty()
    trackedPointers += down
    trackedPointers -= up
    val isLive = trackedPointers.isNotEmpty()
    return when {
        !wasLive && isLive -> GestureEdge.STARTED
        wasLive && !isLive -> GestureEdge.ENDED
        else -> GestureEdge.UNCHANGED
    }
}
