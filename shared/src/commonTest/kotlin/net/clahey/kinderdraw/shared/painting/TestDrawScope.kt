package net.clahey.kinderdraw.shared.painting

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection

/** A real [DrawScope] backed by an off-screen bitmap, for asserting on rendered pixels in tests. */
fun testDrawScope(width: Int = 16, height: Int = 16, block: DrawScope.() -> Unit): ImageBitmap {
    val bitmap = ImageBitmap(width, height)
    val canvas = Canvas(bitmap)
    CanvasDrawScope().draw(
        Density(1f),
        LayoutDirection.Ltr,
        canvas,
        Size(width.toFloat(), height.toFloat()),
        block,
    )
    return bitmap
}
