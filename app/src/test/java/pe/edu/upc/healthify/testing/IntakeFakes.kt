package pe.edu.upc.healthify.testing

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import pe.edu.upc.healthify.features.carerelationship.domain.entity.AiPreferences
import pe.edu.upc.healthify.features.carerelationship.domain.repository.AiPreferencesRepository
import pe.edu.upc.healthify.features.carerelationship.domain.valueobject.PatientId as CarePatientId
import pe.edu.upc.healthify.features.foodcatalog.domain.entity.NewLocalFood
import pe.edu.upc.healthify.features.foodcatalog.domain.entity.ReferenceFood
import pe.edu.upc.healthify.features.foodcatalog.domain.repository.ReferenceFoodRepository
import pe.edu.upc.healthify.features.foodcatalog.domain.valueobject.FoodSearchTerm
import pe.edu.upc.healthify.features.foodcatalog.domain.valueobject.ReferenceFoodId
import pe.edu.upc.healthify.features.intake.domain.entity.DiaryDay
import pe.edu.upc.healthify.features.intake.domain.entity.LoggedMealNotice
import pe.edu.upc.healthify.features.intake.domain.entity.MealIdeas
import pe.edu.upc.healthify.features.intake.domain.entity.MealLogOutcome
import pe.edu.upc.healthify.features.intake.domain.entity.MealPhotoAlternative
import pe.edu.upc.healthify.features.intake.domain.entity.MealPhotoAnalysis
import pe.edu.upc.healthify.features.intake.domain.entity.NewManualMeal
import pe.edu.upc.healthify.features.intake.domain.entity.NewMealGroup
import pe.edu.upc.healthify.features.intake.domain.entity.NewPhotoMeal
import pe.edu.upc.healthify.features.intake.domain.entity.PendingDiaryEntry
import pe.edu.upc.healthify.features.intake.domain.entity.PendingMealPhoto
import pe.edu.upc.healthify.features.intake.domain.repository.DiaryRepository
import pe.edu.upc.healthify.features.intake.domain.repository.MealIdeasRepository
import pe.edu.upc.healthify.features.intake.domain.repository.MealPhotoRepository
import pe.edu.upc.healthify.features.intake.domain.valueobject.ClientEntryId
import pe.edu.upc.healthify.features.intake.domain.valueobject.DiaryEntryId
import pe.edu.upc.healthify.features.intake.domain.valueobject.LocalTimestamp
import pe.edu.upc.healthify.features.intake.domain.valueobject.MealFood
import pe.edu.upc.healthify.features.intake.domain.valueobject.MealPhotoAnalysisId
import pe.edu.upc.healthify.features.intake.domain.valueobject.PatientId
import pe.edu.upc.healthify.features.intake.domain.valueobject.PlanAdherence
import pe.edu.upc.healthify.features.intake.domain.valueobject.PortionGrams
import java.time.Instant
import java.time.LocalDate

fun aiPreferences(photo: Boolean = true, ideas: Boolean = true, consent: Boolean = true) = AiPreferences(
    consentGranted = consent,
    weeklySummaryEnabled = consent,
    mealIdeasEnabled = ideas,
    suggestedQuestionsEnabled = consent,
    mealPhotoRecognitionEnabled = photo,
)

class FakeAiPreferencesRepository(var preferences: AiPreferences = aiPreferences()) : AiPreferencesRepository {
    /** `null` = acepta y devuelve lo pedido. */
    var updateResult: Result<AiPreferences>? = null
    val updates = mutableListOf<AiPreferences>()

    override suspend fun getAiPreferences(patientId: CarePatientId): AiPreferences = preferences

    override suspend fun updateAiPreferences(patientId: CarePatientId, preferences: AiPreferences): Result<AiPreferences> {
        updates += preferences
        val result = updateResult ?: Result.success(preferences)
        result.getOrNull()?.let { this.preferences = it }
        return result
    }
}

fun mealPhotoAnalysis(
    grams: Double = 320.0,
    confidence: Double = 0.82,
    alternatives: List<MealPhotoAlternative> = listOf(
        MealPhotoAlternative("Tallarín saltado", 300.0, MealFood(31, "Tallarín saltado")),
        MealPhotoAlternative("Pollo saltado", 280.0, null),
    ),
) = MealPhotoAnalysis(
    id = MealPhotoAnalysisId("8f14e45f-ceea-467a-9a3b-2c0b1e5f9d10"),
    food = MealFood(12, "Lomo saltado"),
    estimatedGrams = grams,
    confidence = confidence,
    alternatives = alternatives,
    expiresAt = Instant.parse("2026-10-08T13:00:00Z"),
)

/** Respuestas programables de la foto; registra qué se analizó, guardó y borró. */
class FakeMealPhotoRepository : MealPhotoRepository {
    var analyzeResults: List<Result<MealPhotoAnalysis>> = listOf(Result.success(mealPhotoAnalysis()))
    val analyzed = mutableListOf<String>()
    val discarded = mutableListOf<String>()
    val pending = MutableStateFlow<List<PendingMealPhoto>>(emptyList())

    override suspend fun analyze(patientId: PatientId, photoPath: String): Result<MealPhotoAnalysis> {
        val result = analyzeResults[minOf(analyzed.size, analyzeResults.lastIndex)]
        analyzed += photoPath
        return result
    }

    override suspend fun savePendingPhoto(patientId: PatientId, photoPath: String, capturedAt: LocalTimestamp): PendingMealPhoto {
        val photo = PendingMealPhoto(id = "pending-${pending.value.size + 1}", filePath = photoPath, capturedAt = capturedAt)
        pending.value = pending.value + photo
        return photo
    }

    override fun observePendingPhotos(patientId: PatientId): Flow<List<PendingMealPhoto>> = pending

    override suspend fun getPendingPhoto(patientId: PatientId, id: String): PendingMealPhoto? =
        pending.value.firstOrNull { it.id == id }

    override suspend fun discardPhoto(photoPath: String) {
        discarded += photoPath
        pending.value = pending.value.filterNot { it.filePath == photoPath }
    }
}

/** Diario programable; registra cada registro. */
class FakeDiaryRepository : DiaryRepository {
    var dayResult: (LocalDate) -> Result<DiaryDay> = { Result.success(DiaryDay(it, emptyList())) }
    var logResult: Result<MealLogOutcome> = Result.success(MealLogOutcome.LOGGED)
    var confirmResult: Result<Unit> = Result.success(Unit)
    val pendingEntries = MutableStateFlow<List<PendingDiaryEntry>>(emptyList())
    private val notice = MutableStateFlow<LoggedMealNotice?>(null)

    val manualMeals = mutableListOf<NewManualMeal>()
    val photoMeals = mutableListOf<NewPhotoMeal>()
    val unconfirmed = mutableListOf<ClientEntryId>()
    val groups = mutableListOf<NewMealGroup>()
    val confirmations = mutableListOf<Pair<DiaryEntryId, PlanAdherence>>()
    val adjustments = mutableListOf<Triple<DiaryEntryId, MealFood, PortionGrams>>()
    var syncRequests = 0

    override suspend fun getDiaryDay(patientId: PatientId, date: LocalDate) = dayResult(date)

    override fun observePendingEntries(patientId: PatientId): Flow<List<PendingDiaryEntry>> = pendingEntries

    /** `tryEmit(Unit)` simula que el backend aceptó comidas de la cola. */
    val synced = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    override val syncedEntries: Flow<Unit> = synced

    override suspend fun logManualMeal(patientId: PatientId, meal: NewManualMeal): Result<MealLogOutcome> {
        manualMeals += meal
        return logResult
    }

    override suspend fun logPhotoMeal(patientId: PatientId, meal: NewPhotoMeal): Result<MealLogOutcome> {
        photoMeals += meal
        return logResult
    }

    override suspend fun keepPhotoMealUnconfirmed(
        patientId: PatientId,
        analysis: MealPhotoAnalysis,
        localTimestamp: LocalTimestamp,
        clientEntryId: ClientEntryId,
    ): Result<MealLogOutcome> {
        unconfirmed += clientEntryId
        return logResult
    }

    override suspend fun logMealGroup(patientId: PatientId, group: NewMealGroup): Result<MealLogOutcome> {
        groups += group
        return logResult
    }

    override suspend fun confirmEstimate(entryId: DiaryEntryId, planAdherence: PlanAdherence): Result<Unit> {
        confirmations += entryId to planAdherence
        return confirmResult
    }

    override suspend fun adjustEstimate(
        entryId: DiaryEntryId,
        food: MealFood,
        portion: PortionGrams,
        planAdherence: PlanAdherence,
    ): Result<Unit> {
        adjustments += Triple(entryId, food, portion)
        return confirmResult
    }

    override fun requestSync() {
        syncRequests++
    }

    override val loggedMealNotice: Flow<LoggedMealNotice?> = notice

    override fun consumeLoggedMealNotice() {
        notice.value = null
    }

    fun publishNotice(value: LoggedMealNotice) {
        notice.value = value
    }
}

class FakeMealIdeasRepository : MealIdeasRepository {
    var results: List<Result<MealIdeas>> = emptyList()
    val requests = mutableListOf<List<String>>()

    override suspend fun generate(patientId: PatientId, localDate: LocalDate, excludeIdeaIds: List<String>): Result<MealIdeas> {
        val result = results[minOf(requests.size, results.lastIndex)]
        requests += excludeIdeaIds
        return result
    }
}

fun referenceFood(id: Long, name: String, kcal: Double = 120.0, override: Boolean = false) =
    ReferenceFood(ReferenceFoodId(id), name, kcal, 4.0, 20.0, 2.0, isLocalOverride = override)

class FakeReferenceFoodRepository : ReferenceFoodRepository {
    var local: List<ReferenceFood> = emptyList()
    var remote: Result<List<ReferenceFood>> = Result.success(emptyList())
    var refreshes = 0

    override suspend fun searchLocal(term: FoodSearchTerm): List<ReferenceFood> =
        local.filter { it.name.contains(term.value, ignoreCase = true) }

    override suspend fun searchRemote(term: FoodSearchTerm): Result<List<ReferenceFood>> = remote

    override suspend fun refreshLocalCatalog(patientId: Long, force: Boolean): Result<Unit> {
        refreshes++
        return if (remote.isFailure) Result.failure(requireNotNull(remote.exceptionOrNull())) else Result.success(Unit)
    }

    val browsedTerms = mutableListOf<String?>()
    var createResult: Result<ReferenceFood>? = null
    val created = mutableListOf<NewLocalFood>()

    override suspend fun browseCatalog(term: FoodSearchTerm?): Result<List<ReferenceFood>> {
        browsedTerms += term?.value
        return remote
    }

    override suspend fun createLocalFood(food: NewLocalFood): Result<ReferenceFood> {
        created += food
        return createResult ?: Result.success(referenceFood(900, food.name, food.energyKcalPer100g, override = true))
    }
}
