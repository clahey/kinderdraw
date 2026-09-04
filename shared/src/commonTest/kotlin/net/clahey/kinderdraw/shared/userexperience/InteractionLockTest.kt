package net.clahey.kinderdraw.shared.userexperience

import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class InteractionLockTest {
    // @spec CANVAS-UX-020
    @Test
    fun grantsTheInteractionWhileItIsFree() {
        val lock = InteractionLock()

        assertNotNull(lock.tryAcquire())
    }

    // @spec CANVAS-UX-004, CANVAS-UX-020
    @Test
    fun refusesEveryRequestMadeWhileTheInteractionIsHeld() {
        val lock = InteractionLock()
        lock.tryAcquire()

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

        assertNull(lock.tryAcquire())
        next.release()
        assertNotNull(lock.tryAcquire())
    }

    // @spec CANVAS-UX-022
    @Test
    fun aStaleHoldCannotFreeALaterHoldersInteraction() {
        val lock = InteractionLock()
        val stale = assertNotNull(lock.tryAcquire())
        stale.release()
        assertNotNull(lock.tryAcquire())

        stale.release()

        assertNull(lock.tryAcquire())
    }
}
