package pe.edu.upc.healthify.features.nutritionalcare.domain.entity

import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * PT20 · Mi expediente (`PatientOwnRecordResource`, RM-4): el expediente que el paciente ve de sí mismo. Por diseño
 * del backend **nunca** trae diagnóstico, base de cálculo, IMC ni su categoría (regla ética 1). Cada sección puede
 * venir vacía (un contexto que no respondió) sin tumbar el resto.
 */
data class PatientOwnRecord(
    val practitioner: RecordPractitioner?,
    val nextFollowUp: RecordNextFollowUp?,
    val myNumbers: MyNumbers,
    val plan: RecordPlan?,
    val referrals: List<RecordReferral>,
) {
    /** «Derivaciones»: solo las abiertas («Sin derivaciones activas» si no hay). */
    val openReferrals: List<RecordReferral> get() = referrals.filter { it.isOpen }
}

/** «Tu nutricionista»: el nombre tal cual, desde cuándo lo acompaña y si el vínculo está vigente. */
data class RecordPractitioner(
    val fullName: String?,
    val linkedSince: Instant?,
    val isActive: Boolean,
)

/** «Mis consultas · Próxima: …». */
data class RecordNextFollowUp(
    val followUpId: Long,
    val scheduledFor: Instant,
) {
    init {
        require(followUpId > 0) { "A follow up id is positive" }
    }
}

/** «MIS NÚMEROS»: cada número puede faltar (sin plan, sin días evaluados, sin medición clínica, sin tendencia). */
data class MyNumbers(
    val energyTargetKcal: Int?,
    val planVersion: Int?,
    val compliance: RecordCompliance?,
    val clinicalWeight: RecordClinicalWeight?,
    val weightSlopeKgPerWeek: Double?,
) {
    val isEmpty: Boolean
        get() = energyTargetKcal == null && compliance == null && clinicalWeight == null && weightSlopeKgPerWeek == null

    init {
        require(energyTargetKcal == null || energyTargetKcal > 0) { "An energy target is positive" }
        require(planVersion == null || planVersion > 0) { "A plan version is positive" }
    }

    companion object {
        val Empty = MyNumbers(null, null, null, null, null)
    }
}

/**
 * «Mi cumplimiento 5 de 7 días»: días con la meta cumplida sobre los días del rango. Un día sin registro no cuenta como
 * incumplido (regla ética 3): el backend ya lo excluye de [met] y la UI nunca lo pinta como falla.
 */
data class RecordCompliance(
    val from: LocalDate,
    val to: LocalDate,
    val met: Int,
    val total: Int,
) {
    init {
        require(!to.isBefore(from)) { "A compliance range is not inverted" }
        require(total >= 0 && met in 0..total) { "Met days are between 0 and the total" }
    }

    /** Días del rango («Últimos 7 días»). */
    val rangeDays: Int get() = (ChronoUnit.DAYS.between(from, to) + 1).toInt()
}

/** «Peso clínico · Evaluación del 3 mar.»: la última medición de una consulta (nunca un autopesaje). */
data class RecordClinicalWeight(
    val kg: Double,
    val takenAt: Instant,
) {
    init {
        require(kg > 0) { "A clinical weight is positive" }
    }
}

/**
 * «MI PLAN»: códigos del plan tal cual llegan (la app los traduce en la presentación). Una indicación es `code` del
 * catálogo o `custom` escrito por el nutricionista.
 */
data class RecordPlan(
    val guidelines: List<RecordGuideline>,
    val restrictions: List<String>,
    val legacyRestrictions: List<String>,
) {
    val isEmpty: Boolean get() = guidelines.isEmpty() && restrictions.isEmpty() && legacyRestrictions.isEmpty()
}

data class RecordGuideline(val code: String?, val custom: String?) {
    init {
        require(!code.isNullOrBlank() || !custom.isNullOrBlank()) { "A guideline has a code or a text" }
    }
}

/** Una derivación a otra especialidad. La especialidad la escribió el nutricionista: se muestra tal cual. */
data class RecordReferral(
    val id: Long,
    val specialty: String,
    val issuedAt: Instant,
    val isOpen: Boolean,
)
