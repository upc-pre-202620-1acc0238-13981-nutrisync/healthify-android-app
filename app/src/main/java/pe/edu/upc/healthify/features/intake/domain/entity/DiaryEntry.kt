package pe.edu.upc.healthify.features.intake.domain.entity

import pe.edu.upc.healthify.features.intake.domain.valueobject.DiaryEntryId
import pe.edu.upc.healthify.features.intake.domain.valueobject.EntryProvenance
import pe.edu.upc.healthify.features.intake.domain.valueobject.LocalTimestamp
import pe.edu.upc.healthify.features.intake.domain.valueobject.PlanAdherence
import java.time.Instant
import java.time.LocalDate

/**
 * Una entrada del diario (`DiaryEntryResource`). **Nunca se borra** (*Diary Entry Never Deleted*): no hay forma de
 * quitarla, solo de confirmar su porción si quedó «Por confirmar».
 *
 * La propuesta de la IA y lo que confirmó el paciente se guardan juntos (*Proposal Kept Alongside Confirmation*):
 * [proposedGrams] sobrevive a un [confirmedGrams] distinto.
 *
 * @param confidence de la estimación por foto, entre 0 y 1 (*Confidence Always Attached*).
 * @param isCountedTowardsTargets solo lo confirmado suma a las metas del día.
 * @param mealGroupId entradas registradas juntas (una idea de comida, IN-6): el diario las muestra como una comida.
 */
data class DiaryEntry(
    val id: DiaryEntryId,
    val localTimestamp: LocalTimestamp,
    val provenance: EntryProvenance,
    val foodName: String?,
    val proposedFoodId: Long?,
    val proposedGrams: Double?,
    val confidence: Double?,
    val confirmedFoodId: Long?,
    val confirmedGrams: Double?,
    val planAdherence: PlanAdherence,
    val isCountedTowardsTargets: Boolean,
    val mealGroupId: String? = null,
    val fromMealIdea: Boolean = false,
) {
    init {
        require(confidence == null || confidence in 0.0..1.0) { "Confidence must be between 0 and 1" }
        require(proposedGrams == null || proposedGrams > 0) { "A proposed portion must be positive" }
        require(confirmedGrams == null || confirmedGrams > 0) { "A confirmed portion must be positive" }
    }

    /** Foto con propuesta que el paciente todavía no confirmó: «Por confirmar · Aún no cuenta para tus metas». */
    val isPendingConfirmation: Boolean
        get() = provenance == EntryProvenance.PHOTO && confirmedGrams == null && proposedGrams != null

    /** Lo confirmado si existe; si no, la propuesta («280 g aprox.»). */
    val displayedGrams: Double? get() = confirmedGrams ?: proposedGrams

    val isOffPlan: Boolean get() = planAdherence == PlanAdherence.OFF_PLAN || provenance == EntryProvenance.OFF_PLAN
}

/**
 * Un día del diario (PT14).
 *
 * @param savedAt cuándo se guardó la copia del teléfono; solo si [fromCache] (sin conexión).
 */
data class DiaryDay(
    val date: LocalDate,
    val entries: List<DiaryEntry>,
    val fromCache: Boolean = false,
    val savedAt: Instant? = null,
) {
    init {
        require(!fromCache || savedAt != null) { "A cached day needs savedAt" }
    }

    /** «Por confirmar» primero (piden una acción); después de la más reciente a la más antigua. */
    val orderedEntries: List<DiaryEntry>
        get() = entries.sortedWith(
            compareByDescending<DiaryEntry> { it.isPendingConfirmation }.thenByDescending { it.localTimestamp.instant },
        )
}
