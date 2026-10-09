package pe.edu.upc.healthify.features.nutritionalcare.domain.entity

import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.PatientMessage
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.PlanGuidelineCode
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.ReviewItemId
import java.time.Instant

/** En qué quedó una propuesta (`PlanProposalStatus`). Solo [PROPOSED] se puede aceptar. */
enum class PlanProposalStatus(val code: String) {
    PROPOSED("Proposed"),
    ACCEPTED_AS_IS("AcceptedAsIs"),
    ACCEPTED_WITH_EDITS("AcceptedWithEdits"),
    DISMISSED("Dismissed"),
    ;

    companion object {
        /** Un estado desconocido se lee como ya decidido: la app no ofrece aceptar algo que no entiende. */
        fun fromCode(code: String?): PlanProposalStatus =
            entries.firstOrNull { it.code.equals(code?.trim(), ignoreCase = true) } ?: DISMISSED
    }
}

/**
 * Plan que la IA propone ante una desviación sostenida (`PlanAdjustmentProposalResource`, IA-8 + NC-10). Leerlo nunca
 * cambia el plan: el único camino a una versión nueva es que el profesional la acepte.
 *
 * @param title y [rationale] vienen en el idioma del profesional; [patientMessage] en el del paciente (X-2).
 * @param addedGuidelines y [removedGuidelines] son códigos del catálogo cerrado (la IA no puede devolver otros).
 */
data class PlanAdjustmentProposal(
    val reviewItemId: ReviewItemId,
    val title: String,
    val currentEnergyKcal: Double?,
    val proposed: Targets,
    val addedGuidelines: List<PlanGuidelineCode>,
    val removedGuidelines: List<PlanGuidelineCode>,
    val patientMessage: String,
    val recheckAfterDays: Int,
    val rationale: String,
    val generatedAt: Instant,
    val status: PlanProposalStatus,
) {
    init {
        require(title.isNotBlank()) { "A proposal has a title" }
        require(recheckAfterDays > 0) { "The recheck is in the future" }
    }

    val canBeAccepted: Boolean get() = status == PlanProposalStatus.PROPOSED

    /**
     * Indicaciones del catálogo con que queda la versión si se acepta tal cual: las vigentes menos las quitadas más las
     * agregadas (las propias del profesional se conservan siempre en el backend y no se editan aquí).
     */
    fun resultingGuidelines(current: Collection<PlanGuidelineCode>): Set<PlanGuidelineCode> =
        (current.filterNot { it in removedGuidelines } + addedGuidelines).toCollection(LinkedHashSet())
}

/** Lo que responde `GET /review-items/{id}/plan-proposal`. */
sealed interface PlanProposalLookup {
    data class Ready(val proposal: PlanAdjustmentProposal) : PlanProposalLookup

    /** `202` + `Retry-After`: se está generando. */
    data class Generating(val retryAfterSeconds: Int) : PlanProposalLookup {
        init {
            require(retryAfterSeconds > 0) { "Retry-After is positive" }
        }
    }

    /** `404`: no hay propuesta (IA apagada, sin consentimiento, proveedor caído o salida rechazada) → PR14 sin IA. */
    data object None : PlanProposalLookup
}

/** «Resolver» de PR14.IA (tal cual) o «Asignar plan ajustado» de PR14.IA-A (con ediciones). */
sealed interface PlanAcceptance {
    data object AsIs : PlanAcceptance

    data class WithEdits(val edits: ProposalEdits) : PlanAcceptance
}

/**
 * Ediciones de PR14.IA-A. Solo se validan contra el piso calórico en el backend (`422 PlanProposalOutOfSafetyBounds`);
 * aquí se exige que las metas sean positivas, que es lo mínimo que el backend acepta.
 *
 * @param guidelines lista **completa** de indicaciones del catálogo que deja el profesional.
 */
data class ProposalEdits(
    val targets: Targets,
    val guidelines: Set<PlanGuidelineCode>,
    val patientMessage: PatientMessage?,
) {
    init {
        require(targets.proteinG > 0 && targets.carbG > 0 && targets.fatG > 0) { "Edited macros must be positive" }
    }
}

/** El ítem quedó resuelto y la versión [planVersion] es la vigente (`PlanProposalAcceptanceResource`). */
data class AcceptedProposal(val reviewItem: ReviewItem, val planVersion: Int)

/** Campo de PR14.IA-A con un valor que no sirve. */
enum class ProposalEditField { ENERGY, PROTEIN, CARB, FAT, MESSAGE }

/** Validación del formulario de PR14.IA-A (fail fast antes de enviar). */
sealed interface ProposalEditValidation {
    data class Valid(val edits: ProposalEdits) : ProposalEditValidation
    data class Invalid(val fields: Set<ProposalEditField>) : ProposalEditValidation

    companion object {
        fun of(
            energyText: String,
            proteinText: String,
            carbText: String,
            fatText: String,
            guidelines: Set<PlanGuidelineCode>,
            message: String,
        ): ProposalEditValidation {
            val energy = parsePositive(energyText)
            val protein = parsePositive(proteinText)
            val carb = parsePositive(carbText)
            val fat = parsePositive(fatText)
            val trimmedMessage = message.trim()
            val invalid = buildSet {
                if (energy == null) add(ProposalEditField.ENERGY)
                if (protein == null) add(ProposalEditField.PROTEIN)
                if (carb == null) add(ProposalEditField.CARB)
                if (fat == null) add(ProposalEditField.FAT)
                if (trimmedMessage.length > PatientMessage.MAX_LENGTH) add(ProposalEditField.MESSAGE)
            }
            if (invalid.isNotEmpty() || energy == null || protein == null || carb == null || fat == null) {
                return Invalid(invalid)
            }
            return Valid(
                ProposalEdits(
                    targets = Targets(energy, protein, carb, fat),
                    guidelines = guidelines,
                    patientMessage = PatientMessage.ofOptional(trimmedMessage),
                ),
            )
        }

        /** «1 650», «1650», «62,5» o «62.5»; `null` si no es un número positivo. */
        internal fun parsePositive(text: String): Double? {
            val normalized = text.filterNot { it.isWhitespace() || it == ' ' || it == ' ' }.replace(',', '.')
            return normalized.toDoubleOrNull()?.takeIf { it > 0 && it.isFinite() }
        }
    }
}
