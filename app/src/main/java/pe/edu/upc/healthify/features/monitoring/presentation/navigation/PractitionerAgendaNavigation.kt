package pe.edu.upc.healthify.features.monitoring.presentation.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable
import pe.edu.upc.healthify.features.monitoring.presentation.screen.RecordReferralScreen
import pe.edu.upc.healthify.features.monitoring.presentation.screen.ScheduleFollowUpScreen
import java.time.Instant

/**
 * PR17 · Agendar consulta. Desde la ficha del paciente (PAC-1) llega con el paciente fijo ([patientId] > 0); desde la
 * Agenda sin paciente (se elige de la cartera). Con [followUpId] > 0 reprograma esa consulta (paciente fijo, fecha y
 * preparación prellenadas: [scheduledForEpochSecond] y [preparation] con los códigos separados por coma).
 */
@Serializable
data class ScheduleFollowUpRoute(
    val patientId: Long = 0,
    val patientName: String = "",
    val followUpId: Long = 0,
    val scheduledForEpochSecond: Long = 0,
    val preparation: String = "",
) {
    companion object {
        const val ARG_PATIENT_ID = "patientId"
        const val ARG_PATIENT_NAME = "patientName"
        const val ARG_FOLLOW_UP_ID = "followUpId"
        const val ARG_SCHEDULED_FOR = "scheduledForEpochSecond"
        const val ARG_PREPARATION = "preparation"
    }
}

/** PR16 · Registrar derivación (desde PAC-1 «Registrar derivación»). */
@Serializable
data class RecordReferralRoute(val patientId: Long, val patientName: String) {
    companion object {
        const val ARG_PATIENT_ID = "patientId"
    }
}

data class PractitionerAgendaNavigationCallbacks(
    val onBack: () -> Unit,
    /** PR17 agendado o reprogramado: vuelve a donde se abrió con el Snackbar (PR17.0-S o la ficha). */
    val onScheduled: (scheduledFor: Instant, rescheduled: Boolean) -> Unit,
    /** PR16 registrado: vuelve a la ficha con PAC-1-S3 «Derivación … registrada». */
    val onReferralRecorded: (specialty: String) -> Unit,
)

/** PR16 y PR17 en el grafo raíz. */
fun NavGraphBuilder.practitionerAgendaDestinations(callbacks: PractitionerAgendaNavigationCallbacks) {
    composable<ScheduleFollowUpRoute> {
        ScheduleFollowUpScreen(onBack = callbacks.onBack, onScheduled = callbacks.onScheduled)
    }
    composable<RecordReferralRoute> {
        RecordReferralScreen(onBack = callbacks.onBack, onRecorded = callbacks.onReferralRecorded)
    }
}
