package net.clahey.kinderdraw.shared.userexperience

import android.content.Context
import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

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
    fun anUnsetScaleReadsAsOrdinaryMotion() {
        Settings.Global.putString(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            null,
        )

        assertFalse(context.isReduceMotionRequested(), "absent means nothing was asked for")
    }
}
