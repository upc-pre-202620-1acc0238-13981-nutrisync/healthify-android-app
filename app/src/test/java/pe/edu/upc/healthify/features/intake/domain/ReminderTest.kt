package pe.edu.upc.healthify.features.intake.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import pe.edu.upc.healthify.features.iam.application.usecase.ObserveCurrentUserUseCase
import pe.edu.upc.healthify.features.intake.application.usecase.ChangeReminderUseCase
import pe.edu.upc.healthify.features.intake.application.usecase.ObserveReminderSettingsUseCase
import pe.edu.upc.healthify.features.intake.domain.entity.ReminderKind
import pe.edu.upc.healthify.features.intake.domain.entity.ReminderSettings
import pe.edu.upc.healthify.features.intake.domain.repository.ReminderScheduler
import pe.edu.upc.healthify.features.intake.domain.repository.ReminderSettingsRepository
import pe.edu.upc.healthify.features.intake.presentation.viewmodel.RemindersViewModel
import pe.edu.upc.healthify.testing.FakeSessionRepository
import pe.edu.upc.healthify.testing.MainDispatcherRule
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

/** PT22 · recordatorios locales: horas del frame, guardado al vuelo y programación del aviso. */
class ReminderTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val lima = ZoneId.of("America/Lima")

    private class FakeReminderSettings : ReminderSettingsRepository {
        val byUser = MutableStateFlow<Map<Long, ReminderSettings>>(emptyMap())
        override fun observe(patientUserId: Long): Flow<ReminderSettings> =
            byUser.map { it[patientUserId] ?: ReminderSettings() }
        override suspend fun get(patientUserId: Long) = byUser.value[patientUserId] ?: ReminderSettings()
        override suspend fun save(patientUserId: Long, settings: ReminderSettings) {
            byUser.value = byUser.value + (patientUserId to settings)
        }
    }

    private class FakeScheduler : ReminderScheduler {
        val updates = mutableListOf<Pair<ReminderKind, Boolean>>()
        override fun update(kind: ReminderKind, enabled: Boolean) {
            updates += kind to enabled
        }
    }

    @Test
    fun `the next reminder is today if its hour has not passed, otherwise tomorrow`() {
        val morning = ZonedDateTime.of(2026, 10, 7, 6, 0, 0, 0, lima)
        val afternoon = ZonedDateTime.of(2026, 10, 7, 14, 0, 0, 0, lima)
        val weighIn = LocalTime.of(7, 30)

        assertEquals(ZonedDateTime.of(2026, 10, 7, 7, 30, 0, 0, lima), ReminderSettings.nextOccurrence(weighIn, morning))
        assertEquals(ZonedDateTime.of(2026, 10, 8, 7, 30, 0, 0, lima), ReminderSettings.nextOccurrence(weighIn, afternoon))
        // Justo a la hora ya cuenta como pasada.
        val exactly = ZonedDateTime.of(2026, 10, 7, 7, 30, 0, 0, lima)
        assertEquals(exactly.plusDays(1), ReminderSettings.nextOccurrence(weighIn, exactly))
    }

    @Test
    fun `reminders start off and use the hours of the frame`() {
        assertFalse(ReminderSettings().selfWeighInEnabled)
        assertFalse(ReminderSettings().mealsEnabled)
        assertEquals(listOf(LocalTime.of(7, 30)), ReminderKind.SELF_WEIGH_IN.times)
        assertEquals(listOf(LocalTime.of(13, 0), LocalTime.of(20, 0)), ReminderKind.MEALS.times)
    }

    @Test
    fun `turning a reminder on saves it for that patient and schedules it once`() = runTest {
        val settings = FakeReminderSettings()
        val scheduler = FakeScheduler()
        val change = ChangeReminderUseCase(settings, scheduler)

        change(12, ReminderKind.MEALS, true)
        change(12, ReminderKind.MEALS, true)
        change(12, ReminderKind.MEALS, false)

        assertEquals(listOf(ReminderKind.MEALS to true, ReminderKind.MEALS to false), scheduler.updates)
        assertFalse(settings.get(12).mealsEnabled)
        assertFalse(settings.get(99).mealsEnabled)
    }

    @Test
    fun `PT22 shows the saved settings and saves each switch on the fly`() = runTest {
        val settings = FakeReminderSettings().apply { byUser.value = mapOf(12L to ReminderSettings(selfWeighInEnabled = true)) }
        val scheduler = FakeScheduler()
        val viewModel = RemindersViewModel(
            observeCurrentUser = ObserveCurrentUserUseCase(FakeSessionRepository()),
            observeReminderSettings = ObserveReminderSettingsUseCase(settings),
            changeReminder = ChangeReminderUseCase(settings, scheduler),
        )
        assertTrue(viewModel.state.value.settings.selfWeighInEnabled)

        viewModel.onToggle(ReminderKind.MEALS, true)
        assertTrue(settings.get(12).mealsEnabled)

        viewModel.onPermissionDenied()
        assertTrue(viewModel.state.value.showPermissionDialog)
        viewModel.onDismissPermissionDialog()
        assertFalse(viewModel.state.value.showPermissionDialog)
    }
}
