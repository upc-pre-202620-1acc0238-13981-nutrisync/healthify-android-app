package pe.edu.upc.healthify.features.foodcatalog.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pe.edu.upc.healthify.R
import pe.edu.upc.healthify.core.designsystem.component.EmptyState
import pe.edu.upc.healthify.core.designsystem.component.ErrorState
import pe.edu.upc.healthify.core.designsystem.component.HealthifyButton
import pe.edu.upc.healthify.core.designsystem.component.HealthifyButtonStyle
import pe.edu.upc.healthify.core.designsystem.component.HealthifyCard
import pe.edu.upc.healthify.core.designsystem.component.HealthifySnackbarHost
import pe.edu.upc.healthify.core.designsystem.component.HealthifyTextField
import pe.edu.upc.healthify.core.designsystem.component.HealthifyTopAppBar
import pe.edu.upc.healthify.core.designsystem.component.OfflineBanner
import pe.edu.upc.healthify.core.designsystem.component.OfflineBannerType
import pe.edu.upc.healthify.core.designsystem.component.SkeletonListItem
import pe.edu.upc.healthify.core.designsystem.component.SystemBarsAppearance
import pe.edu.upc.healthify.core.designsystem.component.UiText
import pe.edu.upc.healthify.core.designsystem.component.asString
import pe.edu.upc.healthify.core.designsystem.icon.HealthifyIcons
import pe.edu.upc.healthify.core.designsystem.theme.HealthifyTheme
import pe.edu.upc.healthify.features.foodcatalog.presentation.state.FoodCatalogUiState
import pe.edu.upc.healthify.features.foodcatalog.presentation.state.FoodRow
import pe.edu.upc.healthify.features.foodcatalog.presentation.viewmodel.FoodCatalogViewModel

/**
 * PR15 · Catálogo de alimentos (desde PR20). Soporte técnico del registro del paciente: «Agregar alimento local» abre
 * PR15.1. La gestión no funciona sin conexión.
 *
 * @param addedFoodName el alimento que PR15.1 acaba de crear: se busca y se muestra el Snackbar.
 */
@Composable
fun FoodCatalogScreen(
    onBack: () -> Unit,
    onAddLocalFood: () -> Unit,
    addedFoodName: String?,
    onAddedFoodHandled: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FoodCatalogViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val addedTemplate = stringResource(R.string.food_catalog_added)
    LaunchedEffect(addedFoodName) {
        val name = addedFoodName ?: return@LaunchedEffect
        onAddedFoodHandled()
        viewModel.onFoodAdded(name)
        snackbarHostState.showSnackbar(addedTemplate.format(name))
    }
    FoodCatalogContent(
        state = state,
        onBack = onBack,
        onQueryChange = viewModel::onQueryChange,
        onAddLocalFood = onAddLocalFood,
        onRetry = viewModel::onRetry,
        snackbarHostState = snackbarHostState,
        modifier = modifier,
    )
}

@Composable
fun FoodCatalogContent(
    state: FoodCatalogUiState,
    onBack: () -> Unit,
    onQueryChange: (String) -> Unit,
    onAddLocalFood: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    val dimens = HealthifyTheme.dimens
    val focusManager = LocalFocusManager.current
    SystemBarsAppearance(darkBackground = false)
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .navigationBarsPadding(),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            HealthifyTopAppBar(title = stringResource(R.string.food_catalog_title), onBack = onBack)
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(dimens.space16),
                verticalArrangement = Arrangement.spacedBy(dimens.space12),
            ) {
                if (state.isOffline) item(key = "offline") { OfflineBanner(type = OfflineBannerType.RequiresNetwork) }
                item(key = "search") {
                    HealthifyTextField(
                        value = state.query,
                        onValueChange = onQueryChange,
                        label = stringResource(R.string.food_catalog_search),
                        placeholder = stringResource(R.string.food_catalog_search_placeholder),
                        imeAction = ImeAction.Search,
                        keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                        trailingIcon = {
                            Icon(HealthifyIcons.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                val results = state.results
                when {
                    state.needsConnection -> item(key = "needs-connection") {
                        EmptyState(
                            title = stringResource(R.string.practitioner_offline_title),
                            text = stringResource(R.string.food_catalog_offline_body),
                            actionLabel = stringResource(R.string.ds_retry),
                            onAction = onRetry,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    state.isLoading && results == null -> items(SKELETON_ROWS) { SkeletonListItem(modifier = Modifier.fillMaxWidth()) }
                    state.loadFailed && results == null -> item(key = "error") {
                        ErrorState(onRetry = onRetry, modifier = Modifier.fillMaxWidth())
                    }
                    state.isEmpty -> item(key = "empty") {
                        EmptyState(
                            title = stringResource(R.string.food_catalog_empty_title),
                            text = stringResource(R.string.food_catalog_empty_body),
                            actionLabel = stringResource(R.string.food_catalog_add_local),
                            onAction = onAddLocalFood,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    results != null -> {
                        items(results, key = { it.id }) { food -> FoodCard(food) }
                        item(key = "add") {
                            HealthifyButton(
                                text = stringResource(R.string.food_catalog_add_local),
                                onClick = onAddLocalFood,
                                style = HealthifyButtonStyle.Tonal,
                                enabled = !state.isOffline,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
            }
        }
        HealthifySnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(dimens.space16),
        )
    }
}

@Composable
private fun FoodCard(food: FoodRow) {
    val dimens = HealthifyTheme.dimens
    val scheme = MaterialTheme.colorScheme
    HealthifyCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(dimens.space16),
            verticalArrangement = Arrangement.spacedBy(dimens.space4),
        ) {
            // El nombre local está sembrado en español (X-2 pendiente del backend): se muestra tal cual.
            Text(
                text = if (food.isLocal) stringResource(R.string.food_catalog_local_name, food.name) else food.name,
                style = MaterialTheme.typography.titleMedium,
                color = scheme.onSurface,
            )
            Text(
                text = UiText.of(R.string.food_catalog_energy, food.energyKcalPer100g).asString(),
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant,
            )
        }
    }
}

private const val SKELETON_ROWS = 4

private val previewFoods = listOf(
    FoodRow(1, "Papa amarilla", 97.0, isLocal = false),
    FoodRow(2, "Cuy al horno", 176.0, isLocal = true),
)

@Preview(name = "PR15 · Catálogo", widthDp = 360, heightDp = 800)
@Composable
private fun FoodCatalogPreview() {
    HealthifyTheme {
        FoodCatalogContent(
            state = FoodCatalogUiState(isLoading = false, results = previewFoods),
            onBack = {},
            onQueryChange = {},
            onAddLocalFood = {},
            onRetry = {},
        )
    }
}

@Preview(name = "PR15 · Sin resultados", widthDp = 360, heightDp = 800)
@Composable
private fun FoodCatalogEmptyPreview() {
    HealthifyTheme {
        FoodCatalogContent(
            state = FoodCatalogUiState(query = "chaufa de cuy", isLoading = false, results = emptyList()),
            onBack = {},
            onQueryChange = {},
            onAddLocalFood = {},
            onRetry = {},
        )
    }
}

@Preview(name = "PR15 · Cargando", widthDp = 360, heightDp = 800)
@Composable
private fun FoodCatalogLoadingPreview() {
    HealthifyTheme {
        FoodCatalogContent(state = FoodCatalogUiState(), onBack = {}, onQueryChange = {}, onAddLocalFood = {}, onRetry = {})
    }
}

@Preview(name = "PR15 · Sin conexión", widthDp = 360, heightDp = 800)
@Composable
private fun FoodCatalogOfflinePreview() {
    HealthifyTheme {
        FoodCatalogContent(
            state = FoodCatalogUiState(isLoading = false, isOffline = true, loadFailed = true),
            onBack = {},
            onQueryChange = {},
            onAddLocalFood = {},
            onRetry = {},
        )
    }
}

@Preview(name = "PR15 · Error", widthDp = 360, heightDp = 800)
@Composable
private fun FoodCatalogErrorPreview() {
    HealthifyTheme {
        FoodCatalogContent(
            state = FoodCatalogUiState(isLoading = false, loadFailed = true),
            onBack = {},
            onQueryChange = {},
            onAddLocalFood = {},
            onRetry = {},
        )
    }
}
