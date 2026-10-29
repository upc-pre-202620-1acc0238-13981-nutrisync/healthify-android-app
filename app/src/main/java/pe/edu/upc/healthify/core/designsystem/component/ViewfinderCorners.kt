package pe.edu.upc.healthify.core.designsystem.component

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// Figma viewfinder corners: 32 × 5 dp (radius 3), 16 dp from the edge of the 296 dp viewfinder.
private val CornerInset = 16.dp
private val CornerLength = 32.dp
private val CornerThickness = 5.dp
private val CornerRadiusDp = 3.dp

/** The four corners of the QR scanner and meal camera viewfinder, drawn over the camera view (decorative). */
@Composable
fun ViewfinderCorners(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val inset = CornerInset.toPx()
        val length = CornerLength.toPx()
        val thickness = CornerThickness.toPx()
        val radius = CornerRadius(CornerRadiusDp.toPx())
        val left = inset
        val top = inset
        val right = size.width - inset
        val bottom = size.height - inset
        val horizontal = Size(length, thickness)
        val vertical = Size(thickness, length)
        listOf(
            Offset(left, top) to horizontal,
            Offset(left, top) to vertical,
            Offset(right - length, top) to horizontal,
            Offset(right - thickness, top) to vertical,
            Offset(left, bottom - thickness) to horizontal,
            Offset(left, bottom - length) to vertical,
            Offset(right - length, bottom - thickness) to horizontal,
            Offset(right - thickness, bottom - length) to vertical,
        ).forEach { (topLeft, barSize) -> drawRoundRect(color, topLeft, barSize, radius) }
    }
}
