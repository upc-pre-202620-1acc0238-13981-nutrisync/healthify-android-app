package pe.edu.upc.healthify.features.intake.presentation.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.core.graphics.scale
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.UUID
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Carpeta de `cacheDir` donde PT5 deja la foto del plato. Debe coincidir con `AndroidMealPhotoEncoder.PHOTOS_DIR`
 * (infrastructure): solo los archivos de esta carpeta se borran al terminar el flujo.
 */
private const val MEAL_PHOTOS_DIR = "meal_photos"

/** La foto en el teléfono no necesita más: se sube a 1024 px y la vista previa no pide más de esto. */
private const val CAPTURE_MAX_SIDE_PX = 2048
private const val CAPTURE_JPEG_QUALITY = 90

/** Cámara de PT5: un `LifecycleCameraController` con captura de fotos y su ejecutor de guardado. */
class MealCameraState internal constructor(
    internal val controller: LifecycleCameraController,
    private val executor: ExecutorService,
    private val context: Context,
) {
    /**
     * Toma la foto, la endereza y la guarda como JPEG en `cacheDir/meal_photos` **sin EXIF** (se reencoda desde el
     * bitmap: ni GPS, ni fecha, ni modelo del teléfono llegan al archivo). Los callbacks llegan en el hilo principal.
     */
    fun capture(onSaved: (path: String) -> Unit, onError: () -> Unit) {
        val mainExecutor = ContextCompat.getMainExecutor(context)
        controller.takePicture(
            executor,
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    val path = try {
                        image.use { saveWithoutMetadata(it) }
                    } catch (_: IOException) {
                        null
                    } catch (_: IllegalStateException) {
                        null
                    }
                    mainExecutor.execute { if (path != null) onSaved(path) else onError() }
                }

                override fun onError(exception: ImageCaptureException) {
                    mainExecutor.execute(onError)
                }
            },
        )
    }

    /** Al salir de la pantalla: el ejecutor de guardado termina lo que tenga en curso y se cierra. */
    internal fun release() = executor.shutdown()

    private fun saveWithoutMetadata(image: ImageProxy): String {
        val rotation = image.imageInfo.rotationDegrees
        val source = image.toBitmap()
        val scaled = source.scaledToMaxSide(CAPTURE_MAX_SIDE_PX)
        val upright = if (rotation == 0) {
            scaled
        } else {
            Bitmap.createBitmap(scaled, 0, 0, scaled.width, scaled.height, Matrix().apply { postRotate(rotation.toFloat()) }, true)
        }
        val dir = File(context.cacheDir, MEAL_PHOTOS_DIR).apply { mkdirs() }
        val file = File(dir, "${UUID.randomUUID()}.jpg")
        try {
            FileOutputStream(file).use { stream ->
                if (!upright.compress(Bitmap.CompressFormat.JPEG, CAPTURE_JPEG_QUALITY, stream)) throw IOException("JPEG")
            }
        } finally {
            if (upright !== scaled) upright.recycle()
            if (scaled !== source) scaled.recycle()
            source.recycle()
        }
        return file.absolutePath
    }

    private fun Bitmap.scaledToMaxSide(maxSide: Int): Bitmap {
        val longSide = max(width, height)
        if (longSide <= maxSide) return this
        val factor = maxSide.toFloat() / longSide
        return scale((width * factor).roundToInt(), (height * factor).roundToInt())
    }
}

/** La cámara ligada al ciclo de vida de la pantalla (requiere el permiso ya concedido). */
@Composable
fun rememberMealCameraState(): MealCameraState {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val state = remember {
        MealCameraState(
            controller = LifecycleCameraController(context).apply {
                setEnabledUseCases(CameraController.IMAGE_CAPTURE)
                imageCaptureMode = ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY
            },
            executor = Executors.newSingleThreadExecutor(),
            context = context.applicationContext,
        )
    }
    DisposableEffect(lifecycleOwner, state) {
        state.controller.bindToLifecycle(lifecycleOwner)
        onDispose { state.controller.unbind() }
    }
    DisposableEffect(state) {
        onDispose { state.release() }
    }
    return state
}

/** Vista previa de la cámara (se recorta para llenar el visor cuadrado de PT5). */
@Composable
fun MealCameraPreview(state: MealCameraState, modifier: Modifier = Modifier) {
    AndroidView(
        factory = { viewContext ->
            PreviewView(viewContext).apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER
                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                controller = state.controller
            }
        },
        modifier = modifier,
    )
}
