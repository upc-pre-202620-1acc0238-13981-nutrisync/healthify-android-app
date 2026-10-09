package pe.edu.upc.healthify.features.monitoring.domain.entity

import pe.edu.upc.healthify.features.monitoring.domain.valueobject.ConsultationModality
import pe.edu.upc.healthify.features.monitoring.domain.valueobject.PreparationInstruction
import java.time.Instant

/**
 * Próxima consulta del paciente (`PatientFollowUpResource`, MA-3; `UpcomingConsultationResource`, RM-5). Solo
 * lectura: agendarla o moverla lo hace el nutricionista. No expone notas clínicas ni diagnóstico.
 *
 * @param scheduledAt cuándo la agendó el nutricionista («Agendada el 4 de septiembre»), si el backend lo sabe.
 * @param practitionerFullName nombre del nutricionista tal cual (texto de una persona).
 */
data class NextFollowUp(
    val id: FollowUpId,
    val scheduledFor: Instant,
    val modality: ConsultationModality = ConsultationModality.IN_PERSON,
    val preparation: List<PreparationInstruction> = emptyList(),
    val scheduledAt: Instant? = null,
    val practitionerFullName: String? = null,
) {
    /**
     * MA-4 «Check In Editable Until The Visit»: el check-in se envía o edita solo antes de la hora de la consulta.
     * El backend es quien decide (`409 CheckInLocked`); esto evita ofrecer lo que ya no se puede.
     */
    fun acceptsCheckInAt(now: Instant): Boolean = now.isBefore(scheduledFor)
}

/** Id de una consulta agendada (`followUpId`). */
@JvmInline
value class FollowUpId(val value: Long) {
    init {
        require(value > 0) { "FollowUpId must be positive" }
    }
}
