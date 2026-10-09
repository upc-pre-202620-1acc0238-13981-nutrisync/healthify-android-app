package pe.edu.upc.healthify.features.intake.domain.valueobject

/**
 * «¿Esta comida estaba en tu plan?» (IN-1). Se responde al confirmar y no cambia después. «No» marca la entrada como
 * «Fuera del plan» y **igual cuenta** en el registro. [NOT_ANSWERED] solo existe en entradas «Por confirmar» o
 * históricas: un registro nuevo siempre lleva respuesta (`PlanAdherenceRequired`).
 */
enum class PlanAdherence(val code: String) {
    IN_PLAN("InPlan"),
    OFF_PLAN("OffPlan"),
    NOT_ANSWERED("NotAnswered"),
    ;

    val isAnswered: Boolean get() = this != NOT_ANSWERED

    companion object {
        fun fromAnswer(inPlan: Boolean): PlanAdherence = if (inPlan) IN_PLAN else OFF_PLAN

        /** Un valor desconocido se lee como sin responder: no se inventa una respuesta. */
        fun fromCode(code: String?): PlanAdherence =
            entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: NOT_ANSWERED
    }
}
