package net.clahey.kinderdraw.shared.userexperience

import android.content.Context
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

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
internal fun Context.isReduceMotionRequested(): Boolean =
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
    val reduceMotion = remember(context) { mutableStateOf(context.isReduceMotionRequested()) }
    DisposableEffect(context) {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                reduceMotion.value = context.isReduceMotionRequested()
            }
        }
        context.contentResolver.registerContentObserver(
            Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE),
            false,
            observer,
        )
        // Registration lands after the read above, so a change arriving in
        // between would otherwise go unnoticed until the one after it.
        reduceMotion.value = context.isReduceMotionRequested()
        onDispose { context.contentResolver.unregisterContentObserver(observer) }
    }
    return reduceMotion.value
}
