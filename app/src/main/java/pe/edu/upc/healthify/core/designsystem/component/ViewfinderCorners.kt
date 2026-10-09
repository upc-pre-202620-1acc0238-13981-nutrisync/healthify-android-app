package pe.edu.upc.healthify.core.designsystem.component

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// «Grupo fijo» de PT1 y PT5: esquinas de 32 × 5 dp (radio 3) a 16 dp del borde del visor de 296 dp.
private val CornerInset = 16.dp
private val CornerLength = 32.dp
private val CornerThickness = 5.dp
private val CornerRadiusDp = 3.dp

/** Las cuatro esquinas del visor de PT1 y PT5, dibujadas sobre la vista de cámara (decorativas). */
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
