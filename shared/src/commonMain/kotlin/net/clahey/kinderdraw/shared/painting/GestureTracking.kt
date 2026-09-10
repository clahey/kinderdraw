package net.clahey.kinderdraw.shared.painting

/**
 * Applies one input event's net pointer changes to [trackedPointers] and
 * reports whether the gesture is still live afterwards — decided once per
 * event, from the net effect of every change in it, not per individual
 * change within it (see the Painting LLD's Composable Shape).
 */
// @spec CANVAS-PAINT-018
internal fun <T> applyGestureChanges(trackedPointers: MutableSet<T>, down: List<T>, up: List<T>): Boolean {
    trackedPointers += down
    trackedPointers -= up
    return trackedPointers.isNotEmpty()
}
