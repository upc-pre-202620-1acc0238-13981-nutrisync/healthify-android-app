package pe.edu.upc.healthify.features.nutritionalcare.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import pe.edu.upc.healthify.R
import pe.edu.upc.healthify.core.designsystem.component.EmptyState
import pe.edu.upc.healthify.core.designsystem.component.ErrorState
import pe.edu.upc.healthify.core.designsystem.component.HealthifyTopAppBar
import pe.edu.upc.healthify.core.designsystem.component.OfflineBanner
import pe.edu.upc.healthify.core.designsystem.component.OfflineBannerType
import pe.edu.upc.healthify.core.designsystem.component.SectionOverline
import pe.edu.upc.healthify.core.designsystem.component.SkeletonCard
import pe.edu.upc.healthify.core.designsystem.component.SystemBarsAppearance
import pe.edu.upc.healthify.core.designsystem.theme.HealthifyTheme
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.ConsultationStep
import pe.edu.upc.healthify.features.nutritionalcare.presentation.state.StepLoadState

// Indicador lineal del Figma: 4 tramos de 4 dp.
private val ProgressHeight = 4.dp

/**
 * Estructura de EV-2 a EV-5: TopAppBar «CONSULTA DE HOY» con ‹, «PASO n DE 4 · …» con su indicador lineal, el
 * contenido con scroll y las acciones fijas abajo. Mientras se lee la consulta en curso muestra el esqueleto; si falla,
 * el error (o «Necesitas conexión»).
 */
@Composable
fun ConsultationStepScaffold(
    step: ConsultationStep,
    load: StepLoadState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    actions: @Composable ColumnScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    val dimens = HealthifyTheme.dimens
    SystemBarsAppearance(darkBackground = false)
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .imePadding(),
    ) {
        HealthifyTopAppBar(title = stringResource(R.string.consultation_title), onBack = onBack)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(start = dimens.space16, top = dimens.space16, end = dimens.space16, bottom = dimens.space24),
            verticalArrangement = Arrangement.spacedBy(dimens.space24),
        ) {
            if (load.isOffline) OfflineBanner(type = OfflineBannerType.RequiresNetwork)
            StepProgress(step = step, subtitle = subtitle)
            when {
                load.isLoading -> repeat(2) { SkeletonCard(modifier = Modifier.fillMaxWidth()) }
                load.loadFailed && load.isOffline -> EmptyState(
                    title = stringResource(R.string.roster_offline_title),
                    text = stringResource(R.string.consultation_offline_body),
                    actionLabel = stringResource(R.string.ds_retry),
                    onAction = onRetry,
                    modifier = Modifier.fillMaxWidth(),
                )
                load.loadFailed -> ErrorState(onRetry = onRetry, modifier = Modifier.fillMaxWidth())
                else -> content()
            }
        }
        if (!load.isLoading && !load.loadFailed) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(dimens.space16),
                verticalArrangement = Arrangement.spacedBy(dimens.space12),
                content = actions,
            )
        }
    }
}

/** «PASO 2 DE 4 · DIAGNÓSTICO» + indicador lineal (tramos hechos en `primary`). */
@Composable
fun StepProgress(step: ConsultationStep, subtitle: String?, modifier: Modifier = Modifier) {
    val dimens = HealthifyTheme.dimens
    val scheme = MaterialTheme.colorScheme
    val title = stringResource(
        R.string.consultation_step_header,
        step.number,
        ConsultationStep.TOTAL,
        stringResource(step.titleRes()),
    ).uppercase(pe.edu.upc.healthify.core.designsystem.component.currentLocale())
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(dimens.space8)) {
        SectionOverline(title)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = title },
            horizontalArrangement = Arrangement.spacedBy(dimens.space4),
        ) {
            ConsultationStep.entries.forEach { segment ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(ProgressHeight)
                        .clip(MaterialTheme.shapes.extraSmall)
                        .background(if (segment.number <= step.number) scheme.primary else scheme.surfaceContainerHigh),
                )
            }
        }
        if (subtitle != null) {
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
        }
    }
}
