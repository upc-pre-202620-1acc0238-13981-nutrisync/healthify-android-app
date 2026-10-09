package pe.edu.upc.healthify.features.intake.infrastructure.local

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.PrimaryKey

/**
 * Copia offline de las metas vigentes de cada paciente que entró en el dispositivo (PT3.O, PT4 sin conexión).
 * [payloadJson] es el `ActiveTargetsResource` tal como llegó (`ActiveTargetsDto`): así la copia conserva los
 * códigos y se traduce al idioma que tenga la app al leerla. El backup está deshabilitado para toda la app.
 */
@Entity(tableName = "active_targets_cache")
data class ActiveTargetsCacheEntity(
    @PrimaryKey
    @ColumnInfo(name = "patient_id")
    val patientId: Long,
    @ColumnInfo(name = "payload_json")
    val payloadJson: String,
    /** Cuándo la guardó el dispositivo (epoch ms): «actualizadas hoy 8:10 a. m.». */
    @ColumnInfo(name = "saved_at")
    val savedAtEpochMillis: Long,
)
