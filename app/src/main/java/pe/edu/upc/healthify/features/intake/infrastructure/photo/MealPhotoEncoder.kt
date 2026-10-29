package pe.edu.upc.healthify.features.intake.infrastructure.photo

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import pe.edu.upc.healthify.core.di.IoDispatcher
import androidx.core.graphics.scale
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import javax.inject.Inject
import kotlin.math.max
import kotlin.math.roundToInt

/** Prepares the dish photo for upload. Kept apart from the repository so it can be tested on the JVM. */
interface MealPhotoEncoder {

    /**
     * Reads [photoPath], shrinks it to [MAX_SIDE_PX] px on the longer side, compresses it to JPEG (≤ [MAX_BYTES]) and
     * strips all metadata. @throws IOException if the file cannot be read or decoded.
     */
    suspend fun encodeForUpload(photoPath: String): ByteArray

    /** Whether [photoPath] is a photo from this flow (inside [photosDir]): only those are deleted. */
    fun isOwnPhoto(photoPath: String): Boolean

    /** Folder for the dish photos in `cacheDir`. */
    val photosDir: File

    companion object {
        const val MAX_SIDE_PX = 1024

        /** The backend's `Intake:MealPhoto:MaxBytes` (2 MB); at 1024 px it never gets close. */
        const val MAX_BYTES = 2 * 1024 * 1024
    }
}

class AndroidMealPhotoEncoder @Inject constructor(
    @ApplicationContext context: Context,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : MealPhotoEncoder {

    override val photosDir: File = File(context.cacheDir, PHOTOS_DIR)

    override suspend fun encodeForUpload(photoPath: String): ByteArray = withContext(ioDispatcher) {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(photoPath, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) throw IOException("Unreadable photo")

        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSizeFor(max(bounds.outWidth, bounds.outHeight))
        }
        val decoded = BitmapFactory.decodeFile(photoPath, options) ?: throw IOException("Unreadable photo")
        val scaled = decoded.scaledToMaxSide(MealPhotoEncoder.MAX_SIDE_PX)
        try {
            var quality = INITIAL_QUALITY
            var jpeg = scaled.toJpeg(quality)
            while (jpeg.size > MealPhotoEncoder.MAX_BYTES && quality > MIN_QUALITY) {
                quality -= QUALITY_STEP
                jpeg = scaled.toJpeg(quality)
            }
            // Bitmap.compress does not write EXIF, but it is stripped anyway: it is the rule, not a side effect.
            JpegMetadataStripper.strip(jpeg)
        } finally {
            if (scaled !== decoded) scaled.recycle()
            decoded.recycle()
        }
    }

    override fun isOwnPhoto(photoPath: String): Boolean =
        try {
            File(photoPath).canonicalPath.startsWith(photosDir.canonicalPath + File.separator)
        } catch (_: IOException) {
            false
        }

    /** The largest power of 2 that leaves the longer side at least [MealPhotoEncoder.MAX_SIDE_PX]. */
    private fun sampleSizeFor(longSide: Int): Int {
        var sample = 1
        while (longSide / (sample * 2) >= MealPhotoEncoder.MAX_SIDE_PX) sample *= 2
        return sample
    }

    private fun Bitmap.scaledToMaxSide(maxSide: Int): Bitmap {
        val longSide = max(width, height)
        if (longSide <= maxSide) return this
        val factor = maxSide.toFloat() / longSide
        return scale((width * factor).roundToInt(), (height * factor).roundToInt())
    }

    private fun Bitmap.toJpeg(quality: Int): ByteArray =
        ByteArrayOutputStream().use { stream ->
            compress(Bitmap.CompressFormat.JPEG, quality, stream)
            stream.toByteArray()
        }

    companion object {
        /** Folder in `cacheDir` where the meal camera keeps the photo (no EXIF) for the length of the flow. */
        const val PHOTOS_DIR = "meal_photos"
        private const val INITIAL_QUALITY = 85
        private const val MIN_QUALITY = 55
        private const val QUALITY_STEP = 10
    }
}
