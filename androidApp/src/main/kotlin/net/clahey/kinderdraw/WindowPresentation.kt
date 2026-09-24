package net.clahey.kinderdraw

import android.view.Window
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager

/**
 * Hides the system bars and extends the window into the display cutout — see
 * the User Experience LLD's Screen Composition.
 */
// @spec CANVAS-UX-052
internal fun Window.presentWithoutChrome() {
    // A cutout is display the drawing surface should reach, not a region to
    // letterbox around. The platform honors this only for a fullscreen
    // window, which hiding the bars below is what makes this one.
    attributes = attributes.apply {
        layoutInDisplayCutoutMode =
            WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
    }
    // Before the window is attached this is a PendingInsetsController, which
    // buffers the request and replays it onto the real one at attach — so a
    // call from onCreate lands on the first frame rather than being dropped.
    decorView.windowInsetsController?.presentWithoutChrome()
}

/**
 * Hides the system bars and asks for them back only on loan.
 */
// @spec CANVAS-UX-052, CANVAS-UX-054
internal fun WindowInsetsController.presentWithoutChrome() {
    hide(WindowInsets.Type.systemBars())
    // Transient rather than swipe-to-restore: a revealed bar overlays the
    // screen and leaves on its own, so no swipe can strand one over the
    // drawing or resize the surface under a hand already moving on it.
    systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
}
