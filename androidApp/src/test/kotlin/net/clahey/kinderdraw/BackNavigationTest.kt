package net.clahey.kinderdraw

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Back never takes the toddler out of the kid canvas — see the User Experience
 * LLD's OS Navigation and Process Lifecycle.
 *
 * The handler itself is shared code inside `KidCanvasScreen`; these run against
 * the Android shell because what's worth proving is that the platform's own
 * back dispatch reaches it, which only a real activity can show.
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
