package pe.edu.upc.healthify.features.intake.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import pe.edu.upc.healthify.R
import pe.edu.upc.healthify.core.designsystem.component.UiText
import pe.edu.upc.healthify.core.designsystem.component.currentLocale
import pe.edu.upc.healthify.core.designsystem.theme.HealthifyTheme
import pe.edu.upc.healthify.features.intake.domain.entity.WeightTrendPoint
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.floor

// «Gráfico · tendencia» de PT13: 156 dp de alto, columna de etiquetas kg de 28 dp + 16 de separación, línea de 3 dp.
private val ChartHeight = 156.dp
private val AxisLabelWidth = 28.dp
private val AxisGap = 16.dp
private val LineWidth = 3.dp
private const val Y_LABELS = 3
private const val X_LABELS = 4

/**
 * Eje Y redondeado a kilos enteros con un tramo par (tres etiquetas enteras: 67 · 68 · 69 kg). Es solo la escala del
 * eje: el gráfico nunca destaca un valor de un día.
 */
internal data class WeightAxis(val lowKg: Double, val highKg: Double) {
    val labels: List<Double> get() = List(Y_LABELS) { index -> highKg - index * (highKg - lowKg) / (Y_LABELS - 1) }

    companion object {
        fun of(points: List<WeightTrendPoint>): WeightAxis {
            val low = floor(points.minOf { it.smoothedKg })
            var high = ceil(points.maxOf { it.smoothedKg })
            if (high - low < Y_LABELS - 1) high = low + (Y_LABELS - 1)
            if ((high - low).toInt() % 2 != 0) high += 1.0
            return WeightAxis(low, high)
        }
    }
}

/** «15 ago» / «Aug 15». */
internal fun shortDayMonthText(date: LocalDate, locale: Locale): String {
    val pattern = if (locale.language == Locale.ENGLISH.language) "MMM d" else "d MMM"
    return DateTimeFormatter.ofPattern(pattern, locale).format(date).removeSuffix(".")
}

/**
 * Línea de la tendencia de PT13: el promedio móvil (primary) sobre líneas guía (outlineVariant), eje en kg y 4
 * fechas del rango. Dibuja solo la serie suavizada; no marca ni rotula ningún punto («nunca el peso de hoy»).
 *
 * @param from primer día del eje (inicio del rango pedido); [to] el último (hoy).
 */
@Composable
fun WeightTrendChart(
    points: List<WeightTrendPoint>,
    from: LocalDate,
    to: LocalDate,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val guideWidth = HealthifyTheme.dimens.borderThin
    val labelStyle = MaterialTheme.typography.labelSmall.copy(color = scheme.onSurfaceVariant)
    val measurer = rememberTextMeasurer()
    val locale = currentLocale()
    val axis = remember(points) { WeightAxis.of(points) }
    val spanDays = ChronoUnit.DAYS.between(from, to).coerceAtLeast(1)
    val kgLabels = axis.labels.map { stringResource(R.string.format_kg, UiText.formatNumber(it, locale)) }
    val dateLabels = remember(from, to, locale) {
        List(X_LABELS) { index -> from.plusDays(spanDays * index / (X_LABELS - 1)) }
            .map { it to shortDayMonthText(it, locale) }
    }
    val description = stringResource(
        R.string.weight_trend_chart_description,
        dayMonthText(from, locale),
        dayMonthText(to, locale),
    )
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(ChartHeight)
            .semantics { contentDescription = description },
    ) {
        val labelHeight = measurer.measure(kgLabels.first(), labelStyle).size.height.toFloat()
        val left = (AxisLabelWidth + AxisGap).toPx()
        val right = size.width
        val top = labelHeight / 2
        val bottom = size.height - labelHeight * 2 + labelHeight / 2
        val plotHeight = bottom - top
        fun yOf(kg: Double): Float = (bottom - (kg - axis.lowKg) / (axis.highKg - axis.lowKg) * plotHeight).toFloat()
        fun xOf(date: LocalDate): Float =
            left + (right - left) * ChronoUnit.DAYS.between(from, date).toFloat() / spanDays.toFloat()

        // Líneas guía y etiquetas kg.
        axis.labels.forEachIndexed { index, kg ->
            val y = yOf(kg)
            drawLine(scheme.outlineVariant, Offset(left, y), Offset(right, y), strokeWidth = guideWidth.toPx())
            val layout = measurer.measure(kgLabels[index], labelStyle)
            drawText(layout, topLeft = Offset(0f, y - layout.size.height / 2f))
        }
        // Fechas: centradas bajo su día, sin salirse del gráfico.
        dateLabels.forEach { (date, text) ->
            val layout = measurer.measure(text, labelStyle)
            val x = (xOf(date) - layout.size.width / 2f).coerceIn(left, right - layout.size.width)
            drawText(layout, topLeft = Offset(x, size.height - layout.size.height))
        }
        // Promedio móvil: curva suave entre puntos (tangentes horizontales, sin rebasar los valores).
        val offsets = points.map { Offset(xOf(it.date), yOf(it.smoothedKg)) }
        val path = Path().apply {
            moveTo(offsets.first().x, offsets.first().y)
            offsets.zipWithNext().forEach { (a, b) ->
                val mid = (a.x + b.x) / 2
                cubicTo(mid, a.y, mid, b.y, b.x, b.y)
            }
        }
        drawPath(
            path = path,
            color = scheme.primary,
            style = Stroke(width = LineWidth.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
    }
}

@Preview(name = "PT13 · Gráfico de tendencia", widthDp = 296)
@Composable
private fun WeightTrendChartPreview() {
    val from = LocalDate.parse("2026-08-15")
    val values = listOf(67.3, 67.5, 67.6, 67.55, 67.5, 67.5, 67.8, 68.2, 68.4, 68.5, 68.6, 68.9)
    HealthifyTheme {
        WeightTrendChart(
            points = values.mapIndexed { index, kg -> WeightTrendPoint(from.plusDays(index * 2L + 1), kg) },
            from = from,
            to = LocalDate.parse("2026-09-12"),
        )
    }
}
