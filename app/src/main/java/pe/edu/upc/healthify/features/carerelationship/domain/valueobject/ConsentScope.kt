package pe.edu.upc.healthify.features.carerelationship.domain.valueobject

/**
 * Alcance del consentimiento informado (`GrantConsentResource.Scope`, F6). PT2 no ofrece nada para elegir: el paciente
 * acepta un texto fijo, así que la app manda un literal **versionado** que dice qué texto aceptó (CR-5, Ley 29733).
 * Vacío → `ConsentScopeRequired`; se valida aquí para que nunca llegue al backend.
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
         * DECISIÓN PT2/CR-5: literal recomendado por el backend para el texto vigente de PT2 (qué compartes, qué
         * puede hacer tu nutricionista, derechos y privacidad). Si cambia el texto de PT2, se sube la versión.
         * La IA es una decisión aparte (`aiProcessingGranted`, CR-2), no forma parte del alcance.
         */
        val CURRENT = ConsentScope("consent-v2@2026-10: diary,self-weigh-ins,active-targets")
    }
}
