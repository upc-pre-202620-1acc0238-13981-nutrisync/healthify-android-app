package pe.edu.upc.healthify.features.intake.domain.valueobject

import java.util.UUID

/**
 * Id que el teléfono genera **una vez** por registro (UUID) y reenvía siempre igual: en `manual-logs`,
 * `photo-logs`, cada ítem de `manual-logs/batch` y la sincronización offline. Así un reintento o un reenvío nunca
 * duplica la comida en el diario (IN-7, F20).
 */
@JvmInline
value class ClientEntryId(val value: String) {
    init {
        require(isUuid(value)) { "ClientEntryId must be a UUID" }
    }

    companion object {
        fun random(): ClientEntryId = ClientEntryId(UUID.randomUUID().toString())

        private fun isUuid(value: String): Boolean =
            try {
                UUID.fromString(value).toString().equals(value, ignoreCase = true)
            } catch (_: IllegalArgumentException) {
                false
            }
    }
}
