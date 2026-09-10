package net.clahey.kinderdraw.shared.userexperience

/**
 * Whether the lock is held, leaving it as it was found: a free lock is taken
 * and immediately released, and a held one refuses, so neither outcome
 * disturbs the holder the assertion is about.
 */
internal fun InteractionLock.isHeld(): Boolean {
    val hold = tryAcquire() ?: return true
    hold.release()
    return false
}
