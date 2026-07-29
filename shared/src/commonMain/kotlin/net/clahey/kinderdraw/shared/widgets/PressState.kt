package net.clahey.kinderdraw.shared.widgets

/**
 * The claim/activation state machine shared by every KidWidgets control —
 * see the Widgets LLD's Hit-Testing and Activation. A caller drives this
 * from raw pointer events (`onClaim` at down, `onPositionChanged` on every
 * subsequent move including the position at release, `onRelease` at up);
 * this class holds no pointer/geometry types itself, only the timestamps
 * needed to decide whether a release counts as an activation.
 */
class PressState(
    private val onPressedChange: (Boolean) -> Unit,
    private val onActivate: () -> Unit,
    private val strayToleranceMs: Long = 100,
) {
    private var enteredAt: Long = 0
    private var exitedAt: Long? = null

    // @spec CANVAS-WIDGETS-001, CANVAS-WIDGETS-004, CANVAS-WIDGETS-010
    fun onClaim(now: Long) {
        enteredAt = now
        exitedAt = null
        onPressedChange(true)
    }

    fun onPositionChanged(insideRegion: Boolean, now: Long) {
        if (insideRegion) {
            if (exitedAt != null) {
                enteredAt = now
                exitedAt = null
            }
        } else {
            if (exitedAt == null) {
                exitedAt = now
            }
        }
    }

    // @spec CANVAS-WIDGETS-005, CANVAS-WIDGETS-006, CANVAS-WIDGETS-007, CANVAS-WIDGETS-011, CANVAS-WIDGETS-012, CANVAS-WIDGETS-013
    fun onRelease(now: Long) {
        val lastExitedAt = exitedAt
        val activates = if (lastExitedAt == null) {
            true
        } else {
            val strayMs = now - lastExitedAt
            val insideMs = lastExitedAt - enteredAt
            strayMs < strayToleranceMs && insideMs > strayMs
        }
        onPressedChange(false)
        if (activates) onActivate()
    }
}
