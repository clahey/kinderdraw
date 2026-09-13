package net.clahey.kinderdraw

import androidx.compose.ui.graphics.ImageBitmap
import net.clahey.kinderdraw.shared.imagestorage.ImageStorage
import net.clahey.kinderdraw.shared.imagestorage.SavedDrawingEntry

/**
 * Fails every write, so New Picture's failed-save feedback can be watched on a
 * real device. Nothing else can produce that path by hand: MediaStore mediates
 * the write, so a read-only directory doesn't map onto a create failure, and
 * short of genuinely filling storage there is no way to make one fail.
 *
 * Wired in only when `BuildConfig.FAIL_SAVES` is set, which is a debug-build
 * property — see [MainActivity] and `androidApp/build.gradle.kts`.
 */
class FailingImageStorage(private val delegate: ImageStorage) : ImageStorage by delegate {
    override suspend fun create(image: ImageBitmap, timestamp: Long?): Result<SavedDrawingEntry> =
        Result.failure(IllegalStateException("saves are failing on purpose (-PkinderdrawFailSaves)"))

    override suspend fun update(id: String, image: ImageBitmap, timestamp: Long?): Result<SavedDrawingEntry> =
        Result.failure(IllegalStateException("saves are failing on purpose (-PkinderdrawFailSaves)"))
}
