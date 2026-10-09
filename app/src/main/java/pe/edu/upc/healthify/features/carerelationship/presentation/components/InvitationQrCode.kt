package pe.edu.upc.healthify.features.carerelationship.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import pe.edu.upc.healthify.core.designsystem.theme.HealthifyTheme

// QR del frame de PR2 (163 dp).
private val QrSize = 163.dp

/**
 * PR2 · QR de la invitación, generado en el teléfono con ZXing core a partir del token. Contiene **solo el token**
 * (DECISIÓN PT1/PR2): PT1 lo lee con ML Kit y lo canjea. Módulos `onSurface` sobre `surfaceContainerLowest`, con la
 * zona de silencio como padding.
 */
@Composable
fun InvitationQrCode(
    content: String,
    contentDescription: String,
    modifier: Modifier = Modifier,
    size: Dp = QrSize,
) {
    val scheme = MaterialTheme.colorScheme
    val dimens = HealthifyTheme.dimens
    val matrix = remember(content) { encode(content) }
    Box(
        modifier = modifier
            .size(size)
            .clip(MaterialTheme.shapes.medium)
            .background(scheme.surfaceContainerLowest)
            .padding(dimens.space8)
            .semantics { this.contentDescription = contentDescription },
    ) {
        Canvas(modifier = Modifier.size(size - dimens.space16)) {
            val modules = matrix.width
            val cell = this.size.minDimension / modules
            for (y in 0 until modules) {
                for (x in 0 until modules) {
                    if (matrix[x, y]) {
                        drawRect(
                            color = scheme.onSurface,
                            topLeft = Offset(x * cell, y * cell),
                            size = Size(cell, cell),
                        )
                    }
                }
            }
        }
    }
}

private fun encode(content: String): BitMatrix = QRCodeWriter().encode(
    content,
    BarcodeFormat.QR_CODE,
    0,
    0,
    mapOf(
        EncodeHintType.MARGIN to 0,
        EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
    ),
)
