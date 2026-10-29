package pe.edu.upc.healthify.features.carerelationship.domain.valueobject

/**
 * Clinical reason for the discharge (`DischargePatientResource.clinicalReason`). **Required**: a deliberate asymmetry
 * with withdrawing consent, which asks for no reason at all. The practitioner's own text: never translated.
 */
@JvmInline
value class ClinicalReason private constructor(val text: String) {

    companion object {
        /** `null` if blank ("Write the clinical reason for the discharge"). */
        fun of(text: String): ClinicalReason? = text.trim().takeIf(String::isNotEmpty)?.let(::ClinicalReason)
    }
}
