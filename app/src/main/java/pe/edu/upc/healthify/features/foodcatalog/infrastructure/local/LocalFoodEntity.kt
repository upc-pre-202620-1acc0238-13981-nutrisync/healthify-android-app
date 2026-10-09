package pe.edu.upc.healthify.features.foodcatalog.infrastructure.local

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.PrimaryKey

/**
 * Copia del catálogo en el teléfono (`local-food-catalog`, ≤ 500 alimentos) para buscar sin conexión (PT9). Es
 * un catálogo de referencia público, no datos del paciente.
 *
 * @param normalizedName nombre en minúsculas y sin tildes, para buscar «quinua» con «Quínua».
 * @param position orden en que llegó del servidor (los locales del nutricionista primero).
 */
@Entity(tableName = "local_food_catalog")
data class LocalFoodEntity(
    @PrimaryKey
    @ColumnInfo(name = "reference_food_id")
    val referenceFoodId: Long,
    @ColumnInfo(name = "local_name")
    val localName: String,
    @ColumnInfo(name = "normalized_name")
    val normalizedName: String,
    @ColumnInfo(name = "energy_kcal_per_100g")
    val energyKcalPer100g: Double,
    @ColumnInfo(name = "protein_g_per_100g")
    val proteinGPer100g: Double,
    @ColumnInfo(name = "carb_g_per_100g")
    val carbGPer100g: Double,
    @ColumnInfo(name = "fat_g_per_100g")
    val fatGPer100g: Double,
    @ColumnInfo(name = "is_local_override")
    val isLocalOverride: Boolean,
    val position: Int,
    @ColumnInfo(name = "saved_at")
    val savedAtEpochMillis: Long,
)
