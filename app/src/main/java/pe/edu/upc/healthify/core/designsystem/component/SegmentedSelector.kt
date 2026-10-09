package pe.edu.upc.healthify.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.IntrinsicMeasurable
import androidx.compose.ui.layout.IntrinsicMeasureScope
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import pe.edu.upc.healthify.core.designsystem.icon.HealthifyIcons
import pe.edu.upc.healthify.core.designsystem.theme.HealthifyTheme

// Ícono «Check» del componente Segmento del Figma.
private val SegmentCheckSize = 18.dp

/**
 * SegmentedButton de selección única del Figma («Segmento»): rol, factor de actividad, peso de referencia,
 * tipo de déficit… Cada opción ocupa el mismo ancho, alto 48 dp, separación 8 dp.
 *
 * Si el texto de alguna opción no cabe en su parte de la fila (diálogos angostos, fuente grande, otro idioma), las
 * opciones se apilan a ancho completo en vez de partir las palabras.
 *
 * @param selected `null` = ninguna opción elegida todavía.
 * @param isError borde `error` en las opciones no elegidas (p. ej. pregunta obligatoria sin responder).
 */
@Composable
fun <T> SegmentedSelector(
    options: List<T>,
    selected: T?,
    onSelect: (T) -> Unit,
    optionLabel: @Composable (T) -> String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isError: Boolean = false,
) {
    val gap = HealthifyTheme.dimens.space8
    Layout(
        content = {
            options.forEach { option ->
                SegmentedOption(
                    text = optionLabel(option),
                    selected = option == selected,
                    onClick = { onSelect(option) },
                    enabled = enabled,
                    isError = isError,
                )
            }
        },
        modifier = modifier
            .fillMaxWidth()
            .selectableGroup(),
        measurePolicy = rowOrColumnPolicy(gap),
    )
}

/** Fila de partes iguales; si una opción no cabe en su parte, columna a ancho completo. */
private fun rowOrColumnPolicy(gap: Dp) = MeasurePolicy { measurables, constraints ->
    val gapPx = gap.roundToPx()
    val count = measurables.size
    if (count == 0) return@MeasurePolicy layout(constraints.minWidth, constraints.minHeight) {}
    // Sin ancho máximo (dentro de un scroll horizontal): cada parte mide lo que pide la opción más larga.
    val width = if (constraints.hasBoundedWidth) {
        constraints.maxWidth
    } else {
        measurables.maxOf { it.maxIntrinsicWidth(Constraints.Infinity) } * count + gapPx * (count - 1)
    }
    val slot = ((width - gapPx * (count - 1)) / count).coerceAtLeast(0)
    val fitsInRow = measurables.all { it.maxIntrinsicWidth(Constraints.Infinity) <= slot }
    if (fitsInRow) {
        val height = measurables.maxOf { it.maxIntrinsicHeight(slot) }
        val placeables = measurables.map { it.measure(Constraints.fixed(slot, height)) }
        layout(width, height) {
            placeables.forEachIndexed { index, placeable -> placeable.placeRelative(index * (slot + gapPx), 0) }
        }
    } else {
        val placeables = measurables.map { it.measure(Constraints(minWidth = width, maxWidth = width)) }
        val height = placeables.sumOf { it.height } + gapPx * (count - 1)
        layout(width, height) {
            var y = 0
            placeables.forEach { placeable ->
                placeable.placeRelative(0, y)
                y += placeable.height + gapPx
            }
        }
    }
}

/** Una opción («Segmento») suelta, para filas que no encajan en [SegmentedSelector]. */
@Composable
fun SegmentedOption(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isError: Boolean = false,
) {
    val dimens = HealthifyTheme.dimens
    val scheme = MaterialTheme.colorScheme
    val ext = HealthifyTheme.extendedColors
    val container = when {
        selected && !enabled -> ext.disabledContainer
        selected -> scheme.primaryContainer
        else -> scheme.surfaceContainerLowest
    }
    val border = when {
        !enabled -> ext.disabledContainer
        selected -> scheme.primary
        isError -> scheme.error
        else -> scheme.outline
    }
    val content = when {
        !enabled -> ext.onDisabled
        selected -> scheme.onPrimaryContainer
        else -> scheme.onSurface
    }
    Row(
        modifier = modifier
            .heightIn(min = dimens.touchMin)
            .clip(CircleShape)
            .background(container)
            .border(if (selected) dimens.borderThick else dimens.borderThin, border, CircleShape)
            .selectable(selected = selected, enabled = enabled, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = dimens.space16),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Layout(
            content = {
                Icon(
                    imageVector = HealthifyIcons.Check,
                    contentDescription = null,
                    tint = content,
                    modifier = Modifier.size(SegmentCheckSize),
                )
                Text(text = text, style = MaterialTheme.typography.labelLarge, color = content, textAlign = TextAlign.Center)
            },
            measurePolicy = CheckAndLabelPolicy(showCheck = selected, gap = dimens.space8),
        )
    }
}

/**
 * Check + texto centrados. El check de la opción elegida solo se dibuja si cabe junto al texto: así elegir una opción
 * nunca reduce el espacio del texto ni lo parte letra por letra. El ancho intrínseco es el del texto (el check no
 * cuenta), para que [SegmentedSelector] decida la fila o la columna igual con o sin selección.
 */
private class CheckAndLabelPolicy(private val showCheck: Boolean, private val gap: Dp) : MeasurePolicy {

    override fun MeasureScope.measure(measurables: List<Measurable>, constraints: Constraints): MeasureResult {
        val (icon, label) = measurables
        val gapPx = gap.roundToPx()
        val iconPlaceable = icon.measure(Constraints())
        val withCheck = iconPlaceable.width + gapPx
        val drawCheck = showCheck &&
            (!constraints.hasBoundedWidth || label.maxIntrinsicWidth(Constraints.Infinity) + withCheck <= constraints.maxWidth)
        val labelMax = if (drawCheck && constraints.hasBoundedWidth) constraints.maxWidth - withCheck else constraints.maxWidth
        val labelPlaceable = label.measure(Constraints(maxWidth = labelMax.coerceAtLeast(0)))
        val contentWidth = labelPlaceable.width + if (drawCheck) withCheck else 0
        val width = contentWidth.coerceIn(constraints.minWidth, constraints.maxWidth)
        val height = maxOf(labelPlaceable.height, if (drawCheck) iconPlaceable.height else 0)
            .coerceIn(constraints.minHeight, constraints.maxHeight)
        return layout(width, height) {
            var x = (width - contentWidth) / 2
            if (drawCheck) {
                iconPlaceable.placeRelative(x, (height - iconPlaceable.height) / 2)
                x += withCheck
            }
            labelPlaceable.placeRelative(x, (height - labelPlaceable.height) / 2)
        }
    }

    override fun IntrinsicMeasureScope.maxIntrinsicWidth(measurables: List<IntrinsicMeasurable>, height: Int): Int =
        measurables[1].maxIntrinsicWidth(height)

    override fun IntrinsicMeasureScope.minIntrinsicWidth(measurables: List<IntrinsicMeasurable>, height: Int): Int =
        measurables[1].minIntrinsicWidth(height)

    override fun IntrinsicMeasureScope.maxIntrinsicHeight(measurables: List<IntrinsicMeasurable>, width: Int): Int =
        maxOf(measurables[0].maxIntrinsicHeight(width), measurables[1].maxIntrinsicHeight(width))

    override fun IntrinsicMeasureScope.minIntrinsicHeight(measurables: List<IntrinsicMeasurable>, width: Int): Int =
        maxOf(measurables[0].minIntrinsicHeight(width), measurables[1].minIntrinsicHeight(width))
}

@Preview(name = "Segmento · Seleccionado × State", widthDp = 360)
@Composable
private fun SegmentedOptionPreview() {
    ComponentPreview {
        Row(horizontalArrangement = Arrangement.spacedBy(HealthifyTheme.dimens.space8)) {
            SegmentedOption("Opción", selected = false, onClick = {}, modifier = Modifier.weight(1f))
            SegmentedOption("Opción", selected = false, onClick = {}, enabled = false, modifier = Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(HealthifyTheme.dimens.space8)) {
            SegmentedOption("Opción", selected = true, onClick = {}, modifier = Modifier.weight(1f))
            SegmentedOption("Opción", selected = true, onClick = {}, enabled = false, modifier = Modifier.weight(1f))
        }
    }
}

@Preview(name = "SegmentedSelector · rol", widthDp = 360)
@Composable
private fun SegmentedSelectorPreview() {
    ComponentPreview {
        Column(verticalArrangement = Arrangement.spacedBy(HealthifyTheme.dimens.space12)) {
            SegmentedSelector(
                options = listOf("Paciente", "Nutricionista"),
                selected = "Paciente",
                onSelect = {},
                optionLabel = { it },
            )
            SegmentedSelector(
                options = listOf("Sedentario", "Ligero", "Activo"),
                selected = null,
                onSelect = {},
                optionLabel = { it },
                modifier = Modifier.padding(top = HealthifyTheme.dimens.space4),
            )
        }
    }
}

@Preview(name = "SegmentedSelector · angosto (diálogo de PT8)", widthDp = 264)
@Composable
private fun SegmentedSelectorNarrowPreview() {
    ComponentPreview {
        SegmentedSelector(
            options = listOf("Hoy", "Ayer", "Anteayer"),
            selected = "Ayer",
            onSelect = {},
            optionLabel = { it },
        )
        SegmentedSelector(
            options = listOf("Today", "Yesterday", "Day before yesterday"),
            selected = "Today",
            onSelect = {},
            optionLabel = { it },
        )
    }
}
