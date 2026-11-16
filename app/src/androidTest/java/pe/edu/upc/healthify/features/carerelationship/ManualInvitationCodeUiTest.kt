package pe.edu.upc.healthify.features.carerelationship

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import pe.edu.upc.healthify.R
import pe.edu.upc.healthify.core.designsystem.theme.HealthifyTheme
import pe.edu.upc.healthify.features.carerelationship.domain.valueobject.InvitationToken
import pe.edu.upc.healthify.features.carerelationship.presentation.screen.InvitePatientContent
import pe.edu.upc.healthify.features.carerelationship.presentation.screen.ScanInvitationActions
import pe.edu.upc.healthify.features.carerelationship.presentation.screen.ScanInvitationContent
import pe.edu.upc.healthify.features.carerelationship.presentation.state.CameraPermissionState
import pe.edu.upc.healthify.features.carerelationship.presentation.state.InvitePatientUiState
import pe.edu.upc.healthify.features.carerelationship.presentation.state.ManualCode
import pe.edu.upc.healthify.features.carerelationship.presentation.state.ScanInvitationError
import pe.edu.upc.healthify.features.carerelationship.presentation.state.ScanInvitationUiState
import java.time.Duration

/**
 * Typing the invitation code by hand: the entry under the viewfinder, the sheet with its field and button in each state
 * (empty, valid, rejected, linking up), and the nutritionist's side (the code as text with Copy and Share). The QR
 * screen keeps its camera, prompt and hint.
 */
class ManualInvitationCodeUiTest {

    @get:Rule
    val compose = createComposeRule()

    private fun string(id: Int, vararg args: Any): String =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(id, *args)

    private val token = "kP3x9Qm2LrT7vB8nZc4WdA"

    private fun scan(state: ScanInvitationUiState, actions: ScanInvitationActions = ScanInvitationActions()) {
        compose.setContent { HealthifyTheme { ScanInvitationContent(state = state, actions = actions) } }
    }

    private fun granted(
        showManualSheet: Boolean = false,
        manualCode: String = "",
        manualError: ScanInvitationError? = null,
        isRedeeming: Boolean = false,
        isOffline: Boolean = false,
    ) = ScanInvitationUiState(
        cameraPermission = CameraPermissionState.Granted,
        showManualSheet = showManualSheet,
        manualCode = ManualCode(manualCode),
        manualError = manualError,
        isRedeeming = isRedeeming,
        isOffline = isOffline,
    )

    // ------------------------------------------------------------------ the scan screen

    @Test
    fun the_scan_screen_keeps_its_prompt_and_hint_and_offers_typing_the_code() {
        var opened = 0
        scan(granted(), ScanInvitationActions(onManualEntry = { opened++ }))

        compose.onNodeWithText(string(R.string.scan_invitation_prompt)).assertIsDisplayed()
        compose.onNodeWithText(string(R.string.scan_invitation_hint)).assertIsDisplayed()
        compose.onNodeWithText(string(R.string.scan_invitation_enter_manually)).assertIsDisplayed().performClick()
        check(opened == 1) { "The manual entry was not requested" }
    }

    @Test
    fun the_old_no_camera_text_is_gone() {
        scan(granted())

        compose.onNodeWithText("No tengo cámara disponible").assertDoesNotExist()
    }

    @Test
    fun the_entry_is_disabled_offline_with_the_banner_as_before() {
        scan(granted(isOffline = true))

        compose.onNodeWithText(string(R.string.scan_invitation_enter_manually)).assertIsNotEnabled()
    }

    @Test
    fun a_qr_rejection_still_shows_under_the_viewfinder() {
        scan(granted().copy(error = ScanInvitationError.Expired))

        compose.onNodeWithText(string(R.string.scan_invitation_error_expired)).assertIsDisplayed()
    }

    // ------------------------------------------------------------------ the sheet

    @Test
    fun the_sheet_starts_empty_with_the_minimum_length_hint_and_Link_up_disabled() {
        scan(granted(showManualSheet = true))

        compose.onNodeWithText(string(R.string.scan_invitation_manual_title)).assertIsDisplayed()
        compose.onNodeWithText(string(R.string.scan_invitation_manual_label)).assertIsDisplayed()
        compose.onNodeWithText(string(R.string.scan_invitation_manual_helper_short, 0, InvitationToken.MIN_LENGTH))
            .assertIsDisplayed()
        compose.onNodeWithText(string(R.string.scan_invitation_manual_submit)).assertIsNotEnabled()
    }

    @Test
    fun a_short_code_says_how_many_characters_are_missing_and_keeps_Link_up_disabled() {
        scan(granted(showManualSheet = true, manualCode = "kP3x 9Qm2"))

        compose.onNodeWithText(string(R.string.scan_invitation_manual_helper_short, 8, InvitationToken.MIN_LENGTH))
            .assertIsDisplayed()
        compose.onNodeWithText(string(R.string.scan_invitation_manual_submit)).assertIsNotEnabled()
    }

    @Test
    fun a_valid_code_enables_Link_up_and_sends_a_click() {
        var submitted = 0
        scan(
            granted(showManualSheet = true, manualCode = token),
            ScanInvitationActions(onManualSubmit = { submitted++ }),
        )

        compose.onNodeWithText(string(R.string.scan_invitation_manual_helper)).assertIsDisplayed()
        compose.onNodeWithText(string(R.string.scan_invitation_manual_submit)).assertIsEnabled().performClick()
        check(submitted == 1) { "Link up was not requested" }
    }

    @Test
    fun a_character_a_token_never_has_is_flagged_right_away() {
        scan(granted(showManualSheet = true, manualCode = "kP3x9Qm2LrT7vB8nZc4WdA!"))

        compose.onNodeWithText(string(R.string.scan_invitation_manual_error_characters)).assertIsDisplayed()
        compose.onNodeWithText(string(R.string.scan_invitation_manual_submit)).assertIsNotEnabled()
    }

    @Test
    fun too_long_a_code_is_flagged() {
        scan(granted(showManualSheet = true, manualCode = "a".repeat(70)))

        compose.onNodeWithText(string(R.string.scan_invitation_manual_error_too_long, InvitationToken.MAX_LENGTH))
            .assertIsDisplayed()
    }

    private fun rejected(error: ScanInvitationError, message: Int) {
        scan(granted(showManualSheet = true, manualCode = token, manualError = error))

        compose.onNodeWithText(string(message)).assertIsDisplayed()
        // The typed code stays so it can be fixed.
        compose.onNodeWithText(string(R.string.scan_invitation_manual_label)).assertIsDisplayed()
    }

    @Test
    fun a_used_code_shows_its_message_in_the_sheet() =
        rejected(ScanInvitationError.AlreadyUsed, R.string.scan_invitation_error_used)

    @Test
    fun an_expired_code_shows_its_message_in_the_sheet() =
        rejected(ScanInvitationError.Expired, R.string.scan_invitation_error_expired)

    @Test
    fun an_invalid_code_shows_its_message_in_the_sheet() =
        rejected(ScanInvitationError.InvalidFormat, R.string.scan_invitation_error_invalid)

    @Test
    fun an_unknown_invitation_shows_its_message_in_the_sheet() =
        rejected(ScanInvitationError.NotFound, R.string.scan_invitation_error_not_found)

    @Test
    fun a_connection_failure_shows_the_network_message_in_the_sheet() =
        rejected(ScanInvitationError.Network, R.string.error_network)

    @Test
    fun while_linking_up_the_button_announces_loading() {
        scan(granted(showManualSheet = true, manualCode = token, isRedeeming = true))

        compose.onNodeWithContentDescription(string(R.string.ds_cd_loading)).assertIsDisplayed()
    }

    @Test
    fun offline_the_sheet_explains_it_and_does_not_allow_linking_up() {
        scan(granted(showManualSheet = true, manualCode = token, isOffline = true))

        compose.onNodeWithText(string(R.string.error_network)).assertIsDisplayed()
        compose.onNodeWithText(string(R.string.scan_invitation_manual_submit)).assertIsNotEnabled()
    }

    @Test
    fun typing_reports_every_edit() {
        var last = ""
        scan(
            granted(showManualSheet = true),
            ScanInvitationActions(onManualCodeChange = { last = it }),
        )

        compose.onNode(hasText("", substring = true) and androidx.compose.ui.test.hasSetTextAction())
            .performTextInput("kP3x")

        check(last == "kP3x") { "Expected the typed text but got '$last'" }
    }

    // ------------------------------------------------------------------ the nutritionist's side

    private val issued = InvitationToken(token)

    private fun invite(state: InvitePatientUiState) {
        compose.setContent {
            HealthifyTheme {
                InvitePatientContent(state = state, onBack = {}, onValidityChange = {}, onGenerateAnother = {})
            }
        }
    }

    @Test
    fun the_invitation_shows_the_code_in_blocks_of_four_with_copy_and_share() {
        invite(InvitePatientUiState(isGenerating = false, token = issued, remaining = Duration.ofHours(1)))

        compose.onNodeWithText(string(R.string.invite_code_label)).assertIsDisplayed()
        compose.onNodeWithText("kP3x 9Qm2 LrT7 vB8n Zc4W dA").assertIsDisplayed()
        compose.onNodeWithText(string(R.string.invite_code_copy)).assertIsEnabled()
        compose.onNodeWithText(string(R.string.invite_code_share)).assertIsEnabled()
        // The rest of the screen is as it was.
        compose.onNodeWithText(string(R.string.invite_generate_another)).assertIsDisplayed()
    }

    @Test
    fun an_expired_invitation_cannot_be_copied_or_shared() {
        invite(InvitePatientUiState(isGenerating = false, token = issued, remaining = Duration.ZERO, isExpired = true))

        compose.onNodeWithText(string(R.string.invite_code_copy)).assertIsNotEnabled()
        compose.onNodeWithText(string(R.string.invite_code_share)).assertIsNotEnabled()
        compose.onNodeWithText(string(R.string.invite_expired)).assertIsDisplayed()
    }

    @Test
    fun while_the_code_is_being_generated_no_code_text_is_shown() {
        invite(InvitePatientUiState())

        compose.onNodeWithText(string(R.string.invite_code_label)).assertDoesNotExist()
        compose.onNodeWithText(string(R.string.invite_generating)).assertIsDisplayed()
    }
}
