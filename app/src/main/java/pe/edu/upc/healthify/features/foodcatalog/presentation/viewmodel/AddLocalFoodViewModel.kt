package pe.edu.upc.healthify.features.foodcatalog.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pe.edu.upc.healthify.core.common.connectivity.ConnectivityObserver
import pe.edu.upc.healthify.core.common.error.DomainError
import pe.edu.upc.healthify.core.common.error.domainErrorOrNull
import pe.edu.upc.healthify.features.foodcatalog.application.usecase.CreateLocalFoodUseCase
import pe.edu.upc.healthify.features.foodcatalog.domain.entity.LocalFoodField
import pe.edu.upc.healthify.features.foodcatalog.domain.entity.LocalFoodValidation
import pe.edu.upc.healthify.features.foodcatalog.domain.entity.NewLocalFood
import pe.edu.upc.healthify.features.foodcatalog.presentation.state.AddLocalFoodUiState
import javax.inject.Inject

sealed interface AddLocalFoodEvent {
    data class Added(val name: String) : AddLocalFoodEvent
}

/**
 * PR15.1 · Agregar alimento local (platos peruanos que el catálogo externo no trae). Un alimento local nunca se
 * sobrescribe con una importación. Es soporte técnico del registro del paciente, no un dato clínico.
 */
@HiltViewModel
class AddLocalFoodViewModel @Inject constructor(
    private val createLocalFood: CreateLocalFoodUseCase,
    private val connectivityObserver: ConnectivityObserver,
) : ViewModel() {

    private val _state = MutableStateFlow(AddLocalFoodUiState())
    val state: StateFlow<AddLocalFoodUiState> = _state.asStateFlow()

    private val _events = Channel<AddLocalFoodEvent>(Channel.BUFFERED)
    val events: Flow<AddLocalFoodEvent> = _events.receiveAsFlow()

    init {
        viewModelScope.launch {
            connectivityObserver.isOnline.collect { online -> _state.update { it.copy(isOffline = !online) } }
        }
    }

    fun onNameChange(value: String) = _state.update {
        it.copy(
            name = value.take(NewLocalFood.MAX_NAME_LENGTH),
            invalidFields = it.invalidFields - LocalFoodField.NAME,
            duplicatedName = false,
        )
    }

    fun onEnergyChange(value: String) = updateNumber(LocalFoodField.ENERGY) { it.copy(energy = value) }

    fun onProteinChange(value: String) = updateNumber(LocalFoodField.PROTEIN) { it.copy(protein = value) }

    fun onCarbChange(value: String) = updateNumber(LocalFoodField.CARB) { it.copy(carb = value) }

    fun onFatChange(value: String) = updateNumber(LocalFoodField.FAT) { it.copy(fat = value) }

    fun onServerErrorDismissed() = _state.update { it.copy(showServerError = false) }

    /** «Guardar alimento» (o «Reintentar»). */
    fun onSave() {
        val current = _state.value
        if (current.isSaving) return
        val food = when (
            val validation = LocalFoodValidation.of(current.name, current.energy, current.protein, current.carb, current.fat)
        ) {
            is LocalFoodValidation.Invalid -> {
                _state.update { it.copy(invalidFields = validation.fields) }
                return
            }
            is LocalFoodValidation.Valid -> validation.food
        }
        _state.update { it.copy(isSaving = true, showServerError = false, invalidFields = emptySet()) }
        viewModelScope.launch {
            val result = createLocalFood(food)
            _state.update { it.copy(isSaving = false) }
            val error = result.domainErrorOrNull()
            when {
                result.isSuccess -> _events.send(AddLocalFoodEvent.Added(result.getOrThrow().name))
                error is DomainError.Conflict -> _state.update { it.copy(duplicatedName = true) }
                error is DomainError.Validation && error.code == LOCAL_NAME_AND_NUTRIENTS_REQUIRED ->
                    _state.update { it.copy(invalidFields = LocalFoodField.entries.toSet()) }
                else -> _state.update { it.copy(showServerError = true) }
            }
        }
    }

    private fun updateNumber(field: LocalFoodField, transform: (AddLocalFoodUiState) -> AddLocalFoodUiState) =
        _state.update { transform(it).copy(invalidFields = it.invalidFields - field) }

    private companion object {
        const val LOCAL_NAME_AND_NUTRIENTS_REQUIRED = "LocalNameAndNutrientsRequired"
    }
}
