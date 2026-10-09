package pe.edu.upc.healthify.features.monitoring.presentation.viewmodel

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import pe.edu.upc.healthify.core.common.error.DomainError
import pe.edu.upc.healthify.features.monitoring.application.usecase.GetMonitoringPanelUseCase
import pe.edu.upc.healthify.features.monitoring.application.usecase.GetMonitoringSummaryUseCase
import pe.edu.upc.healthify.features.monitoring.domain.entity.MonitoringSummary
import pe.edu.upc.healthify.features.monitoring.domain.entity.PanelEntryProvenance
import pe.edu.upc.healthify.features.monitoring.domain.valueobject.ComplianceOutcome
import pe.edu.upc.healthify.features.monitoring.infrastructure.mapper.toDomain
import pe.edu.upc.healthify.features.monitoring.infrastructure.remote.dto.PatientMonitoringPanelDto
import pe.edu.upc.healthify.testing.FakeConnectivityObserver
import pe.edu.upc.healthify.testing.FakePractitionerMonitoringRepository
import pe.edu.upc.healthify.testing.MainDispatcherRule
import pe.edu.upc.healthify.testing.failureOf
import pe.edu.upc.healthify.testing.fixedClock
import pe.edu.upc.healthify.testing.patientArgs
import java.time.LocalDate

class PatientFollowUpViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakePractitionerMonitoringRepository()

    private fun viewModel() = PatientFollowUpViewModel(
        savedStateHandle = patientArgs(),
        getMonitoringPanel = GetMonitoringPanelUseCase(repository, fixedClock()),
        getMonitoringSummary = GetMonitoringSummaryUseCase(repository),
        connectivityObserver = FakeConnectivityObserver(),
    )

    @Test
    fun `PAC-2 asks for the 7 days up to today and shows the AI summary`() {
        val state = viewModel().state.value

        assertEquals(listOf(LocalDate.parse("2026-09-15")), repository.panelDates)
        assertEquals("Cumple sus metas casi todos los días.", state.aiSummary)
    }

    @Test
    fun `PAC-2 without the AI summary still shows the panel`() {
        repository.summaryResult = failureOf(DomainError.Unexpected("AiProviderUnavailable"))
        val state = viewModel().state.value
        assertNull(state.aiSummary)
        assertTrue(state.panel != null)

        repository.summaryResult = Result.success(MonitoringSummary(null))
        assertNull(viewModel().state.value.aiSummary)
    }

    @Test
    fun `panel mapper keeps unlogged apart and never turns an unknown outcome into a short day`() {
        val panel = Json { ignoreUnknownKeys = true }.decodeFromString<PatientMonitoringPanelDto>(
            """
            { "date": "2026-10-07",
              "week": { "from": "2026-10-01", "to": "2026-10-07", "metDays": 1, "totalDays": 7,
                "days": [ { "date": "2026-10-02", "outcome": "Unlogged" }, { "date": "2026-10-01", "outcome": "Met" },
                          { "date": "2026-10-03", "outcome": "Weird" }, { "date": "2026-10-04", "outcome": "Short" } ] },
              "diary": [ { "diaryEntryId": 1, "localTimestamp": "2026-10-07T13:15:00-05:00", "provenance": "Photo",
                           "proposedFoodName": "Ceviche", "proposedPortionGrams": 280 } ] }
            """.trimIndent(),
        ).toDomain()

        assertEquals(
            listOf(ComplianceOutcome.MET, ComplianceOutcome.UNLOGGED, ComplianceOutcome.UNLOGGED, ComplianceOutcome.SHORT),
            panel.week!!.days.map { it.outcome },
        )
        val entry = panel.diary.single()
        assertEquals(PanelEntryProvenance.PHOTO, entry.provenance)
        assertTrue(entry.isAwaitingConfirmation)
        assertEquals("Ceviche", entry.foodName)
    }
}
