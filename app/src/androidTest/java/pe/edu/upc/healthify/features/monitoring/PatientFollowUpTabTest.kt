package pe.edu.upc.healthify.features.monitoring

import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import pe.edu.upc.healthify.R
import pe.edu.upc.healthify.core.designsystem.theme.HealthifyTheme
import pe.edu.upc.healthify.features.monitoring.domain.entity.DaysRatio
import pe.edu.upc.healthify.features.monitoring.domain.entity.PanelClinicalMeasurement
import pe.edu.upc.healthify.features.monitoring.domain.entity.PanelDay
import pe.edu.upc.healthify.features.monitoring.domain.entity.PanelDiaryEntry
import pe.edu.upc.healthify.features.monitoring.domain.entity.PanelEntryProvenance
import pe.edu.upc.healthify.features.monitoring.domain.entity.PanelProtocolCheck
import pe.edu.upc.healthify.features.monitoring.domain.entity.PanelWeek
import pe.edu.upc.healthify.features.monitoring.domain.entity.PanelWeightTrend
import pe.edu.upc.healthify.features.monitoring.domain.entity.PatientMonitoringPanel
import pe.edu.upc.healthify.features.monitoring.domain.valueobject.ComplianceOutcome
import pe.edu.upc.healthify.features.monitoring.presentation.screen.PatientFollowUpContent
import pe.edu.upc.healthify.features.monitoring.presentation.state.PatientFollowUpUiState
import java.time.Instant
import java.time.LocalDate

/**
 * The Follow-up tab after the compact redesign, on a small phone (360 × 640 dp): the six sections stay reachable, the
 * legend lists the four outcomes, the accordions open and close, and the empty, loading, error and offline states of
 * the tab still render.
 */
class PatientFollowUpTabTest {

    @get:Rule
    val compose = createComposeRule()

    private fun string(id: Int, vararg args: Any): String =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(id, *args)

    private fun panel(
        outcomes: List<ComplianceOutcome> = listOf(
            ComplianceOutcome.MET, ComplianceOutcome.MET, ComplianceOutcome.MET, ComplianceOutcome.SHORT,
            ComplianceOutcome.MET, ComplianceOutcome.UNLOGGED, ComplianceOutcome.EXCEEDED,
        ),
        diary: List<PanelDiaryEntry> = listOf(
            PanelDiaryEntry(1, Instant.parse("2026-10-07T18:15:00Z"), "Lomo saltado", 300.0, PanelEntryProvenance.PHOTO, false),
            PanelDiaryEntry(2, Instant.parse("2026-10-07T19:10:00Z"), "Ceviche", 280.0, PanelEntryProvenance.PHOTO, true),
        ),
        weightTrend: PanelWeightTrend? = PanelWeightTrend(4, -0.3),
        loggedDays: DaysRatio? = DaysRatio(6, 7),
        measurement: PanelClinicalMeasurement? = PanelClinicalMeasurement(
            Instant.parse("2026-09-03T15:00:00Z"),
            74.2,
            listOf(PanelProtocolCheck.FASTING, PanelProtocolCheck.NO_SHOES),
        ),
    ) = PatientMonitoringPanel(
        date = LocalDate.parse("2026-10-07"),
        week = PanelWeek(
            days = outcomes.mapIndexed { index, outcome ->
                PanelDay(LocalDate.parse("2026-09-28").plusDays(index.toLong()), outcome)
            },
            metDays = outcomes.count { it == ComplianceOutcome.MET },
            totalDays = outcomes.size,
        ),
        loggedDays = loggedDays,
        weightTrend = weightTrend,
        diary = diary,
        lastClinicalMeasurement = measurement,
    )

    private fun show(state: PatientFollowUpUiState, onRetry: () -> Unit = {}) {
        compose.setContent {
            HealthifyTheme {
                // The smallest phone the nutritionist uses.
                PatientFollowUpContent(state = state, onRetry = onRetry, modifier = Modifier.size(360.dp, 640.dp))
            }
        }
    }

    private val ai = "Cumple sus metas casi todos los días. El jueves registró menos energía de la indicada."

    @Test
    fun the_six_sections_are_reachable_on_a_small_phone() {
        show(PatientFollowUpUiState(isLoading = false, panel = panel(), aiSummary = ai))

        // 1. Weekly compliance
        compose.onNodeWithText(string(R.string.follow_up_this_week)).assertIsDisplayed()
        // 2. Weight trend, 3. Logging, 6. Last clinical measurement: the indicator row
        compose.onNodeWithText(string(R.string.follow_up_kpi_weight)).assertIsDisplayed()
        compose.onNodeWithText(string(R.string.follow_up_logging)).assertIsDisplayed()
        compose.onNodeWithText(string(R.string.follow_up_kpi_measurement)).assertIsDisplayed()
        // 4. AI summary
        compose.onNodeWithText(string(R.string.follow_up_ai_badge)).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText(ai).performScrollTo().assertIsDisplayed()
        // 5. Today's diary
        compose.onNodeWithText(string(R.string.follow_up_diary_today)).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Ceviche · 280 g").performScrollTo().assertIsDisplayed()
        // The closing note is the end of the scroll: nothing is cut off before it.
        compose.onNodeWithText(string(R.string.follow_up_read_only)).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun the_indicators_show_their_values() {
        show(PatientFollowUpUiState(isLoading = false, panel = panel(), aiSummary = ai))

        compose.onNodeWithText(string(R.string.follow_up_kpi_ratio, 6, 7)).assertIsDisplayed()
        compose.onNodeWithText(string(R.string.follow_up_kpi_weight_caption, 4)).assertIsDisplayed()
        // The protocol of the measurement (fasting, no shoes) is not lost: it is a caption under the indicator row.
        compose.onNode(hasText(string(R.string.protocol_fasting), substring = true, ignoreCase = true))
            .assertIsDisplayed()
    }

    @Test
    fun the_legend_always_lists_the_four_outcomes_even_when_the_week_has_one() {
        val onlyOne = panel(outcomes = List(7) { ComplianceOutcome.UNLOGGED })
        show(PatientFollowUpUiState(isLoading = false, panel = onlyOne))

        listOf(
            R.string.follow_up_legend_met,
            R.string.follow_up_legend_exceeded,
            R.string.follow_up_legend_short,
            R.string.follow_up_legend_unlogged,
        ).forEach { compose.onNodeWithText(string(it)).assertIsDisplayed() }
    }

    @Test
    fun the_ai_accordion_collapses_and_opens_again() {
        show(PatientFollowUpUiState(isLoading = false, panel = panel(), aiSummary = ai))
        compose.onNodeWithText(ai).assertIsDisplayed()

        compose.onNodeWithText(string(R.string.follow_up_ai_badge)).performClick()
        compose.onNodeWithText(ai).assertDoesNotExist()
        // The badge stays: the section is still there, only folded.
        compose.onNodeWithText(string(R.string.follow_up_ai_badge)).assertIsDisplayed()

        compose.onNodeWithText(string(R.string.follow_up_ai_badge)).performClick()
        compose.onNodeWithText(ai).assertIsDisplayed()
    }

    @Test
    fun the_diary_accordion_keeps_the_pending_count_when_folded() {
        show(PatientFollowUpUiState(isLoading = false, panel = panel(), aiSummary = ai))
        val pending = InstrumentationRegistry.getInstrumentation().targetContext.resources
            .getQuantityString(R.plurals.follow_up_pending_count, 1, 1)

        compose.onNodeWithText(string(R.string.follow_up_diary_today)).performScrollTo().performClick()

        compose.onNodeWithText("Ceviche · 280 g").assertDoesNotExist()
        compose.onNodeWithText(pending).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun a_day_without_meals_says_so_and_a_missing_measurement_shows_a_dash() {
        show(
            PatientFollowUpUiState(
                isLoading = false,
                panel = panel(diary = emptyList(), weightTrend = null, loggedDays = null, measurement = null),
            ),
        )

        compose.onNodeWithText(string(R.string.follow_up_diary_empty)).performScrollTo().assertIsDisplayed()
        compose.onAllNodesWithTextCount(string(R.string.value_missing), 3)
    }

    private fun androidx.compose.ui.test.junit4.ComposeContentTestRule.onAllNodesWithTextCount(text: String, expected: Int) {
        val found = onAllNodes(hasText(text)).fetchSemanticsNodes().size
        check(found == expected) { "Expected $expected nodes with '$text' but found $found" }
    }

    @Test
    fun the_ai_card_is_hidden_when_there_is_no_summary() {
        show(PatientFollowUpUiState(isLoading = false, panel = panel(), aiSummary = null))

        compose.onNodeWithText(string(R.string.follow_up_ai_badge)).assertDoesNotExist()
        compose.onNodeWithText(string(R.string.follow_up_diary_today)).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun the_loading_state_shows_no_sections() {
        show(PatientFollowUpUiState())

        compose.onNodeWithText(string(R.string.follow_up_this_week)).assertDoesNotExist()
        compose.onNodeWithText(string(R.string.follow_up_diary_today)).assertDoesNotExist()
    }

    @Test
    fun the_error_state_offers_retry() {
        var retried = 0
        show(PatientFollowUpUiState(isLoading = false, loadFailed = true), onRetry = { retried++ })

        compose.onNodeWithText(string(R.string.ds_retry)).assertIsDisplayed().performClick()

        check(retried == 1) { "Retry was not called" }
    }

    @Test
    fun the_offline_state_explains_and_offers_retry() {
        show(PatientFollowUpUiState(isLoading = false, isOffline = true, loadFailed = true))

        compose.onNodeWithText(string(R.string.roster_offline_title)).assertIsDisplayed()
        compose.onNodeWithText(string(R.string.ds_retry)).assertIsDisplayed()
    }
}
