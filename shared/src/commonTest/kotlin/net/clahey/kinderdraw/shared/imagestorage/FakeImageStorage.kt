package net.clahey.kinderdraw.shared.imagestorage

import androidx.compose.ui.graphics.ImageBitmap
import kotlinx.coroutines.flow.MutableStateFlow

/** Records every image passed to [create]/[update], instead of persisting anything. */
class FakeImageStorage : ImageStorage {
    val createCalls = mutableListOf<ImageBitmap>()
    val updateCalls = mutableListOf<Pair<String, ImageBitmap>>()
    private var nextCreateResult: Result<SavedDrawingEntry> = Result.success(SavedDrawingEntry("id", 0L))
    private var nextUpdateResult: Result<SavedDrawingEntry>? = null

    override val entries = MutableStateFlow<List<SavedDrawingEntry>>(emptyList())

    override suspend fun create(image: ImageBitmap, timestamp: Long?): Result<SavedDrawingEntry> {
        createCalls.add(image)
        return nextCreateResult
    }

    override suspend fun update(id: String, image: ImageBitmap, timestamp: Long?): Result<SavedDrawingEntry> {
        updateCalls.add(id to image)
        return nextUpdateResult ?: Result.success(SavedDrawingEntry(id, 0L))
    }

    override suspend fun read(id: String): Result<ImageBitmap> =
        Result.failure(ImageStorageException("FakeImageStorage.read is unused"))

    override suspend fun delete(id: String): Result<Unit> =
        Result.failure(ImageStorageException("FakeImageStorage.delete is unused"))

    fun failNextCreate(message: String) {
        nextCreateResult = Result.failure(ImageStorageException(message))
    }

    fun failNextUpdate(message: String) {
        nextUpdateResult = Result.failure(ImageStorageException(message))
    }
}
