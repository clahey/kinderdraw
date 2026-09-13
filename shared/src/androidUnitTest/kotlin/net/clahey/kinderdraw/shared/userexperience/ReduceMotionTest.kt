package net.clahey.kinderdraw.shared.userexperience

import android.content.Context
import android.provider.Settings
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.test.core.app.ApplicationProvider
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class ReduceMotionTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    private fun setAnimatorScale(scale: Float) {
        Settings.Global.putFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            scale,
        )
    }

    // @spec CANVAS-UX-039
    @Test
    fun reportsReducedMotionWhenAnimationsAreTurnedOff() {
        setAnimatorScale(0f)

        assertTrue(context.isReduceMotionRequested())
    }

    // @spec CANVAS-UX-039
    @Test
    fun reportsOrdinaryMotionAtTheDefaultScale() {
        setAnimatorScale(1f)

        assertFalse(context.isReduceMotionRequested())
    }

    // @spec CANVAS-UX-039
    @Test
    fun aSlowedScaleIsNotAReducedMotionRequest() {
        // Only zero means "play none". A scale someone stretched or shortened
        // for their own reasons is still a request for animation.
        setAnimatorScale(10f)
        assertFalse(context.isReduceMotionRequested())

        setAnimatorScale(0.5f)
        assertFalse(context.isReduceMotionRequested())
    }

    // @spec CANVAS-UX-039
    @Test
    fun reachesAComposedScreenWhenTheSettingChanges() = runComposeUiTest {
        setAnimatorScale(0f)
        var observed = false
        var atFirstComposition: Boolean? = null

        setContent {
            observed = rememberReduceMotion()
            // Captured from inside the composition, where the effect that
            // registers the observer has not run yet and so cannot have
            // corrected it.
            if (atFirstComposition == null) atFirstComposition = observed
        }
        assertEquals(true, atFirstComposition)

        setAnimatorScale(1f)
        waitForIdle()
        assertFalse(observed)

        // Again, because an observer that fired once and stopped would satisfy
        // everything above.
        setAnimatorScale(0f)
        waitForIdle()
        assertTrue(observed)
    }

    // @spec CANVAS-UX-039
    @Test
    fun anUnsetScaleReadsAsOrdinaryMotion() {
        Settings.Global.putString(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            null,
        )

        assertFalse(context.isReduceMotionRequested(), "absent means nothing was asked for")
    }
}
