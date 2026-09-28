package net.clahey.kinderdraw

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Back never takes the toddler out of the kid canvas. See the User Experience
 * LLD's OS Navigation and Process Lifecycle.
 *
 * The handler is shared code inside `KidCanvasScreen`, but only a real activity
 * shows that Android's back dispatch reaches it.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class BackNavigationTest {

    // @spec CANVAS-UX-015
    @Test
    fun claimsBackFromThePlatform() {
        val activity = Robolectric.buildActivity(MainActivity::class.java).setup().get()

        assertTrue(activity.onBackPressedDispatcher.hasEnabledCallbacks())
    }

    // @spec CANVAS-UX-015
    @Test
    fun staysOnTheCanvasWhenBackFires() {
        val activity = Robolectric.buildActivity(MainActivity::class.java).setup().get()

        activity.onBackPressedDispatcher.onBackPressed()

        assertFalse(activity.isFinishing)
    }
}
