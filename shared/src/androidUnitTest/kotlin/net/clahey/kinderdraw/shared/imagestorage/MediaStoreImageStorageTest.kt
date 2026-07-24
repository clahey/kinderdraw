package net.clahey.kinderdraw.shared.imagestorage

import android.content.Context
import android.os.Looper
import android.provider.MediaStore
import androidx.compose.ui.graphics.ImageBitmap
import androidx.test.core.app.ApplicationProvider
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class MediaStoreImageStorageTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val storage = MediaStoreImageStorage(context)

    // @spec IMAGES-001, IMAGES-002
    @Test
    fun createPersistsANewEntryWithAGeneratedIdAndTimestamp() = runBlocking {
        val result = storage.create(ImageBitmap(4, 4))

        assertTrue(result.isSuccess)
        val entry = result.getOrThrow()
        assertTrue(entry.id.isNotBlank())
        assertTrue(entry.timestamp > 0L)
    }

    // @spec IMAGES-006
    @Test
    fun readReturnsTheImagePassedToCreate() = runBlocking {
        val entry = storage.create(ImageBitmap(4, 4)).getOrThrow()

        val readResult = storage.read(entry.id)

        assertTrue(readResult.isSuccess)
    }

    // @spec IMAGES-017
    @Test
    fun readWithNoMatchingEntryReportsFailure() = runBlocking {
        val result = storage.read("no-such-id")

        assertTrue(result.isFailure)
    }

    // @spec IMAGES-003
    @Test
    fun updateReplacesAnExistingEntrysImage() = runBlocking {
        val entry = storage.create(ImageBitmap(4, 4)).getOrThrow()

        val updateResult = storage.update(entry.id, ImageBitmap(8, 8))

        assertTrue(updateResult.isSuccess)
    }

    // @spec IMAGES-004, IMAGES-011
    @Test
    fun updateWithoutAnExplicitTimestampLeavesTheStoredTimestampUnchanged() = runBlocking {
        val entry = storage.create(ImageBitmap(4, 4), timestamp = 1_000L).getOrThrow()

        val updated = storage.update(entry.id, ImageBitmap(4, 4)).getOrThrow()

        assertEquals(1_000L, updated.timestamp)
    }

    // @spec IMAGES-004
    @Test
    fun updateWithAnExplicitTimestampOverwritesTheStoredTimestamp() = runBlocking {
        val entry = storage.create(ImageBitmap(4, 4), timestamp = 1_000L).getOrThrow()

        val updated = storage.update(entry.id, ImageBitmap(4, 4), timestamp = 2_000L).getOrThrow()

        assertEquals(2_000L, updated.timestamp)
    }

    // @spec IMAGES-016, IMAGES-017
    @Test
    fun updateWithNoMatchingEntryReportsFailureRatherThanCreatingOne() = runBlocking {
        val result = storage.update("no-such-id", ImageBitmap(4, 4))

        assertTrue(result.isFailure)
    }

    // @spec IMAGES-007
    @Test
    fun deleteRemovesAnExistingEntry() = runBlocking {
        val entry = storage.create(ImageBitmap(4, 4)).getOrThrow()

        val deleteResult = storage.delete(entry.id)

        assertTrue(deleteResult.isSuccess)
        assertTrue(storage.read(entry.id).isFailure)
    }

    // @spec IMAGES-016, IMAGES-017
    @Test
    fun deleteWithNoMatchingEntryReportsFailure() = runBlocking {
        val result = storage.delete("no-such-id")

        assertTrue(result.isFailure)
    }

    // @spec IMAGES-005, IMAGES-010, IMAGES-014
    @Test
    fun entriesImmediatelyDeliversTheCurrentList() = runBlocking {
        val entry = storage.create(ImageBitmap(4, 4)).getOrThrow()

        val list = storage.entries.first()

        // Exact match, not .any — proves test isolation (no leftover entries
        // from other tests) as a side effect of checking the real behavior.
        assertEquals(listOf(entry), list)
    }

    // @spec IMAGES-015
    @Test
    fun entriesEmitsAgainWhenNotifiedOfAChangeFromAnySource() = runBlocking {
        val emissions = mutableListOf<List<SavedDrawingEntry>>()
        val subscription = launch { storage.entries.collect { emissions.add(it) } }
        delay(50)

        // Simulates a change made outside Image Storage's own API (e.g. the system
        // Gallery app), rather than depending on whether create()'s own MediaStore
        // insert triggers a notification under Robolectric's fake provider.
        context.contentResolver.notifyChange(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, null)
        shadowOf(Looper.getMainLooper()).idle()
        delay(50)
        subscription.cancel()

        assertEquals(2, emissions.size)
    }

    // @spec IMAGES-009
    @Test
    fun anEntryCreatedByOneInstanceIsReadableByALaterInstance() = runBlocking {
        val entry = storage.create(ImageBitmap(4, 4)).getOrThrow()

        val laterInstance = MediaStoreImageStorage(context)

        assertTrue(laterInstance.read(entry.id).isSuccess)
    }
}
