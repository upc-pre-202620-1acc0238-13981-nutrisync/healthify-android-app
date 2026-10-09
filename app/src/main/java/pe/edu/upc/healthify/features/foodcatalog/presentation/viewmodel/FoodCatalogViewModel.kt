package pe.edu.upc.healthify.features.foodcatalog.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pe.edu.upc.healthify.core.common.connectivity.ConnectivityObserver
import pe.edu.upc.healthify.core.common.error.DomainError
import pe.edu.upc.healthify.core.common.error.domainErrorOrNull
import pe.edu.upc.healthify.features.foodcatalog.application.usecase.BrowseFoodCatalogUseCase
import pe.edu.upc.healthify.features.foodcatalog.presentation.state.FoodCatalogUiState
import pe.edu.upc.healthify.features.foodcatalog.presentation.state.FoodRow
import javax.inject.Inject

/**
 * PR15 · Catálogo de alimentos del nutricionista (desde PR20). Sin texto lista el catálogo del servidor (los alimentos
 * locales primero); al escribir busca con una pausa breve. Se relee al volver de PR15.1 y al volver la conexión.
 */
@HiltViewModel
class FoodCatalogViewModel @Inject constructor(
    private val browseFoodCatalog: BrowseFoodCatalogUseCase,
    private val connectivityObserver: ConnectivityObserver,
) : ViewModel() {

    private val _state = MutableStateFlow(FoodCatalogUiState())
    val state: StateFlow<FoodCatalogUiState> = _state.asStateFlow()

    private var searchJob: Job? = null

    init {
        viewModelScope.launch {
            connectivityObserver.isOnline.collect { online ->
                val wasOffline = _state.value.isOffline
                _state.update { it.copy(isOffline = !online) }
                if (online && wasOffline) search(debounce = false)
            }
        }
        search(debounce = false)
    }

    fun onQueryChange(query: String) {
        _state.update { it.copy(query = query) }
        search(debounce = true)
    }

    fun onRetry() = search(debounce = false)

    /** Al volver de PR15.1: muestra el alimento recién creado. */
    fun onFoodAdded(name: String) {
        _state.update { it.copy(query = name) }
        search(debounce = false)
    }

    private fun search(debounce: Boolean) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            if (debounce) delay(SEARCH_DEBOUNCE_MS)
            _state.update { it.copy(isLoading = true, loadFailed = false) }
            val result = browseFoodCatalog(_state.value.query)
            _state.update { state ->
                result.getOrNull()?.let { foods ->
                    state.copy(isLoading = false, results = foods.map(FoodRow::of))
                } ?: state.copy(
                    isLoading = false,
                    loadFailed = true,
                    isOffline = state.isOffline || result.domainErrorOrNull() == DomainError.Network,
                )
            }
        }
    }

    private companion object {
        const val SEARCH_DEBOUNCE_MS = 300L
    }
}
