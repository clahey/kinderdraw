package net.clahey.kinderdraw.shared.userexperience

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class InteractionLockTest {
    // @spec CANVAS-UX-020
    @Test
    fun grantsTheInteractionWhileItIsFree() {
        val lock = InteractionLock()

        assertNotNull(lock.tryAcquire())
    }

    // Two pointers arriving in one input event can't be constructed from a
    // test, so CANVAS-UX-026's exactly-one-wins half is verified here rather
    // than through the screen.
    // @spec CANVAS-UX-004, CANVAS-UX-020, CANVAS-UX-026
    @Test
    fun refusesEveryRequestMadeWhileTheInteractionIsHeld() {
        val lock = InteractionLock()
        assertNotNull(lock.tryAcquire())

        assertNull(lock.tryAcquire())
        assertNull(lock.tryAcquire())
    }

    // @spec CANVAS-UX-020
    @Test
    fun grantsTheInteractionAgainOnceTheHolderReleasesIt() {
        val lock = InteractionLock()
        val hold = assertNotNull(lock.tryAcquire())

        hold.release()

        assertNotNull(lock.tryAcquire())
    }

    // @spec CANVAS-UX-022
    @Test
    fun releasingTheSameHoldTwiceDoesNothingTheSecondTime() {
        val lock = InteractionLock()
        val hold = assertNotNull(lock.tryAcquire())
        hold.release()
        val next = assertNotNull(lock.tryAcquire())

        // The second release lands while someone else already holds it.
        hold.release()

        assertTrue(lock.isHeld())
        next.release()
        assertFalse(lock.isHeld())
    }

    // @spec CANVAS-UX-022
    @Test
    fun aStaleHoldCannotFreeALaterHoldersInteraction() {
        val lock = InteractionLock()
        val stale = assertNotNull(lock.tryAcquire())
        stale.release()
        val later = assertNotNull(lock.tryAcquire())

        stale.release()

        assertTrue(lock.isHeld())
        // Still held by `later` specifically — its own release is what frees it.
        later.release()
        assertFalse(lock.isHeld())
    }
}
