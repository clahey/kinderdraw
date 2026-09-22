package net.clahey.kinderdraw

import android.view.Window
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager

/**
 * Presents this window with nothing of the platform's own around the kid
 * canvas — see the User Experience LLD's Screen Composition. Removing the
 * action bar is the theme's job (see `res/values/themes.xml`); this covers
 * what no theme attribute can express.
 *
 * Safe to apply repeatedly, which is what keeps the presentation a standing
 * condition rather than a launch-time act.
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
    // A window installs its decor lazily and serves the insets controller from
    // it, so asking for the decor first is what lets this run from onCreate
    // and not only once the window is already up.
    decorView.windowInsetsController?.presentWithoutChrome()
}

/**
 * Hides the system bars and asks for them back only on loan.
 */
// @spec CANVAS-UX-052, CANVAS-UX-054
internal fun WindowInsetsController.presentWithoutChrome() {
    hide(WindowInsets.Type.systemBars())
    // Transient rather than swipe-to-restore: a revealed bar is overlaid and
    // leaves on its own, so the drawing surface never changes size under a
    // hand already moving on it, and no swipe can strand it visible.
    systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
}
