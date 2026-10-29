package pe.edu.upc.healthify.features.nutritionalcare.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import pe.edu.upc.healthify.R
import pe.edu.upc.healthify.core.designsystem.component.BottomSheetStaticFrame
import pe.edu.upc.healthify.core.designsystem.component.HealthifyBottomSheet
import pe.edu.upc.healthify.core.designsystem.component.HealthifyTag
import pe.edu.upc.healthify.core.designsystem.component.StatusChip
import pe.edu.upc.healthify.core.designsystem.component.StatusChipType
import pe.edu.upc.healthify.core.designsystem.component.UiText
import pe.edu.upc.healthify.core.designsystem.component.asString
import pe.edu.upc.healthify.core.designsystem.component.mediumDateText
import pe.edu.upc.healthify.core.designsystem.theme.HealthifyTheme
import pe.edu.upc.healthify.features.nutritionalcare.presentation.state.PlanChangeLine
import pe.edu.upc.healthify.features.nutritionalcare.presentation.state.PlanVersionItem
import java.time.Instant

/**
 * Detalle de una versión publicada del plan, solo de consulta: metas, indicaciones, restricciones, qué cambió y el
 * mensaje. Lo usan el paciente (PT4.1) y el nutricionista (PAC-4 «HISTORIAL DEL PLAN»).
 *
 * DECISIÓN PT4.1/PAC-4: no hay frame de detalle de una versión; se muestra en el BottomSheet del sistema de diseño.
 *
 * @param messageTitle rótulo del mensaje: «Mensaje de tu nutricionista» (paciente) o «Mensaje al paciente».
 */
@Composable
fun PlanVersionSheet(item: PlanVersionItem, messageTitle: String, onDismiss: () -> Unit) {
    HealthifyBottomSheet(onDismissRequest = onDismiss) {
        PlanVersionDetail(item = item, messageTitle = messageTitle)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PlanVersionDetail(item: PlanVersionItem, messageTitle: String) {
    val dimens = HealthifyTheme.dimens
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(start = dimens.space24, end = dimens.space24, top = dimens.space16, bottom = dimens.space24),
        verticalArrangement = Arrangement.spacedBy(dimens.space16),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(dimens.space4)) {
                Text(
                    text = stringResource(R.string.plan_version_title, item.version),
                    style = MaterialTheme.typography.headlineSmall,
                    color = scheme.onSurface,
                    modifier = Modifier.semantics { heading() },
                )
                Text(
                    text = stringResource(R.string.plan_version_detail_published, item.publishedAt.mediumDateText()),
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                )
            }
            if (item.isActive) {
                StatusChip(type = StatusChipType.Confirmed, label = stringResource(R.string.plan_versions_current))
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(dimens.space8)) {
            Macro(compactText(item.energyKcal, 0), stringResource(R.string.plan_kcal_per_day), Modifier.weight(1f))
            Macro(grams(item.proteinG), stringResource(R.string.macro_protein), Modifier.weight(1f))
            Macro(grams(item.carbG), stringResource(R.string.macro_carbs_short), Modifier.weight(1f))
            Macro(grams(item.fatG), stringResource(R.string.macro_fat), Modifier.weight(1f))
        }
        TagGroup(stringResource(R.string.plan_guidelines), item.guidelines)
        TagGroup(stringResource(R.string.plan_restrictions), item.restrictions)
        if (item.changes.isNotEmpty()) {
            Group(stringResource(R.string.plan_version_detail_changes)) {
                item.changes.forEach { line ->
                    Text(
                        text = line.text.asString(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (line.secondary) scheme.onSurfaceVariant else scheme.onSurface,
                    )
                }
            }
        }
        item.patientMessage?.let { message ->
            Group(messageTitle) {
                Text(text = message, style = MaterialTheme.typography.bodyMedium, color = scheme.onSurface)
            }
        }
    }
}

@Composable
private fun Macro(value: String, label: String, modifier: Modifier) {
    val scheme = MaterialTheme.colorScheme
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(HealthifyTheme.dimens.space4)) {
        Text(text = value, style = MaterialTheme.typography.titleMedium, color = scheme.onSurface)
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
    }
}

@Composable
private fun grams(value: Double): String = stringResource(R.string.unit_g_value, compactText(value, 0))

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TagGroup(title: String, tags: List<UiText>) {
    val dimens = HealthifyTheme.dimens
    Group(title) {
        if (tags.isEmpty()) {
            Text(
                text = stringResource(R.string.plan_none),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(dimens.space8),
            verticalArrangement = Arrangement.spacedBy(dimens.space8),
        ) {
            tags.forEach { HealthifyTag(text = it.asString()) }
        }
    }
}

@Composable
private fun Group(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(HealthifyTheme.dimens.space8)) {
        Text(text = title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
        content()
    }
}

@Preview(name = "PT4.1 · Detalle de una versión", widthDp = 360)
@Composable
private fun PlanVersionSheetPreview() {
    HealthifyTheme {
        BottomSheetStaticFrame {
            PlanVersionDetail(
                item = PlanVersionItem(
                    version = 2,
                    publishedAt = Instant.parse("2026-06-12T15:00:00Z"),
                    energyKcal = 1900.0,
                    isActive = false,
                    proteinG = 110.0,
                    carbG = 220.0,
                    fatG = 63.0,
                    guidelines = listOf(UiText.of(R.string.guideline_reduce_salt)),
                    changes = listOf(PlanChangeLine(UiText.of(R.string.plan_change_no_target_changes), secondary = true)),
                    patientMessage = "Vamos muy bien, sigue así.",
                ),
                messageTitle = "MENSAJE DE TU NUTRICIONISTA",
            )
        }
    }
}
