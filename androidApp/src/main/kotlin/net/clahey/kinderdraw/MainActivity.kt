package net.clahey.kinderdraw

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import net.clahey.kinderdraw.shared.userexperience.KidCanvasScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            KidCanvasScreen()
        }
    }
}
