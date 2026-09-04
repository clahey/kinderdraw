package net.clahey.kinderdraw

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import net.clahey.kinderdraw.shared.imagestorage.MediaStoreImageStorage
import net.clahey.kinderdraw.shared.userexperience.KidCanvasScreen
import net.clahey.kinderdraw.shared.userexperience.rememberReduceMotion

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val imageStorage = MediaStoreImageStorage(applicationContext).let {
            // Constant-folded false in release builds — see FailingImageStorage.
            if (BuildConfig.FAIL_SAVES) FailingImageStorage(it) else it
        }
        setContent {
            KidCanvasScreen(
                imageStorage = imageStorage,
                // @spec CANVAS-UX-039
                reduceMotion = rememberReduceMotion(),
            )
        }
    }
}
