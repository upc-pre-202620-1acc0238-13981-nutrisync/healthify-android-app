package pe.edu.upc.healthify.features.nutritionalcare.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
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
import pe.edu.upc.healthify.features.nutritionalcare.application.usecase.GetConsultationInProgressUseCase
import pe.edu.upc.healthify.features.nutritionalcare.application.usecase.PrescribeConsultationTargetsUseCase
import pe.edu.upc.healthify.features.nutritionalcare.application.usecase.ProposeConsultationTargetsUseCase
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.Consultation
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.OverrideField
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.OverrideValidation
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.TargetInputs
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.TargetParameters
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.TargetPrescription
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.TargetProposal
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.validateOverride
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.DecimalText
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.DeficitKind
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.EnergyEquation
import pe.edu.upc.healthify.features.nutritionalcare.presentation.navigation.PatientArgs
import pe.edu.upc.healthify.features.nutritionalcare.presentation.state.OverrideForm
import pe.edu.upc.healthify.features.nutritionalcare.presentation.state.ParametersForm
import pe.edu.upc.healthify.features.nutritionalcare.presentation.state.SaveFailure
import pe.edu.upc.healthify.features.nutritionalcare.presentation.state.TargetsUiState
import javax.inject.Inject

/**
 * EV-4 · Paso 3 Metas calculadas. Se calculan solas con los datos base y la medición (parámetros por defecto, NC-5).
 * «Cambiar parámetros» recalcula el mismo borrador; «Aceptar metas» las prescribe tal cual; «Escribir mis propios
 * valores» exige una razón (`OverrideReasonRequired`; DECISIÓN NC-5 §12: el campo de razón se agrega a la pantalla).
 * Al reanudar con metas ya calculadas no se recalcula.
 */
@HiltViewModel
class TargetsStepViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getConsultationInProgress: GetConsultationInProgressUseCase,
    private val proposeTargets: ProposeConsultationTargetsUseCase,
    private val prescribeTargets: PrescribeConsultationTargetsUseCase,
    private val connectivityObserver: ConnectivityObserver,
) : ViewModel() {

    val patientId: Long = checkNotNull(savedStateHandle[PatientArgs.PATIENT_ID])
    val patientName: String = savedStateHandle.get<String>(PatientArgs.PATIENT_NAME).orEmpty()

    private val _state = MutableStateFlow(TargetsUiState())
    val state: StateFlow<TargetsUiState> = _state.asStateFlow()

    private val _events = Channel<ConsultationStepEvent>(Channel.BUFFERED)
    val events: Flow<ConsultationStepEvent> = _events.receiveAsFlow()

    private var consultation: Consultation? = null

    /** Lo último que falló (recalcular o prescribir), para «Reintentar». */
    private var lastAttempt: (() -> Unit)? = null

    init {
        viewModelScope.launch {
            connectivityObserver.isOnline.collect { online ->
                _state.update { it.copy(load = it.load.copy(isOffline = !online)) }
            }
        }
        load()
    }

    fun onRetry() {
        if (consultation == null) load() else calculate(parameters = null)
    }

    // ----- «Cambiar parámetros» -----

    fun onChangeParameters() {
        val basis = _state.value.proposal?.basis
        _state.update {
            it.copy(
                showParameters = true,
                parametersInvalid = false,
                parameters = it.parameters ?: ParametersForm(
                    equation = basis?.equation ?: EnergyEquation.MIFFLIN_ST_JEOR,
                    deficitKind = basis?.deficitKind ?: DeficitKind.FIXED_KCAL,
                    deficit = basis?.deficitValue?.let(formFormat::format) ?: DEFAULT_DEFICIT,
                    proteinPerKg = DEFAULT_PROTEIN_PER_KG,
                    fatPercent = DEFAULT_FAT_PERCENT,
                ),
            )
        }
    }

    fun onParametersChange(form: ParametersForm) = _state.update { it.copy(parameters = form, parametersInvalid = false) }

    fun onParametersDismissed() = _state.update { it.copy(showParameters = false) }

    fun onApplyParameters() {
        val form = _state.value.parameters ?: return
        val parameters = form.toParameters() ?: run {
            _state.update { it.copy(parametersInvalid = true) }
            return
        }
        _state.update { it.copy(showParameters = false) }
        calculate(parameters)
    }

    // ----- «Aceptar metas» -----

    fun onAccept() {
        val current = consultation ?: return
        val saved = current.targets
        if (saved != null && saved.prescribed != null && !saved.isOverridden && saved.proposal == _state.value.proposal?.proposal) {
            _events.trySend(ConsultationStepEvent.Continue)
            return
        }
        prescribe(TargetPrescription.AcceptedAsProposed)
    }

    // ----- «Escribir mis propios valores» -----

    fun onWriteOwnValues() {
        val proposal = _state.value.proposal?.proposal
        _state.update {
            it.copy(
                showOverride = true,
                overrideProblems = emptySet(),
                override = if (it.override == OverrideForm() && proposal != null) {
                    OverrideForm(
                        energy = formatWhole(proposal.energyKcal),
                        protein = formatWhole(proposal.proteinG),
                        carb = formatWhole(proposal.carbG),
                        fat = formatWhole(proposal.fatG),
                    )
                } else {
                    it.override
                },
            )
        }
    }

    fun onOverrideChange(form: OverrideForm) = _state.update {
        it.copy(override = form, overrideProblems = it.overrideProblems.filterNot { field -> field.isFilledIn(form) }.toSet())
    }

    fun onOverrideDismissed() = _state.update { it.copy(showOverride = false) }

    fun onSaveOverride() {
        val form = _state.value.override
        when (val validation = validateOverride(form.energy, form.protein, form.carb, form.fat, form.reason)) {
            is OverrideValidation.Invalid -> _state.update { it.copy(overrideProblems = validation.problems) }
            is OverrideValidation.Valid -> prescribe(validation.prescription)
        }
    }

    fun onSaveFailureDismissed() = _state.update { it.copy(saveFailure = null) }

    fun onRetrySave() {
        _state.update { it.copy(saveFailure = null) }
        lastAttempt?.invoke()
    }

    private fun prescribe(prescription: TargetPrescription) {
        val current = consultation ?: return
        if (_state.value.isSaving) return
        lastAttempt = { prescribe(prescription) }
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true) }
            val result = prescribeTargets(current.id, prescription)
            _state.update { it.copy(isSaving = false) }
            val updated = result.getOrNull()
            val error = result.domainErrorOrNull()
            when {
                updated != null -> {
                    consultation = updated
                    _state.update { it.copy(showOverride = false) }
                    _events.send(ConsultationStepEvent.Continue)
                }
                error.closesConsultation() -> _events.send(ConsultationStepEvent.ConsultationClosed)
                error == DomainError.Validation(OVERRIDE_REASON_REQUIRED) ->
                    _state.update { it.copy(showOverride = true, overrideProblems = setOf(OverrideField.REASON)) }
                error == DomainError.Validation(STEP_OUT_OF_ORDER) -> _events.send(ConsultationStepEvent.Back)
                else -> _state.update { it.copy(saveFailure = SaveFailure(isNetwork = error == DomainError.Network)) }
            }
        }
    }

    private fun calculate(parameters: TargetParameters?) {
        val current = consultation ?: return
        lastAttempt = { calculate(parameters) }
        viewModelScope.launch {
            _state.update { it.copy(isCalculating = true, proposalFailed = false) }
            val result = proposeTargets(current.id, parameters)
            val error = result.domainErrorOrNull()
            _state.update {
                it.copy(
                    isCalculating = false,
                    proposal = result.getOrNull() ?: it.proposal,
                    proposalFailed = result.isFailure && it.proposal == null,
                    saveFailure = if (result.isFailure && it.proposal != null && !error.closesConsultation()) {
                        SaveFailure(isNetwork = error == DomainError.Network)
                    } else {
                        it.saveFailure
                    },
                )
            }
            when {
                error.closesConsultation() -> _events.send(ConsultationStepEvent.ConsultationClosed)
                error == DomainError.Validation(STEP_OUT_OF_ORDER) -> _events.send(ConsultationStepEvent.Back)
            }
        }
    }

    private fun load() {
        viewModelScope.launch {
            _state.update { it.copy(load = it.load.copy(isLoading = true, loadFailed = false)) }
            val result = getConsultationInProgress(patientId)
            if (result.isFailure) {
                _state.update {
                    it.copy(
                        load = it.load.copy(
                            isLoading = false,
                            loadFailed = true,
                            isOffline = it.load.isOffline || result.domainErrorOrNull() == DomainError.Network,
                        ),
                    )
                }
                return@launch
            }
            val inProgress = result.getOrNull() ?: run {
                _events.send(ConsultationStepEvent.ConsultationClosed)
                return@launch
            }
            val measurement = inProgress.measurement
            if (measurement == null || inProgress.diagnosis == null) {
                _events.send(ConsultationStepEvent.Back)
                return@launch
            }
            consultation = inProgress
            val saved = inProgress.targets
            _state.update { it.copy(load = it.load.copy(isLoading = false, loadFailed = false)) }
            if (saved != null) {
                // Reanudar: se muestran las metas ya calculadas (el borrador prescrito no se puede recalcular).
                val inputs = TargetInputs(
                    measurement.sex,
                    measurement.ageYears,
                    measurement.heightCm,
                    measurement.weightKg,
                    measurement.activityLevel,
                )
                _state.update { it.copy(proposal = TargetProposal(saved.basis, saved.proposal, inputs)) }
            } else {
                calculate(parameters = null)
            }
        }
    }

    private fun ParametersForm.toParameters(): TargetParameters? {
        val deficitValue = DecimalText.parse(deficit) ?: return null
        val protein = DecimalText.parse(proteinPerKg) ?: return null
        val fat = DecimalText.parse(fatPercent) ?: return null
        val valid = deficitValue in 0.0..deficitKind.maximum && protein in TargetParameters.PROTEIN_RANGE &&
            fat in TargetParameters.FAT_RANGE
        return if (valid) TargetParameters(equation, deficitKind, deficitValue, protein, fat) else null
    }

    private fun OverrideField.isFilledIn(form: OverrideForm): Boolean = when (this) {
        OverrideField.ENERGY -> form.energy.isNotBlank()
        OverrideField.PROTEIN -> form.protein.isNotBlank()
        OverrideField.CARB -> form.carb.isNotBlank()
        OverrideField.FAT -> form.fat.isNotBlank()
        OverrideField.REASON -> form.reason.isNotBlank()
    }

    private fun formatWhole(value: Double): String = Math.round(value).toString()

    private companion object {
        const val OVERRIDE_REASON_REQUIRED = "OverrideReasonRequired"
        const val STEP_OUT_OF_ORDER = "ConsultationStepOutOfOrder"

        /** `NutritionalCare:DefaultTargetParameters` del backend (NC-5). */
        const val DEFAULT_DEFICIT = "500"
        const val DEFAULT_PROTEIN_PER_KG = "1.6"
        const val DEFAULT_FAT_PERCENT = "30"
    }
}
