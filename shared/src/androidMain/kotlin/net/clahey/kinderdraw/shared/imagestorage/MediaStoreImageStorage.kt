package net.clahey.kinderdraw.shared.imagestorage

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.database.ContentObserver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext

/** MediaStore-backed [ImageStorage] — see the Image Storage LLD's Android Storage Backend. */
// @spec IMAGES-001, IMAGES-002, IMAGES-003, IMAGES-004, IMAGES-005, IMAGES-006, IMAGES-007,
// IMAGES-009, IMAGES-010, IMAGES-011, IMAGES-014, IMAGES-015, IMAGES-016, IMAGES-017,
// IMAGES-018, IMAGES-019
class MediaStoreImageStorage(private val context: Context) : ImageStorage {
    private val collection: Uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
    private val albumRelativePath = "${Environment.DIRECTORY_PICTURES}/KinderDraw/"

    /**
     * The album path as a `LIKE` pattern, which SQLite matches
     * case-insensitively for ASCII — the album directory can be recorded
     * under any casing and every spelling names the same directory. `LIKE`'s
     * own wildcards are escaped so the pattern still matches literally.
     */
    private val albumPathPattern = albumRelativePath
        .replace("\\", "\\\\")
        .replace("%", "\\%")
        .replace("_", "\\_")

    override val entries: Flow<List<SavedDrawingEntry>> = callbackFlow {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                trySend(queryEntries())
            }
        }
        context.contentResolver.registerContentObserver(collection, true, observer)
        trySend(queryEntries())
        awaitClose { context.contentResolver.unregisterContentObserver(observer) }
    }

    private fun queryEntries(): List<SavedDrawingEntry> = buildList {
        val projection = arrayOf(MediaStore.Images.Media._ID, MediaStore.Images.Media.DATE_TAKEN)
        val selection = "${MediaStore.Images.Media.RELATIVE_PATH} LIKE ? ESCAPE '\\'"
        val selectionArgs = arrayOf(albumPathPattern)
        context.contentResolver.query(collection, projection, selection, selectionArgs, null)?.use { cursor ->
            val idIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val dateIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN)
            while (cursor.moveToNext()) {
                add(SavedDrawingEntry(cursor.getLong(idIndex).toString(), cursor.getLong(dateIndex)))
            }
        }
    }

    override suspend fun create(image: ImageBitmap, timestamp: Long?): Result<SavedDrawingEntry> =
        withContext(Dispatchers.IO) {
            resultOf {
                val resolvedTimestamp = timestamp ?: System.currentTimeMillis()
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, "${UUID.randomUUID()}.png")
                    put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                    put(MediaStore.Images.Media.RELATIVE_PATH, albumRelativePath)
                    put(MediaStore.Images.Media.DATE_TAKEN, resolvedTimestamp)
                }
                val uri = context.contentResolver.insert(collection, values)
                    ?: throw ImageStorageException("MediaStore insert failed")
                // The entry exists from here on, so a failure past this point
                // has to take it back out again — a create reported as failed
                // leaves nothing behind. A cleanup that fails itself must not
                // replace the failure that caused it.
                try {
                    writeBitmap(uri, image)
                } catch (e: Throwable) {
                    runCatching { context.contentResolver.delete(uri, null, null) }
                    throw e
                }
                SavedDrawingEntry(ContentUris.parseId(uri).toString(), resolvedTimestamp)
            }
        }

    override suspend fun update(id: String, image: ImageBitmap, timestamp: Long?): Result<SavedDrawingEntry> =
        withContext(Dispatchers.IO) {
            resultOf {
                val uri = uriForId(id) ?: throw notFound(id)
                val existingTimestamp = queryTimestamp(uri) ?: throw notFound(id)
                writeBitmap(uri, image)
                if (timestamp != null) {
                    val values = ContentValues().apply { put(MediaStore.Images.Media.DATE_TAKEN, timestamp) }
                    context.contentResolver.update(uri, values, null, null)
                }
                SavedDrawingEntry(id, timestamp ?: existingTimestamp)
            }
        }

    override suspend fun read(id: String): Result<ImageBitmap> = withContext(Dispatchers.IO) {
        resultOf {
            val uri = uriForId(id) ?: throw notFound(id)
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream)?.asImageBitmap()
            } ?: throw notFound(id)
        }
    }

    override suspend fun delete(id: String): Result<Unit> = withContext(Dispatchers.IO) {
        resultOf {
            val uri = uriForId(id) ?: throw notFound(id)
            val rowsDeleted = context.contentResolver.delete(uri, null, null)
            if (rowsDeleted == 0) throw notFound(id)
        }
    }

    private fun uriForId(id: String): Uri? =
        id.toLongOrNull()?.let { ContentUris.withAppendedId(collection, it) }

    private fun queryTimestamp(uri: Uri): Long? {
        val projection = arrayOf(MediaStore.Images.Media.DATE_TAKEN)
        return context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN))
            } else {
                null
            }
        }
    }

    private fun writeBitmap(uri: Uri, image: ImageBitmap) {
        val output = context.contentResolver.openOutputStream(uri, "wt")
            ?: throw ImageStorageException("Unable to open $uri for writing")
        output.use { stream ->
            if (!image.asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, stream)) {
                throw ImageStorageException("Bitmap compression failed for $uri")
            }
        }
    }

    private fun notFound(id: String) = ImageStorageException("No saved drawing with id $id")

    private inline fun <T> resultOf(block: () -> T): Result<T> =
        try {
            Result.success(block())
        } catch (e: CancellationException) {
            throw e
        } catch (e: ImageStorageException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(ImageStorageException(e.message ?: "Image Storage operation failed", e))
        }
}
