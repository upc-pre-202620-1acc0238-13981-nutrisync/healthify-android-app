package pe.edu.upc.healthify.features.intake.presentation.viewmodel

import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import pe.edu.upc.healthify.core.common.error.DomainError
import pe.edu.upc.healthify.features.carerelationship.application.usecase.GetAiPreferencesUseCase
import pe.edu.upc.healthify.features.iam.application.usecase.GetAccountCreatedOnUseCase
import pe.edu.upc.healthify.features.iam.application.usecase.ObserveCurrentUserUseCase
import pe.edu.upc.healthify.features.intake.application.usecase.ConsumeLoggedMealNoticeUseCase
import pe.edu.upc.healthify.features.intake.application.usecase.DiscardMealPhotoUseCase
import pe.edu.upc.healthify.features.intake.application.usecase.GetDiaryDayUseCase
import pe.edu.upc.healthify.features.intake.application.usecase.GetMealIdeasUseCase
import pe.edu.upc.healthify.features.intake.application.usecase.LogMealIdeaUseCase
import pe.edu.upc.healthify.features.intake.application.usecase.ObserveLoggedMealNoticeUseCase
import pe.edu.upc.healthify.features.intake.application.usecase.ObservePendingDiaryEntriesUseCase
import pe.edu.upc.healthify.features.intake.application.usecase.ObservePendingMealPhotosUseCase
import pe.edu.upc.healthify.features.intake.domain.entity.DiaryDay
import pe.edu.upc.healthify.features.intake.domain.entity.DiaryEntry
import pe.edu.upc.healthify.features.intake.domain.entity.LoggedMealNotice
import pe.edu.upc.healthify.features.intake.domain.entity.MealIdea
import pe.edu.upc.healthify.features.intake.domain.entity.MealIdeaIngredient
import pe.edu.upc.healthify.features.intake.domain.entity.MealIdeas
import pe.edu.upc.healthify.features.intake.domain.entity.MealLogOutcome
import pe.edu.upc.healthify.features.intake.domain.entity.PendingDiaryEntry
import pe.edu.upc.healthify.features.intake.domain.entity.PendingMealPhoto
import pe.edu.upc.healthify.features.intake.domain.valueobject.ClientEntryId
import pe.edu.upc.healthify.features.intake.domain.valueobject.DiaryEntryId
import pe.edu.upc.healthify.features.intake.domain.valueobject.EntryProvenance
import pe.edu.upc.healthify.features.intake.domain.valueobject.LocalTimestamp
import pe.edu.upc.healthify.features.intake.domain.valueobject.MealFood
import pe.edu.upc.healthify.features.intake.domain.valueobject.PlanAdherence
import pe.edu.upc.healthify.features.intake.presentation.state.DiaryEvent
import pe.edu.upc.healthify.features.intake.presentation.state.DiaryItem
import pe.edu.upc.healthify.features.intake.presentation.state.MealIdeasEvent
import pe.edu.upc.healthify.features.intake.presentation.state.MealIdeasFailure
import pe.edu.upc.healthify.testing.FakeAccountRepository
import pe.edu.upc.healthify.testing.FakeAiPreferencesRepository
import pe.edu.upc.healthify.testing.FakeConnectivityObserver
import pe.edu.upc.healthify.testing.FakeDiaryRepository
import pe.edu.upc.healthify.testing.FakeMealIdeasRepository
import pe.edu.upc.healthify.testing.FakeMealPhotoRepository
import pe.edu.upc.healthify.testing.FakeSessionRepository
import pe.edu.upc.healthify.testing.MainDispatcherRule
import pe.edu.upc.healthify.testing.TODAY
import pe.edu.upc.healthify.testing.aiPreferences
import pe.edu.upc.healthify.testing.failureOf
import pe.edu.upc.healthify.testing.fixedClock
import java.time.LocalDate
import java.time.OffsetDateTime
import pe.edu.upc.healthify.features.intake.application.usecase.ObserveSyncedDiaryEntriesUseCase

class DiaryAndIdeasViewModelsTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val diary = FakeDiaryRepository()
    private val photos = FakeMealPhotoRepository()
    private val preferences = FakeAiPreferencesRepository()
    private val connectivity = FakeConnectivityObserver()
    private val ideasRepository = FakeMealIdeasRepository()
    private val accounts = FakeAccountRepository()

    private val diaryViewModel by lazy {
        DiaryViewModel(
            ObserveCurrentUserUseCase(FakeSessionRepository()),
            GetDiaryDayUseCase(diary),
            ObservePendingDiaryEntriesUseCase(diary),
            ObservePendingMealPhotosUseCase(photos),
            ObserveSyncedDiaryEntriesUseCase(diary),
            ObserveLoggedMealNoticeUseCase(diary),
            ConsumeLoggedMealNoticeUseCase(diary),
            GetAiPreferencesUseCase(preferences),
            GetAccountCreatedOnUseCase(accounts),
            DiscardMealPhotoUseCase(photos),
            connectivity,
            fixedClock,
        )
    }

    private val ideasViewModel by lazy {
        MealIdeasViewModel(
            ObserveCurrentUserUseCase(FakeSessionRepository()),
            GetMealIdeasUseCase(ideasRepository),
            LogMealIdeaUseCase(diary),
            connectivity,
            fixedClock,
        )
    }

    // ----- PT14 · Diario -----

    @Test
    fun `PT14 shows the day with the entry to confirm first and the ideas card`() {
        diary.dayResult = { date -> Result.success(DiaryDay(date, listOf(entry(1, "07:30", confirmed = true), entry(2, "13:15", confirmed = false)))) }

        val state = diaryViewModel.state.value

        assertFalse(state.isLoading)
        assertEquals(listOf("entry-2", "entry-1"), state.items.map { it.key })
        assertTrue(state.showIdeasCard)
    }

    @Test
    fun `the ideas card needs the meal ideas preference`() {
        preferences.preferences = aiPreferences(ideas = false)

        assertFalse(diaryViewModel.state.value.showIdeasCard)
    }

    @Test
    fun `PT14_V an empty day is the invitation, not an error`() {
        assertTrue(diaryViewModel.state.value.isEmpty)
        assertFalse(diaryViewModel.state.value.loadFailed)
    }

    @Test
    fun `PT14_O offline shows the queued meals of the day as pending to send`() {
        diary.dayResult = { failureOf(DomainError.Network) }
        diary.pendingEntries.value = listOf(pending("2026-10-07T08:00:00Z"), pending("2026-10-05T08:00:00Z"))

        val state = diaryViewModel.state.value

        assertTrue(state.isOffline)
        assertEquals(1, state.items.count { it is DiaryItem.Pending })
        assertEquals(2, state.pendingCount)
        assertFalse(state.loadFailed)
    }

    @Test
    fun `a queued meal accepted after the reload shows up in the day`() {
        val serverEntries = mutableListOf<DiaryEntry>()
        diary.dayResult = { date -> Result.success(DiaryDay(date, serverEntries.toList())) }
        diary.pendingEntries.value = listOf(pending("2026-10-07T08:00:00Z"))
        connectivity.online.value = false
        diaryViewModel.state.value

        // Vuelve la conexión: el día se relee antes de que la cola llegue al servidor.
        connectivity.online.value = true
        assertEquals(listOf(DiaryItem.Pending::class), diaryViewModel.state.value.items.map { it::class })

        // La cola se envía: la comida sale de la cola y ya está en el servidor.
        serverEntries += entry(9, "08:00", confirmed = true)
        diary.pendingEntries.value = emptyList()
        diary.synced.tryEmit(Unit)

        assertEquals(listOf("entry-9"), diaryViewModel.state.value.items.map { it.key })
    }

    @Test
    fun `an account created today cannot go back to previous days`() {
        accounts.createdOn = Result.success(TODAY)
        val requested = mutableListOf<LocalDate>()
        diary.dayResult = { date -> requested += date; Result.success(DiaryDay(date, emptyList())) }

        assertFalse(diaryViewModel.state.value.canGoToPreviousDay)
        diaryViewModel.onPreviousDay()

        assertEquals(TODAY, diaryViewModel.state.value.selectedDate)
        assertEquals(listOf(TODAY), requested)
    }

    @Test
    fun `the diary goes back down to the day the account was created`() {
        accounts.createdOn = Result.success(TODAY.minusDays(1))

        assertTrue(diaryViewModel.state.value.canGoToPreviousDay)
        diaryViewModel.onPreviousDay()
        assertEquals(TODAY.minusDays(1), diaryViewModel.state.value.selectedDate)
        assertFalse(diaryViewModel.state.value.canGoToPreviousDay)
        diaryViewModel.onPreviousDay()
        assertEquals(TODAY.minusDays(1), diaryViewModel.state.value.selectedDate)
    }

    @Test
    fun `without the account date offline there is no limit and it is asked again online`() {
        accounts.createdOn = failureOf(DomainError.Network)
        connectivity.online.value = false

        assertTrue(diaryViewModel.state.value.canGoToPreviousDay)
        assertEquals(1, accounts.calls)

        accounts.createdOn = Result.success(TODAY)
        connectivity.online.value = true

        assertEquals(TODAY, diaryViewModel.state.value.earliestDate)
        assertFalse(diaryViewModel.state.value.canGoToPreviousDay)
    }

    @Test
    fun `the next day is never after today and the previous one loads`() {
        val requested = mutableListOf<LocalDate>()
        diary.dayResult = { date -> requested += date; Result.success(DiaryDay(date, emptyList())) }

        diaryViewModel.onNextDay()
        diaryViewModel.onPreviousDay()

        assertEquals(TODAY.minusDays(1), diaryViewModel.state.value.selectedDate)
        assertFalse(diaryViewModel.state.value.showIdeasCard)
        assertEquals(listOf(TODAY, TODAY.minusDays(1)), requested)
    }

    @Test
    fun `a logged meal shows its Snackbar once`() = runTest {
        diaryViewModel.state.value
        diary.publishNotice(LoggedMealNotice("Lomo saltado", 320.0, MealLogOutcome.LOGGED))

        diaryViewModel.events.test {
            assertEquals(DiaryEvent.ShowLoggedMeal(LoggedMealNotice("Lomo saltado", 320.0, MealLogOutcome.LOGGED)), awaitItem())
        }
        diary.loggedMealNotice.test { assertNull(awaitItem()) }
    }

    @Test
    fun `a photo taken offline shows on its day and logging it by hand deletes it`() {
        val photo = PendingMealPhoto(
            id = "pending-1",
            filePath = "/cache/meal_photos/a.jpg",
            capturedAt = LocalTimestamp.restore(OffsetDateTime.parse("2026-10-07T08:00:00Z")),
        )
        photos.pending.value = listOf(photo)
        assertEquals("photo-pending-1", diaryViewModel.state.value.items.first().key)

        diaryViewModel.onRegisterPendingPhotoManually(photo)

        assertEquals(listOf("/cache/meal_photos/a.jpg"), photos.discarded)
        assertTrue(diaryViewModel.state.value.items.none { it is DiaryItem.PendingPhoto })
    }

    // ----- PT14.4 / PT14.5 · Ideas -----

    @Test
    fun `PT14_4 shows the ideas and «Ver otras ideas» excludes the ones seen`() {
        ideasRepository.results = listOf(Result.success(ideas("a", "b")), Result.success(ideas("c", "d")))

        assertEquals(listOf("a", "b"), ideasViewModel.state.value.ideas!!.ideas.map { it.id })
        ideasViewModel.onMoreIdeas()

        assertEquals(listOf(emptyList(), listOf("a", "b")), ideasRepository.requests)
        assertEquals(listOf("c", "d"), ideasViewModel.state.value.ideas!!.ideas.map { it.id })
    }

    @Test
    fun `PT14_4_O offline is its own state, PT14_4_E is the generic error`() {
        ideasRepository.results = listOf(failureOf(DomainError.Network))
        assertTrue(ideasViewModel.state.value.isOffline)
        assertNull(ideasViewModel.state.value.failure)

        ideasRepository.results = listOf(failureOf(DomainError.Unexpected("AiOutputRejected")))
        ideasViewModel.onRetry()
        assertEquals(MealIdeasFailure.GENERIC, ideasViewModel.state.value.failure)
    }

    @Test
    fun `not enough left today is a kind message`() {
        ideasRepository.results = listOf(failureOf(DomainError.Validation("NotEnoughRemaining")))

        assertEquals(MealIdeasFailure.NOT_ENOUGH_REMAINING, ideasViewModel.state.value.failure)
    }

    @Test
    fun `registering an idea sends its resolved ingredients and retries with the same ids`() = runTest {
        ideasRepository.results = listOf(Result.success(ideas("a")))
        diary.logResult = failureOf(DomainError.Unexpected("HTTP_500"))
        ideasViewModel.onIdeaSelected("a")

        ideasViewModel.onRegisterIdea()
        assertTrue(ideasViewModel.state.value.showRegisterError)

        diary.logResult = Result.success(MealLogOutcome.LOGGED)
        ideasViewModel.onRegisterIdea()

        assertEquals(diary.groups[0].items.map { it.clientEntryId }, diary.groups[1].items.map { it.clientEntryId })
        assertEquals(1, diary.groups[1].items.size)
        ideasViewModel.events.test { assertEquals(MealIdeasEvent.Logged(MealLogOutcome.LOGGED), awaitItem()) }
    }

    @Test
    fun `back from the detail returns to the list`() {
        ideasRepository.results = listOf(Result.success(ideas("a")))
        ideasViewModel.onIdeaSelected("a")

        assertTrue(ideasViewModel.onBack())
        assertNull(ideasViewModel.state.value.selectedIdeaId)
        assertFalse(ideasViewModel.onBack())
    }

    private fun ideas(vararg ids: String) = MealIdeas(
        localDate = TODAY,
        remainingKcal = 590.0,
        restrictions = emptyList(),
        ideas = ids.map { id ->
            MealIdea(
                id = id,
                name = "Idea $id",
                energyKcal = 500.0,
                proteinG = 30.0,
                carbG = 50.0,
                fatG = 10.0,
                ingredients = listOf(
                    MealIdeaIngredient("Pollo", 150.0, MealFood(1, "Pollo")),
                    MealIdeaIngredient("Aceite", 5.0, null),
                ),
                why = "",
            )
        },
    )

    private fun entry(id: Long, time: String, confirmed: Boolean) = DiaryEntry(
        id = DiaryEntryId(id),
        localTimestamp = LocalTimestamp.restore(OffsetDateTime.parse("2026-10-07T$time:00Z")),
        provenance = EntryProvenance.PHOTO,
        foodName = "Comida $id",
        proposedFoodId = 3,
        proposedGrams = 280.0,
        confidence = 0.64,
        confirmedFoodId = if (confirmed) 3 else null,
        confirmedGrams = if (confirmed) 280.0 else null,
        planAdherence = if (confirmed) PlanAdherence.IN_PLAN else PlanAdherence.NOT_ANSWERED,
        isCountedTowardsTargets = confirmed,
    )

    private fun pending(timestamp: String) = PendingDiaryEntry(
        clientEntryId = ClientEntryId.random(),
        localTimestamp = LocalTimestamp.restore(OffsetDateTime.parse(timestamp)),
        provenance = EntryProvenance.MANUAL,
        foodName = "Avena",
        grams = 250.0,
        confidence = null,
        confirmed = true,
        planAdherence = PlanAdherence.IN_PLAN,
    )
}
