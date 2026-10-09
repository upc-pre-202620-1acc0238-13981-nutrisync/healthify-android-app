package pe.edu.upc.healthify.features.iam.domain.valueobject

/** Idioma preferido de la cuenta (IAM-3). El backend solo acepta `es` y `en`; `es` es el valor por defecto. */
enum class PreferredLanguage(val code: String) {
    SPANISH("es"),
    ENGLISH("en"),
    ;

    companion object {
        val DEFAULT = SPANISH

        fun fromCode(code: String?): PreferredLanguage =
            entries.firstOrNull { it.code.equals(code?.trim(), ignoreCase = true) } ?: DEFAULT
    }
}
