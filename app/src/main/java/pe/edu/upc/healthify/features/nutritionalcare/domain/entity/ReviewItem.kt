package pe.edu.upc.healthify.features.nutritionalcare.domain.entity

import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.DeviationDirection
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.PatientId
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.ReviewItemId
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.SignalType
import java.time.Instant
import java.time.LocalDate

/**
 * Un ítem de la bandeja de revisión (F27, NC-11): una señal que Monitoring levantó y que **espera una decisión humana**.
 * Nada de lo que hay aquí cambió un plan (*Signal Notifies Never Modifies The Plan*).
 *
 * @param legacyEvidence frase en inglés del backend (`evidence`): solo se muestra si no hay [evidence] estructurada.
 * @param evidence la evidencia en números (`evidenceData`), que la app arma en el idioma del lector (X-2).
 * @param hasPlanProposal la IA adjuntó (o está generando) un plan propuesto: el ítem abre PR14.IA en vez de PR14.
 */
data class ReviewItem(
    val id: ReviewItemId,
    val patientId: PatientId,
    val patientFullName: String?,
    val signalType: SignalType,
    val legacyEvidence: String,
    val evidence: ReviewEvidence?,
    val isOpen: Boolean,
    val resolvedWithAdjustment: Boolean?,
    val resolutionNote: ResolutionNote?,
    val createdAt: Instant?,
    val resolvedAt: Instant?,
    val hasPlanProposal: Boolean,
) {
    /** Solo una desviación sostenida abierta puede tener un plan propuesto por IA (NC-10). */
    val offersPlanProposal: Boolean get() = isOpen && hasPlanProposal && signalType == SignalType.SustainedDeviation
}

/**
 * Evidencia de un ítem (`ReviewItemResource.evidenceData`, X-2). Los días sin registro **nunca cuentan**: el
 * denominador son los días registrados.
 */
sealed interface ReviewEvidence {

    /** «Registró en promedio 40 % menos de su meta de energía (≈ 718 kcal/día) en 5 de sus últimos 9 días registrados». */
    data class SustainedDeviation(
        /** Sin signo: cuánto se alejó en promedio (el sentido va en [direction]). */
        val averagePercentFromTarget: Double,
        val deviatedDays: Int,
        val loggedDaysConsidered: Int,
        val direction: DeviationDirection,
        /** kcal/día sin signo; `null` en ítems anteriores a X-2. */
        val averageEnergyKcalFromTarget: Double?,
    ) : ReviewEvidence {
        init {
            require(averagePercentFromTarget >= 0) { "The percent is unsigned" }
            require(loggedDaysConsidered > 0) { "A deviation is measured over logged days" }
            require(deviatedDays in 1..loggedDaysConsidered) { "Deviated days are a part of the logged days" }
            require(averageEnergyKcalFromTarget == null || averageEnergyKcalFromTarget >= 0) { "The kcal are unsigned" }
        }
    }

    /** «Revisión programada tras el ajuste del 8 sept. (v3): 2 de 6 días registrados fuera de la meta». */
    data class ScheduledRecheck(
        val adjustedOn: LocalDate,
        val adjustedPlanVersion: Int,
        /** `null` (los dos) si no hubo resumen desde el ajuste. */
        val deviatedDays: Int?,
        val loggedDaysConsidered: Int?,
    ) : ReviewEvidence {
        init {
            require(adjustedPlanVersion > 0) { "A plan version is positive" }
            require((deviatedDays == null) == (loggedDaysConsidered == null)) { "Both counts or none" }
            if (deviatedDays != null && loggedDaysConsidered != null) {
                require(loggedDaysConsidered >= 0 && deviatedDays in 0..loggedDaysConsidered) {
                    "Deviated days are a part of the logged days"
                }
            }
        }

        val hasCounts: Boolean get() = deviatedDays != null && loggedDaysConsidered != null
    }

    /**
     * «Índice de consistencia: 0,46 kg/semana sin explicar; en alerta desde el 20 ago. (4 semanas); se le mostró al
     * paciente el 9 sept.». Las partes que falten se omiten.
     */
    data class ConsistencyEscalation(
        val kgPerWeek: Double,
        val alertSinceOn: LocalDate?,
        val weeksInAlert: Int?,
        val shownToPatientOn: LocalDate?,
    ) : ReviewEvidence {
        init {
            require(kgPerWeek >= 0) { "The index is unsigned" }
            require(weeksInAlert == null || weeksInAlert >= 0) { "Weeks in alert cannot be negative" }
        }
    }
}

/**
 * Nota con la que se resolvió un ítem (`resolutionNoteCode` + `resolutionNoteData`, X-2). Una nota escrita por el
 * profesional ([Custom]) se muestra tal cual y nunca se traduce.
 */
sealed interface ResolutionNote {
    data class PlanAssignedAsIs(val planVersion: Int) : ResolutionNote
    data class PlanAssignedWithEdits(val planVersion: Int) : ResolutionNote
    data object ProposalDiscarded : ResolutionNote

    data class Custom(val text: String) : ResolutionNote {
        init {
            require(text.isNotBlank()) { "A custom note needs text" }
        }
    }
}

/**
 * Resolución de PR14 (`POST /review-items/{id}/resolution`). **Decir si el plan se ajustó es obligatorio**: sin eso no
 * queda constancia de qué provocó la señal (*Resolution States Whether The Plan Was Adjusted*).
 */
@ConsistentCopyVisibility
data class ReviewResolution private constructor(val resolvedWithAdjustment: Boolean, val note: String?) {

    companion object {
        /** `null` si no se respondió Sí o No (PR14.1 «Responde Sí o No»). La nota vacía no se envía. */
        fun of(resolvedWithAdjustment: Boolean?, note: String?): ReviewResolution? =
            resolvedWithAdjustment?.let { ReviewResolution(it, note?.trim()?.takeIf(String::isNotEmpty)) }
    }
}
