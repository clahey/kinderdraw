package net.clahey.kinderdraw.shared.imagestorage

import androidx.compose.ui.graphics.ImageBitmap
import kotlinx.coroutines.flow.Flow

/**
 * Cross-platform boundary for saved-drawing storage — see the Image Storage
 * LLD. Painting and the Companion App depend only on this interface; each
 * platform provides its own implementation.
 */
interface ImageStorage {
    /** The current saved-drawing list, then every subsequent change from any source. */
    val entries: Flow<List<SavedDrawingEntry>>

    /** Creates a new saved-drawing entry. [timestamp] is generated when omitted. */
    suspend fun create(image: ImageBitmap, timestamp: Long? = null): Result<SavedDrawingEntry>

    /** Replaces [id]'s raster image. Fails if [id] has no existing entry. [timestamp] is left unchanged when omitted. */
    suspend fun update(id: String, image: ImageBitmap, timestamp: Long? = null): Result<SavedDrawingEntry>

    suspend fun read(id: String): Result<ImageBitmap>

    suspend fun delete(id: String): Result<Unit>
}

/** A saved drawing's metadata, without its raster image — see [ImageStorage.entries]. */
data class SavedDrawingEntry(val id: String, val timestamp: Long)

/** Uniform failure type for [ImageStorage] operations, normalized from whatever a backend's platform surfaces. */
class ImageStorageException(message: String, cause: Throwable? = null) : Exception(message, cause)
