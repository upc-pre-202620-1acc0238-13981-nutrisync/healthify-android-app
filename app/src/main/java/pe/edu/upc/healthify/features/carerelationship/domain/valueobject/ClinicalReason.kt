package pe.edu.upc.healthify.features.carerelationship.domain.valueobject

/**
 * Motivo clínico del alta (`DischargePatientResource.clinicalReason`, F29). **Obligatorio**: asimetría deliberada con
 * el retiro del consentimiento (PT23), que no pide ninguna justificación. Texto del profesional: nunca se traduce.
 */
@JvmInline
value class ClinicalReason private constructor(val text: String) {

    companion object {
        /** `null` si está vacío (PR18 «Escribe el motivo clínico del alta»). */
        fun of(text: String): ClinicalReason? = text.trim().takeIf(String::isNotEmpty)?.let(::ClinicalReason)
    }
}
