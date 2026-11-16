package pe.edu.upc.healthify.features.main

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.foundation.layout.size
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import pe.edu.upc.healthify.R
import pe.edu.upc.healthify.core.designsystem.theme.HealthifyTheme
import pe.edu.upc.healthify.features.main.presentation.screen.PatientDetailTab
import pe.edu.upc.healthify.features.main.presentation.screen.PractitionerPatientContent

/**
 * The patient file's tab row: the four tabs are all there and evenly spaced, so a short label like «Plan» is not left far
 * from the others (it used to sit in a quarter of the width of its own).
 */
class PatientTabRowTest {

    @get:Rule
    val compose = createComposeRule()

    private fun string(id: Int): String = InstrumentationRegistry.getInstrumentation().targetContext.getString(id)

    private fun show(selected: PatientDetailTab = PatientDetailTab.FollowUp, onSelect: (PatientDetailTab) -> Unit = {}) {
        compose.setContent {
            HealthifyTheme {
                PractitionerPatientContent(
                    patientName = "Ana Flores",
                    selectedTab = selected,
                    onSelectTab = onSelect,
                    onBack = {},
                    snackbarHostState = remember { SnackbarHostState() },
                    modifier = Modifier.size(360.dp, 640.dp),
                ) {}
            }
        }
    }

    @Test
    fun the_four_tabs_are_displayed_on_a_small_phone() {
        show()

        listOf(
            R.string.patient_tab_summary,
            R.string.patient_tab_follow_up,
            R.string.patient_tab_record,
            R.string.patient_tab_plan,
        ).forEach { compose.onNodeWithText(string(it)).assertIsDisplayed() }
    }

    @Test
    fun the_selected_tab_is_marked_and_tapping_another_selects_it() {
        var selected: PatientDetailTab? = null
        show(onSelect = { selected = it })

        compose.onNodeWithText(string(R.string.patient_tab_follow_up)).assertIsSelected()
        compose.onNodeWithText(string(R.string.patient_tab_plan)).performClick()

        assertEquals(PatientDetailTab.Plan, selected)
    }

    @Test
    fun the_gaps_between_the_labels_are_even() {
        show()

        val summary = compose.onNodeWithText(string(R.string.patient_tab_summary)).getUnclippedBoundsInRoot()
        val followUp = compose.onNodeWithText(string(R.string.patient_tab_follow_up)).getUnclippedBoundsInRoot()
        val record = compose.onNodeWithText(string(R.string.patient_tab_record)).getUnclippedBoundsInRoot()
        val plan = compose.onNodeWithText(string(R.string.patient_tab_plan)).getUnclippedBoundsInRoot()

        val gaps = listOf(
            followUp.left.value - summary.right.value,
            record.left.value - followUp.right.value,
            plan.left.value - record.right.value,
        )
        // Same distance (within 2 dp of each other) between every pair of neighbours; none is stuck to its neighbour.
        assertTrue("Uneven gaps: $gaps", gaps.max() - gaps.min() <= 2f)
        assertTrue("A label touches its neighbour: $gaps", gaps.min() >= 8f)
    }

    @Test
    fun the_last_tab_stays_inside_the_screen() {
        show()

        val plan = compose.onNodeWithText(string(R.string.patient_tab_plan)).getUnclippedBoundsInRoot()

        assertTrue("Plan is cut off at ${plan.right}", plan.right.value <= 360f)
    }
}
