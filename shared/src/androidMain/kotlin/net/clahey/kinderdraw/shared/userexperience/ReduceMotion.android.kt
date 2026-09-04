package net.clahey.kinderdraw.shared.userexperience

import android.content.Context
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.awaitCancellation

/**
 * Whether the platform reports that the user has asked for reduced motion —
 * see the User Experience LLD's Putting the Drawing Away.
 *
 * Android has no flag of its own for this. Both the "Remove animations"
 * accessibility setting and the developer-options animator scale write
 * [Settings.Global.ANIMATOR_DURATION_SCALE], and zero there means play none.
 * Any other value, however stretched or shortened, is still a request for
 * animation rather than against it.
 */
// @spec CANVAS-UX-039
fun Context.isReduceMotionRequested(): Boolean =
    Settings.Global.getFloat(contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f

/**
 * [isReduceMotionRequested], re-read whenever the setting changes, so turning
 * animations off takes effect without restarting the app. Changing it does not
 * recreate the activity, so nothing else would notice.
 */
// @spec CANVAS-UX-039
@Composable
fun rememberReduceMotion(): Boolean {
    val context = LocalContext.current
    val reduceMotion by produceState(context.isReduceMotionRequested(), context) {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                value = context.isReduceMotionRequested()
            }
        }
        context.contentResolver.registerContentObserver(
            Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE),
            false,
            observer,
        )
        try {
            awaitCancellation()
        } finally {
            context.contentResolver.unregisterContentObserver(observer)
        }
    }
    return reduceMotion
}
