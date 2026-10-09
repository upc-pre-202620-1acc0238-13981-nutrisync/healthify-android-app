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
 * Argumentos que los ViewModels del lado del nutricionista leen del `SavedStateHandle` **por nombre de propiedad**.
 * Las pestañas de la ficha del paciente (PAC-1 a PAC-4) viven en la ruta de `features/main`
 * (`PractitionerPatientRoute`), que usa estos mismos nombres.
 */
object PatientArgs {
    const val PATIENT_ID = "patientId"
    const val PATIENT_NAME = "patientName"
    const val CARE_LINK_ID = "careLinkId"
    const val EDITING = "editing"
}

/**
 * EV-1 · Datos base. [editing] `true` desde «Editar» (Resumen o EV-2): «Guardar y salir» vuelve a donde se abrió y no
 * ofrece iniciar la consulta.
 */
@Serializable
data class BaselineRoute(val patientId: Long, val patientName: String, val editing: Boolean = false)

/** EV-2 · Paso 1 Medición de hoy. */
@Serializable
data class ConsultationMeasurementRoute(val patientId: Long, val patientName: String)

/** EV-3 · Paso 2 Diagnóstico. */
@Serializable
data class ConsultationDiagnosisRoute(val patientId: Long, val patientName: String)

/** EV-4 · Paso 3 Metas. */
@Serializable
data class ConsultationTargetsRoute(val patientId: Long, val patientName: String)

/** EV-5 · Paso 4 Indicaciones y publicación (+ EV-5.E). */
@Serializable
data class ConsultationPublicationRoute(val patientId: Long, val patientName: String)

/** La ruta del paso [step] de la consulta de [patientId]. */
fun consultationStepRoute(step: ConsultationStep, patientId: Long, patientName: String): Any = when (step) {
    ConsultationStep.MEASUREMENT -> ConsultationMeasurementRoute(patientId, patientName)
    ConsultationStep.DIAGNOSIS -> ConsultationDiagnosisRoute(patientId, patientName)
    ConsultationStep.TARGETS -> ConsultationTargetsRoute(patientId, patientName)
    ConsultationStep.PUBLICATION -> ConsultationPublicationRoute(patientId, patientName)
}

/**
 * Callbacks de EV-1 a EV-5. El grafo raíz decide el back stack: entre pasos «‹» vuelve al anterior (no borra lo
 * guardado) y salir de la consulta vuelve a la ficha del paciente, que muestra PAC-1.C.
 */
data class ConsultationNavigationCallbacks(
    val onBack: () -> Unit,
    /** EV-1 «Guardar e iniciar consulta»: reemplaza EV-1 por el paso en que quedó la consulta. */
    val onBaselineSavedStartConsultation: (route: Any) -> Unit,
    /** Avanzar al paso siguiente (se apila). */
    val onOpenStep: (route: Any) -> Unit,
    /** «‹» en EV-3, EV-4 o EV-5: al paso anterior (si no está debajo, lo pone en lugar del actual). */
    val onPreviousStep: (previousRoute: Any) -> Unit,
    /** EV-2.S «Salir y continuar después»: vuelve a la ficha del paciente. */
    val onExitConsultation: () -> Unit,
    /** EV-2 «Editar» datos base. */
    val onEditBaseline: (route: Any) -> Unit,
    /** EV-5 publicado: vuelve a la ficha y muestra PAC-1-S1 «Plan publicado». */
    val onPublished: () -> Unit,
)

/** EV-1 a EV-5 en el grafo raíz (pantallas completas sobre la ficha del paciente). */
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
