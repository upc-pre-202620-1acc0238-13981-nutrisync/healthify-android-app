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

/** Prepara la foto del plato para subirla (IN-7). Separado del repositorio para probar este en la JVM. */
interface MealPhotoEncoder {

    /**
     * Lee [photoPath], la achica a [MAX_SIDE_PX] px por el lado mayor, la comprime a JPEG (≤ [MAX_BYTES]) y le borra
     * todos los metadatos. @throws IOException si el archivo no se puede leer o decodificar.
     */
    suspend fun encodeForUpload(photoPath: String): ByteArray

    /** Si [photoPath] es una foto de este flujo (dentro de [photosDir]): solo esas se borran. */
    fun isOwnPhoto(photoPath: String): Boolean

    /** Carpeta de las fotos del plato en `cacheDir`. */
    val photosDir: File

    companion object {
        const val MAX_SIDE_PX = 1024

        /** `Intake:MealPhoto:MaxBytes` del backend (2 MB); con 1024 px nunca se acerca. */
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
            // Bitmap.compress no escribe EXIF, pero se limpia igual: es la regla, no un efecto secundario.
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

    /** La mayor potencia de 2 que deja el lado mayor en al menos [MealPhotoEncoder.MAX_SIDE_PX]. */
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
        /** Carpeta en `cacheDir` donde la cámara de PT5 guarda la foto (sin EXIF) mientras dura el flujo. */
        const val PHOTOS_DIR = "meal_photos"
        private const val INITIAL_QUALITY = 85
        private const val MIN_QUALITY = 55
        private const val QUALITY_STEP = 10
    }
}
