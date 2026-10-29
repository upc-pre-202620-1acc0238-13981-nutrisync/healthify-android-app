package pe.edu.upc.healthify.features.intake.infrastructure.local

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.PrimaryKey

/**
 * Offline copy of the active targets of each patient who signed in on the device.
 * [payloadJson] is the `ActiveTargetsResource` as it arrived (`ActiveTargetsDto`), so the copy keeps the
 * codes and is translated into the app's current language when read. Backup is disabled for the whole app.
 */
@Entity(tableName = "active_targets_cache")
data class ActiveTargetsCacheEntity(
    @PrimaryKey
    @ColumnInfo(name = "patient_id")
    val patientId: Long,
    @ColumnInfo(name = "payload_json")
    val payloadJson: String,
    /** When the device saved it (epoch ms): "updated today 8:10 a.m.". */
    @ColumnInfo(name = "saved_at")
    val savedAtEpochMillis: Long,
)
