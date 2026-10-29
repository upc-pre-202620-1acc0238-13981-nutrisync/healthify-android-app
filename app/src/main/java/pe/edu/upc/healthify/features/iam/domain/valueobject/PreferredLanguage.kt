package pe.edu.upc.healthify.features.iam.domain.valueobject

/** Preferred account language. The backend only accepts `es` and `en`; `es` is the default. */
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
