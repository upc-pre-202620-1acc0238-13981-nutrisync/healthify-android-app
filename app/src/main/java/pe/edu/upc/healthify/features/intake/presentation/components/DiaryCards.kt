package pe.edu.upc.healthify.features.intake.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import pe.edu.upc.healthify.R
import pe.edu.upc.healthify.core.designsystem.component.HealthifyButton
import pe.edu.upc.healthify.core.designsystem.component.HealthifyButtonStyle
import pe.edu.upc.healthify.core.designsystem.component.HealthifyCard
import pe.edu.upc.healthify.core.designsystem.component.StatusChip
import pe.edu.upc.healthify.core.designsystem.component.StatusChipType
import pe.edu.upc.healthify.core.designsystem.theme.HealthifyTheme
import pe.edu.upc.healthify.features.intake.domain.entity.DiaryEntry
import pe.edu.upc.healthify.features.intake.domain.entity.PendingDiaryEntry
import pe.edu.upc.healthify.features.intake.domain.entity.PendingMealPhoto
import pe.edu.upc.healthify.features.intake.domain.valueobject.ClientEntryId
import pe.edu.upc.healthify.features.intake.domain.valueobject.DiaryEntryId
import pe.edu.upc.healthify.features.intake.domain.valueobject.EntryProvenance
import pe.edu.upc.healthify.features.intake.domain.valueobject.LocalTimestamp
import pe.edu.upc.healthify.features.intake.domain.valueobject.PlanAdherence
import java.io.File
import java.time.OffsetDateTime

// Miniatura de la foto por analizar (sin frame propio: medida de un ícono grande de lista).
private val PhotoThumbnailSize = 56.dp

/**
 * «Card · Ceviche (por confirmar)» de PT14: la estimación de la foto todavía no cuenta. Muestra procedencia y
 * confianza siempre (*Confidence And Provenance Always Exposed*) y «Confirmar porción» (→ PT8).
 */
@Composable
fun PendingConfirmationCard(entry: DiaryEntry, onConfirmPortion: (() -> Unit)?, modifier: Modifier = Modifier) {
    val dimens = HealthifyTheme.dimens
    HealthifyCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(dimens.space16), verticalArrangement = Arrangement.spacedBy(dimens.space16)) {
            Column(
                modifier = Modifier.semantics(mergeDescendants = true) {},
                verticalArrangement = Arrangement.spacedBy(dimens.space8),
            ) {
                TitleAndTime(
                    title = entryTitle(entry),
                    time = entry.localTimestamp.value,
                    style = MaterialTheme.typography.titleMedium,
                )
                ChipsRow {
                    StatusChip(type = StatusChipType.PendingConfirmation)
                    PhotoChip(entry.confidence)
                }
                Text(
                    text = stringResource(R.string.diary_not_counted_yet),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (onConfirmPortion != null) {
                HealthifyButton(
                    text = stringResource(R.string.diary_confirm_portion),
                    onClick = onConfirmPortion,
                    style = HealthifyButtonStyle.Tonal,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

/** Card de una entrada ya contada (PT14 «Avena con fruta · 250 g · 7:30 a. m.»). No tiene botón de borrar. */
@Composable
fun DiaryEntryCard(entry: DiaryEntry, modifier: Modifier = Modifier) {
    ListEntryCard(title = entryTitle(entry), time = entry.localTimestamp.value, modifier = modifier) {
        when (entry.provenance) {
            EntryProvenance.PHOTO -> {
                PhotoChip(entry.confidence)
                StatusChip(type = StatusChipType.Confirmed, label = stringResource(R.string.diary_chip_confirmed))
            }
            EntryProvenance.MANUAL ->
                StatusChip(type = StatusChipType.Manual, label = stringResource(R.string.diary_chip_manual))
            EntryProvenance.OFF_PLAN -> Unit
        }
        if (entry.isOffPlan) StatusChip(type = StatusChipType.OffPlan)
    }
}

/** Comida registrada desde una idea (IN-6): sus alimentos en una sola card. */
@Composable
fun MealGroupCard(entries: List<DiaryEntry>, modifier: Modifier = Modifier) {
    val dimens = HealthifyTheme.dimens
    val first = entries.first()
    HealthifyCard(modifier = modifier.fillMaxWidth(), shape = MaterialTheme.shapes.extraLarge) {
        Column(
            modifier = Modifier
                .padding(dimens.space16)
                .semantics(mergeDescendants = true) {},
            verticalArrangement = Arrangement.spacedBy(dimens.space8),
        ) {
            TitleAndTime(
                title = stringResource(R.string.diary_meal_group_title),
                time = first.localTimestamp.value,
                style = MaterialTheme.typography.titleSmall,
            )
            entries.forEach { entry ->
                Text(
                    text = stringResource(
                        R.string.diary_meal_group_item,
                        entry.foodName ?: stringResource(R.string.diary_unknown_food),
                        numberText(entry.displayedGrams ?: 0.0),
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            ChipsRow {
                StatusChip(type = StatusChipType.Manual, label = stringResource(R.string.diary_chip_manual))
                if (entries.any { it.isOffPlan }) StatusChip(type = StatusChipType.OffPlan)
            }
        }
    }
}

/** PT14.O · una comida en la cola del teléfono: «Pendiente de enviar» (o rechazada al sincronizar). */
@Composable
fun PendingEntryCard(entry: PendingDiaryEntry, modifier: Modifier = Modifier) {
    val approximate = entry.provenance == EntryProvenance.PHOTO && !entry.confirmed
    ListEntryCard(
        title = stringResource(R.string.format_separator_dot, entry.foodName, gramsText(entry.grams, approximate)),
        time = entry.localTimestamp.value,
        modifier = modifier,
        footer = when {
            entry.isRejected -> stringResource(pendingRejectionRes(entry.rejectionCode))
            approximate -> stringResource(R.string.diary_confirm_after_sync)
            else -> null
        },
    ) {
        if (approximate) StatusChip(type = StatusChipType.PendingConfirmation)
        when (entry.provenance) {
            EntryProvenance.PHOTO -> {
                PhotoChip(entry.confidence)
                if (entry.confirmed) {
                    StatusChip(type = StatusChipType.Confirmed, label = stringResource(R.string.diary_chip_confirmed))
                }
            }
            else -> StatusChip(type = StatusChipType.Manual, label = stringResource(R.string.diary_chip_manual))
        }
        if (entry.planAdherence == PlanAdherence.OFF_PLAN) StatusChip(type = StatusChipType.OffPlan)
        StatusChip(
            type = StatusChipType.PendingSync,
            label = stringResource(if (entry.isRejected) R.string.diary_chip_not_sent else R.string.diary_chip_pending_sync),
        )
    }
}

/** Foto tomada sin conexión: «Analizar ahora» (con red) o «Registrar a mano». */
@Composable
fun PendingPhotoCard(
    photo: PendingMealPhoto,
    isOffline: Boolean,
    onAnalyze: () -> Unit,
    onRegisterManually: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dimens = HealthifyTheme.dimens
    val scheme = MaterialTheme.colorScheme
    HealthifyCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(dimens.space16), verticalArrangement = Arrangement.spacedBy(dimens.space16)) {
            Row(
                modifier = Modifier.semantics(mergeDescendants = true) {},
                horizontalArrangement = Arrangement.spacedBy(dimens.space12),
                verticalAlignment = Alignment.Top,
            ) {
                AsyncImage(
                    model = File(photo.filePath),
                    contentDescription = stringResource(R.string.meal_photo_cd_preview),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(PhotoThumbnailSize)
                        .clip(MaterialTheme.shapes.medium),
                )
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(dimens.space4)) {
                    TitleAndTime(
                        title = stringResource(R.string.diary_pending_photo_title),
                        time = photo.capturedAt.value,
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Text(
                        text = stringResource(
                            if (isOffline) R.string.diary_pending_photo_body_offline else R.string.diary_pending_photo_body,
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant,
                    )
                }
            }
            HealthifyButton(
                text = stringResource(R.string.diary_pending_photo_analyze),
                onClick = onAnalyze,
                enabled = !isOffline,
                style = HealthifyButtonStyle.Tonal,
                modifier = Modifier.fillMaxWidth(),
            )
            HealthifyButton(
                text = stringResource(R.string.diary_pending_photo_manual),
                onClick = onRegisterManually,
                style = HealthifyButtonStyle.Text,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/** Card de lista de 24 dp: título + hora, chips y una nota opcional. */
@Composable
private fun ListEntryCard(
    title: String,
    time: OffsetDateTime,
    modifier: Modifier = Modifier,
    footer: String? = null,
    chips: @Composable () -> Unit,
) {
    val dimens = HealthifyTheme.dimens
    HealthifyCard(modifier = modifier.fillMaxWidth(), shape = MaterialTheme.shapes.extraLarge) {
        Column(
            modifier = Modifier
                .padding(dimens.space16)
                .semantics(mergeDescendants = true) {},
            verticalArrangement = Arrangement.spacedBy(dimens.space8),
        ) {
            TitleAndTime(title = title, time = time, style = MaterialTheme.typography.titleSmall)
            ChipsRow(content = chips)
            if (footer != null) {
                Text(text = footer, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun TitleAndTime(title: String, time: OffsetDateTime, style: TextStyle) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(HealthifyTheme.dimens.space8),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = title, style = style, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
        Text(
            text = time.clockTimeText(),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ChipsRow(content: @Composable () -> Unit) {
    val dimens = HealthifyTheme.dimens
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(dimens.space8),
        verticalArrangement = Arrangement.spacedBy(dimens.space8),
    ) { content() }
}

/** «Foto · confianza 64%» (o «Foto» si una entrada vieja no trae confianza). */
@Composable
private fun PhotoChip(confidence: Double?) {
    StatusChip(
        type = StatusChipType.Photo,
        label = if (confidence != null) {
            stringResource(R.string.diary_chip_photo_confidence, confidencePercent(confidence))
        } else {
            stringResource(R.string.diary_chip_photo)
        },
    )
}

/** «Ceviche · 280 g aprox.» / «Lomo saltado · 320 g» / «Comida sin detalle» (fuera del plan histórica). */
@Composable
private fun entryTitle(entry: DiaryEntry): String {
    val name = entry.foodName ?: stringResource(R.string.diary_unknown_food)
    val grams = entry.displayedGrams ?: return name
    return stringResource(R.string.format_separator_dot, name, gramsText(grams, approximate = entry.isPendingConfirmation))
}

/** Motivo de rechazo al sincronizar (PT19 · Validaciones). */
fun pendingRejectionRes(code: String?): Int = when (code) {
    "LocalTimestampCannotBeRewritten" -> R.string.pending_rejected_timestamp
    "ReferenceFoodNotResolved" -> R.string.pending_rejected_food
    else -> R.string.pending_rejected_generic
}

internal fun previewEntry(
    id: Long,
    name: String,
    time: String,
    provenance: EntryProvenance,
    grams: Double,
    confidence: Double? = null,
    confirmed: Boolean = true,
    adherence: PlanAdherence = PlanAdherence.IN_PLAN,
    group: String? = null,
) = DiaryEntry(
    id = DiaryEntryId(id),
    localTimestamp = LocalTimestamp.restore(OffsetDateTime.parse("2026-09-09T$time:00-05:00")),
    provenance = provenance,
    foodName = name,
    proposedFoodId = 10,
    proposedGrams = if (provenance == EntryProvenance.PHOTO) grams else null,
    confidence = confidence,
    confirmedFoodId = if (confirmed) 10 else null,
    confirmedGrams = if (confirmed) grams else null,
    planAdherence = if (confirmed) adherence else PlanAdherence.NOT_ANSWERED,
    isCountedTowardsTargets = confirmed,
    mealGroupId = group,
)

internal fun previewPending(name: String, time: String, rejection: String? = null) = PendingDiaryEntry(
    clientEntryId = ClientEntryId("2b0c3f4e-8a7d-4a51-9a59-1f0d6c2e9b11"),
    localTimestamp = LocalTimestamp.restore(OffsetDateTime.parse("2026-09-09T$time:00-05:00")),
    provenance = EntryProvenance.PHOTO,
    foodName = name,
    grams = 320.0,
    confidence = 0.82,
    confirmed = true,
    planAdherence = PlanAdherence.IN_PLAN,
    rejectionCode = rejection,
)

@Preview(name = "PT14 · cards del diario", widthDp = 360, heightDp = 800)
@Composable
private fun DiaryCardsPreview() {
    HealthifyTheme {
        Column(
            modifier = Modifier.padding(HealthifyTheme.dimens.space16),
            verticalArrangement = Arrangement.spacedBy(HealthifyTheme.dimens.space12),
        ) {
            PendingConfirmationCard(
                entry = previewEntry(1, "Ceviche", "14:10", EntryProvenance.PHOTO, 280.0, 0.64, confirmed = false),
                onConfirmPortion = {},
            )
            DiaryEntryCard(previewEntry(2, "Avena con fruta", "07:30", EntryProvenance.MANUAL, 250.0))
            PendingEntryCard(previewPending("Lomo saltado", "13:15"))
            DiaryEntryCard(
                previewEntry(4, "Pizza", "20:40", EntryProvenance.MANUAL, 300.0, adherence = PlanAdherence.OFF_PLAN),
            )
        }
    }
}
