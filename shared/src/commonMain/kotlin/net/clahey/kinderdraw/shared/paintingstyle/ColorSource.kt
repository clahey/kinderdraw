package net.clahey.kinderdraw.shared.paintingstyle

import androidx.compose.ui.graphics.Color

/**
 * A pluggable color-resolution strategy, queried whenever a caller needs a
 * color — see the Painting Style LLD's Color Sources section.
 */
interface ColorSource {
    fun getNextColor(): Color
}
