package pe.edu.upc.healthify.features.intake.domain.valueobject

import java.util.UUID

/**
 * Id the phone generates **once** per log (UUID) and always resends unchanged: in `manual-logs`,
 * `photo-logs`, each item of `manual-logs/batch` and the offline sync. That way a retry or a resend never
 * duplicates the meal in the diary.
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
