package net.clahey.kinderdraw.shared.imagestorage

import androidx.compose.ui.graphics.ImageBitmap
import kotlinx.coroutines.flow.MutableStateFlow

/** Records every image passed to [create]/[update], instead of persisting anything. */
class FakeImageStorage : ImageStorage {
    val createCalls = mutableListOf<ImageBitmap>()
    val updateCalls = mutableListOf<Pair<String, ImageBitmap>>()
    private var createFailuresRemaining = 0
    private var createFailureMessage = ""
    private var nextUpdateResult: Result<SavedDrawingEntry>? = null

    override val entries = MutableStateFlow<List<SavedDrawingEntry>>(emptyList())

    override suspend fun create(image: ImageBitmap, timestamp: Long?): Result<SavedDrawingEntry> {
        createCalls.add(image)
        return if (createFailuresRemaining > 0) {
            createFailuresRemaining--
            Result.failure(ImageStorageException(createFailureMessage))
        } else {
            Result.success(SavedDrawingEntry("id", 0L))
        }
    }

    override suspend fun update(id: String, image: ImageBitmap, timestamp: Long?): Result<SavedDrawingEntry> {
        updateCalls.add(id to image)
        return nextUpdateResult ?: Result.success(SavedDrawingEntry(id, 0L))
    }

    override suspend fun read(id: String): Result<ImageBitmap> =
        Result.failure(ImageStorageException("FakeImageStorage.read is unused"))

    override suspend fun delete(id: String): Result<Unit> =
        Result.failure(ImageStorageException("FakeImageStorage.delete is unused"))

    /** Fails the next [count] calls to [create], then succeeds as usual. */
    fun failNextCreates(count: Int, message: String) {
        createFailuresRemaining = count
        createFailureMessage = message
    }

    fun failNextCreate(message: String) = failNextCreates(count = 1, message = message)

    fun failNextUpdate(message: String) {
        nextUpdateResult = Result.failure(ImageStorageException(message))
    }
}
