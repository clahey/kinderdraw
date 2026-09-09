package net.clahey.kinderdraw.shared.userexperience

import androidx.compose.runtime.Stable
import androidx.compose.ui.input.pointer.AwaitPointerEventScope

/**
 * The kid canvas's arbiter — see the User Experience LLD's Input Arbitration.
 * At most one component holds it at a time; a component asks for a [Hold] when
 * its own gesture begins, and one that's refused starts nothing and swallows
 * the rest of that gesture ([swallowGesture]).
 *
 * Release goes through the [Hold] that acquiring returned, never through the
 * lock itself, so a component can only end a hold it is actually holding.
 *
 * Confined to the UI dispatcher, which is where Compose delivers pointer
 * events and where every holder's release resumes, so it carries no
 * synchronization of its own.
 */
// @spec CANVAS-UX-020, CANVAS-UX-021, CANVAS-UX-023
@Stable
class InteractionLock {
    private var holder: Hold? = null

    /** The hold, or null if another component already holds this lock. */
    fun tryAcquire(): Hold? = if (holder != null) null else Hold(this).also { holder = it }

    // @spec CANVAS-UX-022
    private fun releaseIfHolder(hold: Hold) {
        if (holder === hold) holder = null
    }

    /** One grant of the lock, held until [release]. */
    class Hold internal constructor(private val lock: InteractionLock) {
        /**
         * Ends this hold. Releasing an already-released hold does nothing —
         * in particular it never frees a later holder's.
         */
        fun release() = lock.releaseIfHolder(this)
    }
}

/**
 * Consumes events until no pointer is pressed anywhere on this component —
 * what a component owes after the lock refuses it (see the User Experience
 * LLD's Input Arbitration). The refusal covers the whole gesture, so a
 * pointer resting on the component extends the swallow for as long as it
 * stays down, whether or not it belongs to the refused press.
 */
// @spec CANVAS-UX-005
suspend fun AwaitPointerEventScope.swallowGesture() {
    do {
        val event = awaitPointerEvent()
        event.changes.forEach { it.consume() }
    } while (event.changes.any { it.pressed })
}
