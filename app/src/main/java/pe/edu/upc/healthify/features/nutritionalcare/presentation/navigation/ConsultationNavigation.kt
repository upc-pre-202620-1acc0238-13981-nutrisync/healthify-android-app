package pe.edu.upc.healthify.features.nutritionalcare.presentation.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.ConsultationStep
import pe.edu.upc.healthify.features.nutritionalcare.presentation.screen.BaselineScreen
import pe.edu.upc.healthify.features.nutritionalcare.presentation.screen.DiagnosisStepScreen
import pe.edu.upc.healthify.features.nutritionalcare.presentation.screen.MeasurementStepScreen
import pe.edu.upc.healthify.features.nutritionalcare.presentation.screen.PublicationStepScreen
import pe.edu.upc.healthify.features.nutritionalcare.presentation.screen.TargetsStepScreen

/**
 * Arguments the practitioner-side ViewModels read from `SavedStateHandle` **by property name**.
 * The patient profile tabs live in the `features/main` route
 * (`PractitionerPatientRoute`), which uses these same names.
 */
object PatientArgs {
    const val PATIENT_ID = "patientId"
    const val PATIENT_NAME = "patientName"
    const val CARE_LINK_ID = "careLinkId"
    const val EDITING = "editing"
}

/**
 * Baseline. [editing] `true` when opened from "Edit" (Summary or measurement step): "Save and exit" goes back to
 * where it was opened and does not offer to start the consultation.
 */
@Serializable
data class BaselineRoute(val patientId: Long, val patientName: String, val editing: Boolean = false)

/** Step 1, today's measurement. */
@Serializable
data class ConsultationMeasurementRoute(val patientId: Long, val patientName: String)

/** Step 2, diagnosis. */
@Serializable
data class ConsultationDiagnosisRoute(val patientId: Long, val patientName: String)

/** Step 3, targets. */
@Serializable
data class ConsultationTargetsRoute(val patientId: Long, val patientName: String)

/** Step 4, guidelines and publication (including the publish error). */
@Serializable
data class ConsultationPublicationRoute(val patientId: Long, val patientName: String)

/** The route of step [step] of [patientId]'s consultation. */
fun consultationStepRoute(step: ConsultationStep, patientId: Long, patientName: String): Any = when (step) {
    ConsultationStep.MEASUREMENT -> ConsultationMeasurementRoute(patientId, patientName)
    ConsultationStep.DIAGNOSIS -> ConsultationDiagnosisRoute(patientId, patientName)
    ConsultationStep.TARGETS -> ConsultationTargetsRoute(patientId, patientName)
    ConsultationStep.PUBLICATION -> ConsultationPublicationRoute(patientId, patientName)
}

/**
 * Callbacks for the baseline and the consultation steps. The root graph owns the back stack: between steps "‹" goes
 * back to the previous one (keeping what was saved) and leaving returns to the patient profile, showing it in progress.
 */
data class ConsultationNavigationCallbacks(
    val onBack: () -> Unit,
    /** Baseline "Save and start consultation": replaces the baseline with the step the consultation is on. */
    val onBaselineSavedStartConsultation: (route: Any) -> Unit,
    /** Go to the next step (pushed). */
    val onOpenStep: (route: Any) -> Unit,
    /** "‹" on steps 2 to 4: to the previous step (if it is not below, it replaces the current one). */
    val onPreviousStep: (previousRoute: Any) -> Unit,
    /** "Exit and continue later": back to the patient profile. */
    val onExitConsultation: () -> Unit,
    /** "Edit" baseline from the measurement step. */
    val onEditBaseline: (route: Any) -> Unit,
    /** Plan published: back to the profile, which shows "Plan published". */
    val onPublished: () -> Unit,
)

/** Baseline and consultation steps in the root graph (full screens over the patient profile). */
fun NavGraphBuilder.consultationDestinations(callbacks: ConsultationNavigationCallbacks) {
    composable<BaselineRoute> {
        BaselineScreen(
            onBack = callbacks.onBack,
            onSaved = callbacks.onBack,
            onStartConsultation = { step, patientId, name ->
                callbacks.onBaselineSavedStartConsultation(consultationStepRoute(step, patientId, name))
            },
        )
    }
    composable<ConsultationMeasurementRoute> {
        MeasurementStepScreen(
            onExit = callbacks.onExitConsultation,
            onContinue = { id, name -> callbacks.onOpenStep(ConsultationDiagnosisRoute(id, name)) },
            onEditBaseline = { id, name -> callbacks.onEditBaseline(BaselineRoute(id, name, editing = true)) },
        )
    }
    composable<ConsultationDiagnosisRoute> {
        DiagnosisStepScreen(
            onBack = { id, name ->
                callbacks.onPreviousStep(ConsultationMeasurementRoute(id, name))
            },
            onContinue = { id, name -> callbacks.onOpenStep(ConsultationTargetsRoute(id, name)) },
            onConsultationClosed = callbacks.onExitConsultation,
        )
    }
    composable<ConsultationTargetsRoute> {
        TargetsStepScreen(
            onBack = { id, name ->
                callbacks.onPreviousStep(ConsultationDiagnosisRoute(id, name))
            },
            onContinue = { id, name -> callbacks.onOpenStep(ConsultationPublicationRoute(id, name)) },
            onConsultationClosed = callbacks.onExitConsultation,
        )
    }
    composable<ConsultationPublicationRoute> {
        PublicationStepScreen(
            onBack = { id, name ->
                callbacks.onPreviousStep(ConsultationTargetsRoute(id, name))
            },
            onPublished = callbacks.onPublished,
            onConsultationClosed = callbacks.onExitConsultation,
        )
    }
}
