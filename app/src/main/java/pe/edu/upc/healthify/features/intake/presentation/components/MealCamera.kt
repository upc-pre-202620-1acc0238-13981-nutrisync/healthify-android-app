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
 * Folder in `cacheDir` where the camera leaves the dish photo. It must match `AndroidMealPhotoEncoder.PHOTOS_DIR`
 * (infrastructure): only files in this folder are deleted when the flow ends.
 */
private const val MEAL_PHOTOS_DIR = "meal_photos"

/** The photo on the phone needs no more: it is uploaded at 1024 px and the preview asks for no more than this. */
private const val CAPTURE_MAX_SIDE_PX = 2048
private const val CAPTURE_JPEG_QUALITY = 90

/** The meal camera: a `LifecycleCameraController` with photo capture and its save executor. */
class MealCameraState internal constructor(
    internal val controller: LifecycleCameraController,
    private val executor: ExecutorService,
    private val context: Context,
) {
    /**
     * Takes the photo, straightens it and saves it as JPEG in `cacheDir/meal_photos` **without EXIF** (it is re-encoded
     * from the bitmap: no GPS, date or phone model reaches the file). Callbacks arrive on the main thread.
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

    /** On leaving the screen: the save executor finishes whatever is in progress and shuts down. */
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

/** The camera bound to the screen's lifecycle (requires the permission already granted). */
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

/** Camera preview (cropped to fill the square viewfinder). */
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
