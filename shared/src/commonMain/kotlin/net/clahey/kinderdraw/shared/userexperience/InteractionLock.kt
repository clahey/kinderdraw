package net.clahey.kinderdraw.shared.userexperience

import androidx.compose.runtime.Stable
import androidx.compose.ui.input.pointer.AwaitPointerEventScope

/**
 * The kid canvas's arbiter — see the User Experience LLD's Input Arbitration.
 * At most one component holds the interaction at a time; a component asks for
 * it when its own gesture begins, and one that's refused starts nothing and
 * swallows the rest of that gesture ([swallowGesture]).
 *
 * There is deliberately no `release` here: acquiring returns the [Hold] that
 * ends it, so a component can't release an interaction it never acquired.
 *
 * Confined to the UI dispatcher, which is where Compose delivers pointer
 * events and where every holder's release resumes, so it carries no
 * synchronization of its own.
 */
// @spec CANVAS-UX-020, CANVAS-UX-021, CANVAS-UX-023
@Stable
class InteractionLock {
    private var holder: Hold? = null

    /** The hold, or null if another component already has the interaction. */
    fun tryAcquire(): Hold? = if (holder != null) null else Hold(this).also { holder = it }

    // @spec CANVAS-UX-022
    private fun releaseIfHolder(hold: Hold) {
        if (holder === hold) holder = null
    }

    /** One granted interaction, held until [release]. */
    class Hold internal constructor(private val lock: InteractionLock) {
        /**
         * Ends this hold. Releasing an already-released hold does nothing —
         * in particular it never frees a later holder's interaction.
         */
        fun release() = lock.releaseIfHolder(this)
    }
}

/**
 * Consumes the rest of the current gesture, returning once every pointer
 * this component is tracking has lifted — what a component owes after the
 * lock refuses it (see the User Experience LLD's Input Arbitration).
 */
// @spec CANVAS-UX-005
suspend fun AwaitPointerEventScope.swallowGesture() {
    do {
        val event = awaitPointerEvent()
        event.changes.forEach { it.consume() }
    } while (event.changes.any { it.pressed })
}
