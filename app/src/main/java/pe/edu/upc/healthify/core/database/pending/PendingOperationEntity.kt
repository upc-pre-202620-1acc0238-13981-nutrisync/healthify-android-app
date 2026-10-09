package pe.edu.upc.healthify.core.database.pending

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey

/**
 * Operación registrada sin conexión y pendiente de enviar (diario, autopesajes…). Genérica: [type] dice qué
 * `PendingOperationSender` la envía y [payload] es el JSON que esa feature serializó.
 *
 * [clientEntryId] es el UUID generado en el dispositivo al registrar: se reenvía siempre el mismo, así que el
 * backend reconoce el reenvío y no duplica (F20, IN-4).
 */
@Entity(
    tableName = "pending_operations",
    indices = [Index(value = ["owner_user_id", "type", "status"])],
)
data class PendingOperationEntity(
    @PrimaryKey
    @ColumnInfo(name = "client_entry_id")
    val clientEntryId: String,
    val type: String,
    /** El usuario que la registró: solo se envía con su sesión (el backend responde 403 a otro paciente). */
    @ColumnInfo(name = "owner_user_id")
    val ownerUserId: Long,
    val payload: String,
    /** Momento que el paciente declaró (ISO-8601 con offset). El backend nunca lo reescribe. */
    @ColumnInfo(name = "local_timestamp")
    val localTimestamp: String,
    @ColumnInfo(name = "created_at_epoch_ms")
    val createdAtEpochMs: Long,
    val attempts: Int = 0,
    /** `PENDING` o `REJECTED` (el backend no la aceptó; se conserva para mostrarla, no se reintenta). */
    val status: String = STATUS_PENDING,
    @ColumnInfo(name = "rejection_code")
    val rejectionCode: String? = null,
) {
    companion object {
        const val STATUS_PENDING = "PENDING"
        const val STATUS_REJECTED = "REJECTED"
    }
}
