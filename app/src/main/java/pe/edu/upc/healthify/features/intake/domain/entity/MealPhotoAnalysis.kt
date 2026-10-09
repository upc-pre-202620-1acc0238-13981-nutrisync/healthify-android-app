package pe.edu.upc.healthify.features.intake.domain.entity

import pe.edu.upc.healthify.features.intake.domain.valueobject.LocalTimestamp
import pe.edu.upc.healthify.features.intake.domain.valueobject.MealFood
import pe.edu.upc.healthify.features.intake.domain.valueobject.MealPhotoAnalysisId
import java.time.Instant

/**
 * Lo que la IA del servidor estimó a partir de la foto (`MealPhotoAnalysisResource`, IN-7). Es una **propuesta**,
 * nunca ingesta: no se guarda nada en el diario hasta que el paciente confirma (*Estimate Stored As Proposal Only*).
 *
 * @param confidence siempre presente, entre 0 y 1 (*Confidence Always Attached*).
 * @param expiresAt el análisis vence a las 24 h (`422 MealPhotoAnalysisExpired`).
 */
data class MealPhotoAnalysis(
    val id: MealPhotoAnalysisId,
    val food: MealFood,
    val estimatedGrams: Double,
    val confidence: Double,
    val alternatives: List<MealPhotoAlternative>,
    val expiresAt: Instant,
) {
    init {
        require(estimatedGrams > 0) { "An estimate needs grams" }
        require(confidence in 0.0..1.0) { "Confidence must be between 0 and 1" }
        require(alternatives.size <= MAX_ALTERNATIVES) { "At most $MAX_ALTERNATIVES alternatives" }
    }

    /** Las alternativas que se pueden elegir: solo las que el catálogo resolvió. */
    val selectableAlternatives: List<MealPhotoAlternative> get() = alternatives.filter { it.food != null }

    companion object {
        const val MAX_ALTERNATIVES = 3
    }
}

/** Otro plato posible. [food] es `null` si no está en el catálogo local (se muestra, pero no se puede elegir). */
data class MealPhotoAlternative(val name: String, val grams: Double, val food: MealFood?) {
    init {
        require(name.isNotBlank()) { "An alternative needs a name" }
        require(grams > 0) { "An alternative needs grams" }
    }
}

/**
 * Foto tomada sin conexión que espera para analizarse al volver la red (PT5 «Sin conexión: la captura funciona
 * igual, queda pendiente»). Vive en `cacheDir` y se borra al analizarla o descartarla.
 *
 * @param capturedAt cuándo se tomó: es el «¿Cuándo comiste?» propuesto al registrarla.
 */
data class PendingMealPhoto(
    val id: String,
    val filePath: String,
    val capturedAt: LocalTimestamp,
) {
    init {
        require(id.isNotBlank() && filePath.isNotBlank()) { "A pending photo needs an id and a file" }
    }
}
