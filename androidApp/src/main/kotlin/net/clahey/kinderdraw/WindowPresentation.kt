package net.clahey.kinderdraw

import android.view.Window
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager

/**
 * Hides the system bars and extends the window into the display cutout. See
 * the User Experience LLD's Screen Composition.
 */
// @spec CANVAS-UX-052
internal fun Window.presentWithoutChrome() {
    // Only takes effect on a fullscreen window; hiding the bars below is what
    // makes it one.
    attributes = attributes.apply {
        layoutInDisplayCutoutMode =
            WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
    }
    // Before attach this is a PendingInsetsController, which replays these
    // calls once the real controller exists.
    decorView.windowInsetsController?.presentWithoutChrome()
}

/** Hides the system bars, leaving them revealable only transiently. */
// @spec CANVAS-UX-052, CANVAS-UX-054
internal fun WindowInsetsController.presentWithoutChrome() {
    hide(WindowInsets.Type.systemBars())
    // A revealed bar overlays the canvas and re-hides itself, so nothing resizes.
    systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
}
