package net.clahey.kinderdraw

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import net.clahey.kinderdraw.shared.userexperience.KidCanvasScreen
import net.clahey.kinderdraw.shared.userexperience.seedFrom

/**
 * Fixes the colors the canvas draws, so a given seed and a given sequence of
 * actions reproduce the same drawing — see the User Experience LLD's Seeding
 * the Sampled Colors. Honored in every build type, since it reaches no
 * consumer-facing surface and anyone who can send it can already launch the app.
 */
const val EXTRA_RANDOM_SEED: String = "net.clahey.kinderdraw.extra.RANDOM_SEED"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            KidCanvasScreen(seed = intent.randomSeed())
        }
    }
}

/**
 * The seed the launch intent asks for, whatever type it was sent as — the
 * typed getters can't answer that, so this reads the raw value and lets
 * [seedFrom] decide what it means.
 */
// @spec CANVAS-UX-045, CANVAS-UX-046
@Suppress("DEPRECATION")
private fun Intent.randomSeed(): Long? = seedFrom(extras?.get(EXTRA_RANDOM_SEED))
