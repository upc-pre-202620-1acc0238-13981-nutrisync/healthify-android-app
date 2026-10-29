package pe.edu.upc.healthify.features.nutritionalcare.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pe.edu.upc.healthify.core.common.connectivity.ConnectivityObserver
import pe.edu.upc.healthify.core.common.error.DomainError
import pe.edu.upc.healthify.core.common.error.domainErrorOrNull
import pe.edu.upc.healthify.features.nutritionalcare.application.usecase.GetOpenReviewItemsUseCase
import pe.edu.upc.healthify.features.nutritionalcare.presentation.state.InboxRow
import pe.edu.upc.healthify.features.nutritionalcare.presentation.state.ReviewInboxUiState
import javax.inject.Inject

/**
 * Review inbox. Lists the open items (`?state=Open`); reread on returning to the tab (a resolved item no longer
 * appears) and when the connection returns. Reviewing an item adjusts nothing by itself.
 */
@HiltViewModel
class ReviewInboxViewModel @Inject constructor(
    private val getOpenReviewItems: GetOpenReviewItemsUseCase,
    private val connectivityObserver: ConnectivityObserver,
) : ViewModel() {

    private val _state = MutableStateFlow(ReviewInboxUiState())
    val state: StateFlow<ReviewInboxUiState> = _state.asStateFlow()

    private var loadJob: Job? = null

    init {
        viewModelScope.launch {
            connectivityObserver.isOnline.collect { online ->
                val wasOffline = _state.value.isOffline
                _state.update { it.copy(isOffline = !online) }
                if (online && wasOffline) load()
            }
        }
        load()
    }

    fun onRetry() = load()

    fun onResume() {
        if (_state.value.items != null) load()
    }

    private fun load() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _state.update { it.copy(isLoading = it.items == null, loadFailed = false) }
            val result = getOpenReviewItems()
            _state.update { state ->
                result.getOrNull()?.let { items ->
                    state.copy(isLoading = false, items = items.map(InboxRow::of))
                } ?: state.copy(
                    isLoading = false,
                    loadFailed = state.items == null,
                    isOffline = state.isOffline || result.domainErrorOrNull() == DomainError.Network,
                )
            }
        }
    }
}
