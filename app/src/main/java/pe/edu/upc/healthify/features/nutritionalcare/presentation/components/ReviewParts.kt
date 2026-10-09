package pe.edu.upc.healthify.features.nutritionalcare.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import pe.edu.upc.healthify.R
import pe.edu.upc.healthify.core.designsystem.component.DayMonth
import pe.edu.upc.healthify.core.designsystem.component.HealthifyDialog
import pe.edu.upc.healthify.core.designsystem.component.SectionCard
import pe.edu.upc.healthify.core.designsystem.component.ServerErrorDialog
import pe.edu.upc.healthify.core.designsystem.component.UiText
import pe.edu.upc.healthify.core.designsystem.component.asString
import pe.edu.upc.healthify.core.designsystem.icon.HealthifyIcons
import pe.edu.upc.healthify.core.designsystem.theme.HealthifyTheme
import pe.edu.upc.healthify.features.nutritionalcare.presentation.state.ReviewDialog
import pe.edu.upc.healthify.features.nutritionalcare.presentation.state.ReviewItemHeader
import java.time.Instant
import java.time.ZoneId

private val ChipIconSize = 16.dp

/**
 * Card de evidencia de PR14 / PR14.IA: chip «Desviación sostenida · recibido el 8 sept.» y la frase armada en la app.
 * Si la evidencia usa días registrados se aclara que los días sin registro no cuentan (nunca son incumplimiento).
 */
@Composable
fun ReviewEvidenceCard(header: ReviewItemHeader, modifier: Modifier = Modifier) {
    val dimens = HealthifyTheme.dimens
    val scheme = MaterialTheme.colorScheme
    SectionCard(modifier = modifier.fillMaxWidth()) {
        val signal = header.signal.asString()
        val chipText = header.receivedAt
            ?.let { UiText.of(R.string.review_chip, signal, DayMonth(it.atZone(ZoneId.systemDefault()).toLocalDate())).asString() }
            ?: signal
        Row(
            modifier = Modifier
                .clip(MaterialTheme.shapes.small)
                .background(scheme.secondaryContainer)
                .padding(horizontal = dimens.space8, vertical = dimens.space4),
            horizontalArrangement = Arrangement.spacedBy(dimens.space4),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = HealthifyIcons.Clock,
                contentDescription = null,
                tint = scheme.onSecondaryContainer,
                modifier = Modifier.size(ChipIconSize),
            )
            Text(text = chipText, style = MaterialTheme.typography.labelLarge, color = scheme.onSecondaryContainer)
        }
        val evidence = header.evidence.asString()
        Text(
            text = if (header.mentionsLoggedDays) {
                stringResource(R.string.review_evidence_with_note, evidence, stringResource(R.string.review_unlogged_days_note))
            } else {
                evidence
            },
            style = MaterialTheme.typography.bodyMedium,
            color = scheme.onSurfaceVariant,
        )
    }
}

/** Avisos comunes de PR14, PR14.IA y PR14.IA-A. */
@Composable
fun ReviewDialogHost(dialog: ReviewDialog?, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    when (dialog) {
        null -> Unit
        ReviewDialog.SERVER_ERROR -> ServerErrorDialog(onRetry = onConfirm, onDismiss = onDismiss)
        ReviewDialog.NOT_AVAILABLE -> InfoDialog(R.string.review_not_available_title, R.string.review_not_available_body,
            R.string.review_back_to_inbox, onConfirm)
        ReviewDialog.ALREADY_RESOLVED -> InfoDialog(R.string.review_already_resolved_title,
            R.string.review_already_resolved_body, R.string.review_back_to_inbox, onConfirm)
        ReviewDialog.NO_ACTIVE_LINK -> InfoDialog(R.string.practitioner_no_active_link_title,
            R.string.practitioner_no_active_link_body, R.string.common_understood, onConfirm)
        ReviewDialog.PLAN_CHANGED -> InfoDialog(R.string.review_plan_changed_title, R.string.review_plan_changed_body,
            R.string.common_understood, onConfirm)
    }
}

@Composable
private fun InfoDialog(titleRes: Int, bodyRes: Int, confirmRes: Int, onConfirm: () -> Unit) {
    HealthifyDialog(
        title = stringResource(titleRes),
        text = stringResource(bodyRes),
        confirmLabel = stringResource(confirmRes),
        onConfirm = onConfirm,
        onDismiss = onConfirm,
        dismissLabel = null,
    )
}

/** Fila «etiqueta / valor» de la propuesta de IA (Energía diaria, Nueva indicación, Mensaje, Seguimiento). */
@Composable
fun ProposalRow(
    icon: ImageVector,
    title: String,
    supportingText: String,
    modifier: Modifier = Modifier,
) {
    val dimens = HealthifyTheme.dimens
    val scheme = MaterialTheme.colorScheme
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(dimens.space12)) {
        Icon(imageVector = icon, contentDescription = null, tint = scheme.primary)
        Column(verticalArrangement = Arrangement.spacedBy(dimens.space4)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium, color = scheme.onSurface)
            Text(text = supportingText, style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant)
        }
    }
}

@Preview(name = "PR14 · Card de evidencia", widthDp = 360, heightDp = 240)
@Composable
private fun ReviewEvidenceCardPreview() {
    HealthifyTheme {
        Column(modifier = Modifier.background(MaterialTheme.colorScheme.surface).padding(HealthifyTheme.dimens.space16)) {
            ReviewEvidenceCard(
                header = ReviewItemHeader(
                    patientName = "Ana Flores",
                    signal = UiText.Raw("Desviación sostenida"),
                    receivedAt = Instant.parse("2026-09-08T15:00:00Z"),
                    evidence = UiText.Raw(
                        "Ana Flores registró en promedio 40 % menos de su meta de energía en 5 de sus últimos 9 días registrados.",
                    ),
                    mentionsLoggedDays = true,
                ),
            )
        }
    }
}
