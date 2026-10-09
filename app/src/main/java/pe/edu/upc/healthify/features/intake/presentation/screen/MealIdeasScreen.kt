package pe.edu.upc.healthify.features.intake.presentation.screen

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pe.edu.upc.healthify.R
import pe.edu.upc.healthify.core.designsystem.component.EmptyState
import pe.edu.upc.healthify.core.designsystem.component.FailureState
import pe.edu.upc.healthify.core.designsystem.component.HealthifyButton
import pe.edu.upc.healthify.core.designsystem.component.HealthifyButtonStyle
import pe.edu.upc.healthify.core.designsystem.component.HealthifyCard
import pe.edu.upc.healthify.core.designsystem.component.HealthifyTopAppBar
import pe.edu.upc.healthify.core.designsystem.component.ObserveEvents
import pe.edu.upc.healthify.core.designsystem.component.ServerErrorDialog
import pe.edu.upc.healthify.core.designsystem.component.SkeletonCard
import pe.edu.upc.healthify.core.designsystem.component.SystemBarsAppearance
import pe.edu.upc.healthify.core.designsystem.component.UiText
import pe.edu.upc.healthify.core.designsystem.component.asString
import pe.edu.upc.healthify.core.designsystem.icon.HealthifyIcons
import pe.edu.upc.healthify.core.designsystem.theme.HealthifyTheme
import pe.edu.upc.healthify.features.intake.domain.entity.MealIdea
import pe.edu.upc.healthify.features.intake.domain.entity.MealIdeaIngredient
import pe.edu.upc.healthify.features.intake.domain.entity.MealIdeas
import pe.edu.upc.healthify.features.intake.domain.entity.MealLogOutcome
import pe.edu.upc.healthify.features.intake.domain.valueobject.MealFood
import pe.edu.upc.healthify.features.intake.domain.valueobject.RestrictionCode
import pe.edu.upc.healthify.core.designsystem.component.AiBadge
import pe.edu.upc.healthify.features.intake.presentation.components.numberText
import pe.edu.upc.healthify.features.intake.presentation.components.restrictionsPhrase
import pe.edu.upc.healthify.features.intake.presentation.state.MealIdeasEvent
import pe.edu.upc.healthify.features.intake.presentation.state.MealIdeasFailure
import pe.edu.upc.healthify.features.intake.presentation.state.MealIdeasUiState
import pe.edu.upc.healthify.features.intake.presentation.viewmodel.MealIdeasViewModel
import java.time.LocalDate

data class MealIdeasActions(
    val onBack: () -> Unit = {},
    val onRetry: () -> Unit = {},
    val onMoreIdeas: () -> Unit = {},
    val onIdeaSelected: (String) -> Unit = {},
    val onRegisterIdea: () -> Unit = {},
    val onDismissErrors: () -> Unit = {},
)

/** PT14.4 Ideas para hoy (+ PT14.4.E, PT14.4.O) y PT14.5 Detalle. */
@Composable
fun MealIdeasScreen(
    onBack: () -> Unit,
    onLogged: (MealLogOutcome) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MealIdeasViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ObserveEvents(viewModel.events) { event ->
        when (event) {
            is MealIdeasEvent.Logged -> onLogged(event.outcome)
        }
    }
    val back = { if (!viewModel.onBack()) onBack() }
    BackHandler(enabled = state.selectedIdeaId != null) { viewModel.onBack() }
    MealIdeasContent(
        state = state,
        actions = MealIdeasActions(
            onBack = back,
            onRetry = viewModel::onRetry,
            onMoreIdeas = viewModel::onMoreIdeas,
            onIdeaSelected = viewModel::onIdeaSelected,
            onRegisterIdea = viewModel::onRegisterIdea,
            onDismissErrors = viewModel::onDismissErrors,
        ),
        modifier = modifier,
    )
}

@Composable
fun MealIdeasContent(state: MealIdeasUiState, actions: MealIdeasActions, modifier: Modifier = Modifier) {
    val idea = state.selectedIdea
    val ideas = state.ideas
    when {
        idea != null && ideas != null -> IdeaDetail(idea, state, actions, modifier)
        state.failure == MealIdeasFailure.GENERIC || state.failure == MealIdeasFailure.RATE_LIMITED -> {
            // PT14.4.E · sin barra superior, como el frame.
            SystemBarsAppearance(darkBackground = false)
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface)
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = HealthifyTheme.dimens.space16, vertical = HealthifyTheme.dimens.space40),
                contentAlignment = Alignment.Center,
            ) {
                FailureState(
                    title = stringResource(R.string.ideas_error_title),
                    text = stringResource(
                        if (state.failure == MealIdeasFailure.RATE_LIMITED) R.string.ideas_rate_limited_body else R.string.ideas_error_body,
                    ),
                    actionLabel = stringResource(R.string.meal_photo_retry),
                    onAction = actions.onRetry,
                    actionLoading = state.isLoading,
                    secondaryLabel = stringResource(R.string.common_close),
                    onSecondary = actions.onBack,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        else -> IdeasScaffold(title = stringResource(R.string.ideas_title), onBack = actions.onBack, modifier = modifier) {
            IdeasListBody(state, actions)
        }
    }
    when {
        state.showRegisterError -> ServerErrorDialog(onRetry = actions.onRegisterIdea, onDismiss = actions.onDismissErrors)
        state.showMoreError -> ServerErrorDialog(onRetry = actions.onMoreIdeas, onDismiss = actions.onDismissErrors)
    }
}

@Composable
private fun ColumnScope.IdeasListBody(state: MealIdeasUiState, actions: MealIdeasActions) {
    val ideas = state.ideas
    val emptyModifier = Modifier
        .fillMaxWidth()
        .heightIn(min = HealthifyTheme.dimens.space64 * EMPTY_HEIGHT_FACTOR)
    when {
        state.isLoading -> {
            SkeletonCard()
            SkeletonCard()
        }
        ideas != null -> {
            Text(
                text = introText(ideas).asString(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            IdeasCard(ideas = ideas.ideas, onSelect = actions.onIdeaSelected)
            HealthifyButton(
                text = stringResource(R.string.ideas_more),
                onClick = actions.onMoreIdeas,
                style = HealthifyButtonStyle.Text,
                loading = state.isLoadingMore,
                enabled = !state.isOffline,
                modifier = Modifier.fillMaxWidth(),
            )
            Disclaimer()
        }
        state.isOffline -> EmptyState(
            title = stringResource(R.string.ideas_offline_title),
            text = stringResource(R.string.ideas_offline_body),
            modifier = emptyModifier,
        )
        state.failure == MealIdeasFailure.NOT_ENOUGH_REMAINING -> EmptyState(
            title = stringResource(R.string.ideas_not_enough_title),
            text = stringResource(R.string.ideas_not_enough_body),
            modifier = emptyModifier,
        )
        state.failure == MealIdeasFailure.NO_TARGETS -> EmptyState(
            title = stringResource(R.string.ideas_no_targets_title),
            text = stringResource(R.string.ideas_no_targets_body),
            modifier = emptyModifier,
        )
        else -> EmptyState(
            title = stringResource(R.string.ideas_unavailable_title),
            text = stringResource(R.string.ideas_unavailable_body),
            modifier = emptyModifier,
        )
    }
}

/** «Caben en lo que te queda hoy (590 kcal) y no llevan mariscos.». */
private fun introText(ideas: MealIdeas): UiText {
    val restrictions = restrictionsPhrase(ideas.restrictions)
    return if (restrictions == null) {
        UiText.of(R.string.ideas_intro_no_restrictions, ideas.remainingKcal)
    } else {
        UiText.of(R.string.ideas_intro, ideas.remainingKcal, restrictions)
    }
}

/** «Card · Ideas (IA)»: una fila por idea (nombre, kcal y proteína) separadas por divisores. */
@Composable
private fun IdeasCard(ideas: List<MealIdea>, onSelect: (String) -> Unit) {
    val dimens = HealthifyTheme.dimens
    val scheme = MaterialTheme.colorScheme
    HealthifyCard(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.extraLarge, borderColor = scheme.tertiary) {
        Column(modifier = Modifier.padding(dimens.space16), verticalArrangement = Arrangement.spacedBy(dimens.space12)) {
            AiBadge(label = stringResource(R.string.diary_ideas_badge))
            ideas.forEachIndexed { index, idea ->
                if (index > 0) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(dimens.borderThin)
                            .background(scheme.outlineVariant),
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = dimens.touchMin)
                        .clickable(role = Role.Button) { onSelect(idea.id) }
                        .semantics(mergeDescendants = true) {},
                    horizontalArrangement = Arrangement.spacedBy(dimens.space8),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(HealthifyIcons.Bowl, contentDescription = null, tint = scheme.primary)
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(dimens.space4)) {
                        Text(text = idea.name, style = MaterialTheme.typography.titleSmall, color = scheme.onSurface)
                        Text(
                            text = stringResource(R.string.ideas_row_summary, numberText(idea.energyKcal), numberText(idea.proteinG)),
                            style = MaterialTheme.typography.bodySmall,
                            color = scheme.onSurfaceVariant,
                        )
                    }
                    Icon(HealthifyIcons.ChevronRight, contentDescription = null, tint = scheme.onSurface)
                }
            }
        }
    }
}

// ----- PT14.5 · Detalle -----

@Composable
private fun IdeaDetail(idea: MealIdea, state: MealIdeasUiState, actions: MealIdeasActions, modifier: Modifier) {
    val dimens = HealthifyTheme.dimens
    val scheme = MaterialTheme.colorScheme
    IdeasScaffold(
        title = stringResource(R.string.ideas_detail_title),
        onBack = actions.onBack,
        modifier = modifier,
        bottomBar = {
            HealthifyButton(
                text = stringResource(R.string.ideas_register),
                onClick = actions.onRegisterIdea,
                loading = state.isRegistering,
                enabled = idea.canBeLogged,
                modifier = Modifier.fillMaxWidth(),
            )
        },
    ) {
        HealthifyCard(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.extraLarge, borderColor = scheme.tertiary) {
            Column(modifier = Modifier.padding(dimens.space16), verticalArrangement = Arrangement.spacedBy(dimens.space12)) {
                AiBadge(label = stringResource(R.string.ideas_detail_badge))
                Column(
                    modifier = Modifier.semantics(mergeDescendants = true) {},
                    verticalArrangement = Arrangement.spacedBy(dimens.space4),
                ) {
                    Text(
                        text = idea.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = scheme.onSurface,
                        modifier = Modifier.semantics { heading() },
                    )
                    Text(
                        text = stringResource(R.string.format_kcal, numberText(idea.energyKcal)),
                        style = HealthifyTheme.extendedTypography.metricMedium,
                        color = scheme.onSurface,
                    )
                    Text(
                        text = stringResource(
                            R.string.ideas_macros,
                            numberText(idea.proteinG),
                            numberText(idea.carbG),
                            numberText(idea.fatG),
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant,
                    )
                }
                Section(title = stringResource(R.string.ideas_ingredients)) {
                    idea.ingredients.forEach { ingredient ->
                        Text(
                            text = stringResource(R.string.ideas_ingredient_line, ingredient.name, numberText(ingredient.grams)),
                            style = MaterialTheme.typography.bodyMedium,
                            color = scheme.onSurface,
                        )
                    }
                }
                if (idea.why.isNotBlank()) {
                    // Texto de la IA: ya llega en el idioma del lector.
                    Section(title = stringResource(R.string.ideas_why)) {
                        Text(text = idea.why, style = MaterialTheme.typography.bodyMedium, color = scheme.onSurface)
                    }
                }
            }
        }
        val excluded = idea.excludedIngredients
        if (excluded.isNotEmpty()) {
            Text(
                text = stringResource(R.string.ideas_excluded_notice, excluded.joinToString { it.name }),
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant,
            )
        }
        Disclaimer()
    }
}

@Composable
private fun Section(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(HealthifyTheme.dimens.space8)) {
        Text(
            text = title,
            style = HealthifyTheme.extendedTypography.overlineSection,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.semantics { heading() },
        )
        content()
    }
}

@Composable
private fun Disclaimer() {
    Text(
        text = stringResource(R.string.ideas_disclaimer),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun IdeasScaffold(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    bottomBar: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val dimens = HealthifyTheme.dimens
    SystemBarsAppearance(darkBackground = false)
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .navigationBarsPadding(),
    ) {
        HealthifyTopAppBar(title = title, onBack = onBack)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(start = dimens.space16, top = dimens.space16, end = dimens.space16, bottom = dimens.space40),
            verticalArrangement = Arrangement.spacedBy(dimens.space24),
            content = content,
        )
        if (bottomBar != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(dimens.space16),
            ) { bottomBar() }
        }
    }
}

private const val EMPTY_HEIGHT_FACTOR = 6

private val PreviewIdeas = MealIdeas(
    localDate = LocalDate.parse("2026-09-09"),
    remainingKcal = 590.0,
    restrictions = listOf(RestrictionCode.SHELLFISH_FREE),
    ideas = listOf(
        MealIdea(
            id = "a",
            name = "Pollo al horno con camote y ensalada",
            energyKcal = 540.0,
            proteinG = 38.0,
            carbG = 52.0,
            fatG = 14.0,
            ingredients = listOf(
                MealIdeaIngredient("Pechuga de pollo", 150.0, MealFood(231, "Pechuga de pollo")),
                MealIdeaIngredient("Camote", 120.0, MealFood(118, "Camote")),
                MealIdeaIngredient("Lechuga y tomate", 100.0, MealFood(77, "Lechuga")),
                MealIdeaIngredient("Aceite de oliva", 5.0, null),
            ),
            why = "Te faltan 48 g de proteína hoy, cabe en tus 590 kcal y no lleva mariscos.",
        ),
        MealIdea("b", "Tortilla de verduras con pan integral", 420.0, 24.0, 40.0, 16.0, emptyList(), ""),
        MealIdea("c", "Quinua con verduras salteadas y huevo", 480.0, 22.0, 55.0, 15.0, emptyList(), ""),
    ),
)

@Preview(name = "PT14.4 · Ideas para hoy", widthDp = 360, heightDp = 800)
@Composable
private fun MealIdeasPreview() {
    HealthifyTheme { MealIdeasContent(MealIdeasUiState(isLoading = false, ideas = PreviewIdeas), MealIdeasActions()) }
}

@Preview(name = "PT14.5 · Idea — detalle", widthDp = 360, heightDp = 800)
@Composable
private fun MealIdeaDetailPreview() {
    HealthifyTheme {
        MealIdeasContent(MealIdeasUiState(isLoading = false, ideas = PreviewIdeas, selectedIdeaId = "a"), MealIdeasActions())
    }
}

@Preview(name = "PT14.4.E · no se pudieron preparar", widthDp = 360, heightDp = 800)
@Composable
private fun MealIdeasErrorPreview() {
    HealthifyTheme {
        MealIdeasContent(MealIdeasUiState(isLoading = false, failure = MealIdeasFailure.GENERIC), MealIdeasActions())
    }
}

@Preview(name = "PT14.4.O · sin conexión", widthDp = 360, heightDp = 800)
@Composable
private fun MealIdeasOfflinePreview() {
    HealthifyTheme { MealIdeasContent(MealIdeasUiState(isLoading = false, isOffline = true), MealIdeasActions()) }
}

@Preview(name = "PT14.4 · cargando", widthDp = 360, heightDp = 800)
@Composable
private fun MealIdeasLoadingPreview() {
    HealthifyTheme { MealIdeasContent(MealIdeasUiState(), MealIdeasActions()) }
}
