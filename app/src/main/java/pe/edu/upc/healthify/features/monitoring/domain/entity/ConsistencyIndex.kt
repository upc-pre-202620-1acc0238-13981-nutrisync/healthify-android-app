package pe.edu.upc.healthify.features.monitoring.domain.entity

/**
 * Índice de consistencia (F23, `ConsistencyIndexResource`): compara la tendencia de peso con lo registrado. Solo lo
 * lee el paciente, y siempre lo ve **antes** que cualquier escalación (*Patient Prompt Required Before Escalation*).
 *
 * @param patientPromptPending MA-7: hay un aviso que el paciente todavía no vio; la app lo acusa al pintar la
 *   tarjeta de PT3 (`prompt-acknowledgement`).
 */
data class ConsistencyIndex(
    val state: ConsistencyState,
    val patientPromptPending: Boolean,
) {
    /**
     * PT3 muestra «Algo no cuadra» mientras haya alerta activa o un aviso sin ver. `Watch` no se muestra: aún no
     * hay nada que preguntarle al paciente.
     */
    val invitesPatientToReview: Boolean get() = patientPromptPending || state == ConsistencyState.ALERT
}

/** `Normal` · `Watch` · `Alert` (F23 paso 7). Un estado desconocido se lee como [NORMAL]: no se avisa de más. */
enum class ConsistencyState(val code: String) {
    NORMAL("Normal"),
    WATCH("Watch"),
    ALERT("Alert"),
    ;

    companion object {
        fun fromCode(code: String?): ConsistencyState = entries.firstOrNull { it.code == code } ?: NORMAL
    }
}
