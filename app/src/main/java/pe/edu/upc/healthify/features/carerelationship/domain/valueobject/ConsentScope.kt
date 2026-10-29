package pe.edu.upc.healthify.features.carerelationship.domain.valueobject

/**
 * Scope of informed consent (`GrantConsentResource.Scope`). The consent screen offers nothing to choose: the patient
 * accepts a fixed text, so the app sends a **versioned** literal that says which text they accepted (Ley 29733).
 * Blank → `ConsentScopeRequired`; it is validated here so it never reaches the backend.
 */
@JvmInline
value class ConsentScope(val value: String) {
    init {
        require(value.isNotBlank()) { "ConsentScope must not be blank" }
        require(value.startsWith(VERSION_PREFIX)) { "ConsentScope must start with $VERSION_PREFIX" }
    }

    companion object {
        private const val VERSION_PREFIX = "consent-v"

        /**
         * Literal the backend recommends for the current consent text (what you share, what your nutritionist
         * can do, rights and privacy). If that text changes, bump the version.
         * AI is a separate decision (`aiProcessingGranted`), not part of the scope.
         */
        val CURRENT = ConsentScope("consent-v2@2026-10: diary,self-weigh-ins,active-targets")
    }
}
