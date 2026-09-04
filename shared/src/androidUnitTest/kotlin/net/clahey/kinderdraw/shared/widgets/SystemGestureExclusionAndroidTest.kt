package net.clahey.kinderdraw.shared.widgets

import androidx.compose.ui.geometry.Rect
import kotlin.test.Test
import kotlin.test.assertEquals
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class SystemGestureExclusionAndroidTest {
    // @spec CANVAS-WIDGETS-017
    @Test
    fun toAndroidRectRoundsEachEdgeToTheNearestPixel() {
        val rect = Rect(left = 1.4f, top = 2.6f, right = 10.5f, bottom = 20.5f)

        val androidRect = rect.toAndroidRect()

        assertEquals(android.graphics.Rect(1, 3, 11, 21), androidRect)
    }
}
