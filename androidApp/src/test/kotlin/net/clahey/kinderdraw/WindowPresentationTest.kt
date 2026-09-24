package net.clahey.kinderdraw

import android.os.CancellationSignal
import android.view.WindowInsets
import android.view.WindowInsetsAnimationControlListener
import android.view.WindowInsetsController
import android.view.WindowManager
import android.view.animation.Interpolator
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The window the kid canvas is presented in. See the User Experience LLD's
 * Screen Composition.
 *
 * Hiding is asserted against [RecordingInsetsController] because Robolectric's
 * `InsetsController` isn't attached to a display, so `hide()` leaves nothing to
 * read back. `systemBarsBehavior` and `layoutInDisplayCutoutMode` do read back,
 * so the activity's own window covers the rest.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class WindowPresentationTest {

    // @spec CANVAS-UX-051
    @Test
    fun presentsTheCanvasWithNoActionBar() {
        val activity = Robolectric.buildActivity(MainActivity::class.java).setup().get()

        assertNull(activity.actionBar)
    }

    // @spec CANVAS-UX-052
    @Test
    fun extendsTheDrawingSurfaceIntoTheDisplayCutout() {
        val activity = Robolectric.buildActivity(MainActivity::class.java).setup().get()

        assertEquals(
            WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS,
            activity.window.attributes.layoutInDisplayCutoutMode,
        )
    }

    // @spec CANVAS-UX-052
    @Test
    fun hidesTheStatusAndNavigationBars() {
        val controller = RecordingInsetsController()

        controller.presentWithoutChrome()

        assertTrue(controller.hidden and WindowInsets.Type.systemBars() != 0)
    }

    // @spec CANVAS-UX-054
    @Test
    fun asksForTheBarsToReturnOnlyTransiently() {
        val controller = RecordingInsetsController()

        controller.presentWithoutChrome()

        assertEquals(
            WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE,
            controller.systemBarsBehavior,
        )
    }

    // @spec CANVAS-UX-053
    @Test
    fun presentsTheCanvasAgainWhenTheWindowRegainsFocus() {
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
        val window = controller.get().window
        // Also the only check that launching applied it at all.
        assertEquals(
            WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE,
            window.insetsController!!.systemBarsBehavior,
        )
        // Stand in for whatever the OS restored while the window was away.
        window.attributes = window.attributes.apply {
            layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_DEFAULT
        }
        window.insetsController!!.systemBarsBehavior = WindowInsetsController.BEHAVIOR_DEFAULT

        controller.windowFocusChanged(true)

        assertEquals(
            WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS,
            window.attributes.layoutInDisplayCutoutMode,
        )
        assertEquals(
            WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE,
            window.insetsController!!.systemBarsBehavior,
        )
    }
}

/** Records what [presentWithoutChrome] asks of a controller. */
private class RecordingInsetsController : WindowInsetsController {
    var hidden: Int = 0
        private set
    private var behavior: Int = WindowInsetsController.BEHAVIOR_DEFAULT

    override fun hide(types: Int) {
        hidden = hidden or types
    }

    override fun show(types: Int) {
        hidden = hidden and types.inv()
    }

    override fun setSystemBarsBehavior(behavior: Int) {
        this.behavior = behavior
    }

    override fun getSystemBarsBehavior(): Int = behavior

    override fun setSystemBarsAppearance(appearance: Int, mask: Int) = Unit

    override fun getSystemBarsAppearance(): Int = 0

    override fun controlWindowInsetsAnimation(
        types: Int,
        durationMillis: Long,
        interpolator: Interpolator?,
        cancellationSignal: CancellationSignal?,
        listener: WindowInsetsAnimationControlListener,
    ) = Unit

    override fun addOnControllableInsetsChangedListener(
        listener: WindowInsetsController.OnControllableInsetsChangedListener,
    ) = Unit

    override fun removeOnControllableInsetsChangedListener(
        listener: WindowInsetsController.OnControllableInsetsChangedListener,
    ) = Unit
}
