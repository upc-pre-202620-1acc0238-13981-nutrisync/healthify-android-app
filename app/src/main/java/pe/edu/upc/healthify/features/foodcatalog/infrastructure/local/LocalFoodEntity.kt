package pe.edu.upc.healthify.features.foodcatalog.infrastructure.local

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.PrimaryKey

/**
 * The phone's copy of the catalog (`local-food-catalog`, ≤ 500 foods) for offline search. It is
 * a public reference catalog, not patient data.
 *
 * @param normalizedName lowercase name without accents, so "quinua" finds "Quínua".
 * @param position order in which it came from the server (the nutritionist's local foods first).
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
