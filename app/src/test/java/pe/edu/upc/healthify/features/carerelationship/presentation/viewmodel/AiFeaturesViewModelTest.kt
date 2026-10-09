package pe.edu.upc.healthify.features.carerelationship.presentation.viewmodel

import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import pe.edu.upc.healthify.core.common.error.DomainError
import pe.edu.upc.healthify.core.common.error.domainErrorOrNull
import pe.edu.upc.healthify.features.carerelationship.application.usecase.ChangeAiFeatureUseCase
import pe.edu.upc.healthify.features.carerelationship.application.usecase.GetAiPreferencesUseCase
import pe.edu.upc.healthify.features.carerelationship.application.usecase.GrantAiProcessingConsentUseCase
import pe.edu.upc.healthify.features.carerelationship.domain.entity.AiFeature
import pe.edu.upc.healthify.features.carerelationship.domain.entity.AiPreferences
import pe.edu.upc.healthify.features.carerelationship.domain.valueobject.CareLinkId
import pe.edu.upc.healthify.features.carerelationship.presentation.state.ConsentOffer
import pe.edu.upc.healthify.features.iam.application.usecase.ObserveCurrentUserUseCase
import pe.edu.upc.healthify.testing.FakeAiPreferencesRepository
import pe.edu.upc.healthify.testing.FakeCareLinkRepository
import pe.edu.upc.healthify.testing.FakeConnectivityObserver
import pe.edu.upc.healthify.testing.FakeSessionRepository
import pe.edu.upc.healthify.testing.MainDispatcherRule
import pe.edu.upc.healthify.testing.aiPreferences
import pe.edu.upc.healthify.testing.careLink
import pe.edu.upc.healthify.testing.failureOf

/** PT21.IA · Funciones con IA: sin consentimiento de IA se ofrece activarlo antes de encender una función (CR-2, IA-1). */
class AiFeaturesViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val preferences = FakeAiPreferencesRepository(AiPreferences.NoneGranted)
    private val careLinks = FakeCareLinkRepository().apply { activeResult = Result.success(careLink(id = 8, isActive = true)) }
    private val connectivity = FakeConnectivityObserver()

    private val viewModel by lazy {
        AiFeaturesViewModel(
            observeCurrentUser = ObserveCurrentUserUseCase(FakeSessionRepository()),
            getAiPreferences = GetAiPreferencesUseCase(preferences),
            changeAiFeature = ChangeAiFeatureUseCase(preferences),
            grantAiProcessingConsent = GrantAiProcessingConsentUseCase(careLinks, preferences),
            connectivityObserver = connectivity,
        )
    }

    @Test
    fun `without AI consent every switch reads off even if its preference was stored on`() = runTest {
        preferences.preferences = aiPreferences(consent = false, ideas = true, photo = true)

        val state = viewModel.state.value

        assertFalse(state.consentGranted)
        AiFeature.entries.forEach { assertFalse(state.isOn(it)) }
    }

    @Test
    fun `turning a feature on without consent offers the consent and does not call the backend`() = runTest {
        viewModel.onToggle(AiFeature.WEEKLY_SUMMARY, true)

        assertEquals(ConsentOffer(AiFeature.WEEKLY_SUMMARY), viewModel.state.value.consentOffer)
        assertTrue(preferences.updates.isEmpty())
        assertTrue(careLinks.aiConsentChanges.isEmpty())
    }

    @Test
    fun `accepting the offer grants the AI consent on the active link and then turns the feature on`() = runTest {
        viewModel.onToggle(AiFeature.SUGGESTED_QUESTIONS, true)
        // El backend ya devuelve el consentimiento otorgado al releer.
        preferences.preferences = AiPreferences.NoneGranted.copy(consentGranted = true)

        viewModel.onConfirmConsent()

        assertEquals(listOf(CareLinkId(8) to true), careLinks.aiConsentChanges)
        assertTrue(preferences.updates.single().suggestedQuestionsEnabled)
        val state = viewModel.state.value
        assertNull(state.consentOffer)
        assertTrue(state.consentGranted)
        assertTrue(state.isOn(AiFeature.SUGGESTED_QUESTIONS))
        assertFalse(state.isOn(AiFeature.WEEKLY_SUMMARY))
    }

    @Test
    fun `declining the offer leaves everything off`() = runTest {
        viewModel.onToggle(AiFeature.MEAL_IDEAS, true)

        viewModel.onDismissConsent()

        assertNull(viewModel.state.value.consentOffer)
        assertTrue(careLinks.aiConsentChanges.isEmpty())
        assertFalse(viewModel.state.value.isOn(AiFeature.MEAL_IDEAS))
    }

    @Test
    fun `if granting the consent fails the switch stays off and a message is shown`() = runTest {
        careLinks.aiConsentResult = failureOf(DomainError.Network)
        viewModel.onToggle(AiFeature.MEAL_PHOTO_RECOGNITION, true)

        viewModel.events.test {
            viewModel.onConfirmConsent()
            assertEquals(AiFeaturesEvent.ShowSaveFailed(offline = true), awaitItem())
        }
        assertTrue(preferences.updates.isEmpty())
        assertFalse(viewModel.state.value.isOn(AiFeature.MEAL_PHOTO_RECOGNITION))
    }

    @Test
    fun `with consent a feature is turned off right away and a failed save reverts the switch`() = runTest {
        preferences.preferences = aiPreferences(consent = true, ideas = true)
        viewModel.onToggle(AiFeature.MEAL_IDEAS, false)
        assertFalse(preferences.updates.single().mealIdeasEnabled)
        assertFalse(viewModel.state.value.isOn(AiFeature.MEAL_IDEAS))

        preferences.updateResult = failureOf(DomainError.Unexpected("HTTP_500"))
        viewModel.events.test {
            viewModel.onToggle(AiFeature.WEEKLY_SUMMARY, false)
            assertEquals(AiFeaturesEvent.ShowSaveFailed(offline = false), awaitItem())
        }
        assertTrue(viewModel.state.value.isOn(AiFeature.WEEKLY_SUMMARY))
    }

    @Test
    fun `409 AiConsentRequiredToEnableFeature from the backend offers the consent again`() = runTest {
        preferences.preferences = aiPreferences(consent = true, ideas = false)
        preferences.updateResult = failureOf(DomainError.Conflict(ChangeAiFeatureUseCase.CODE_CONSENT_REQUIRED))

        viewModel.onToggle(AiFeature.MEAL_IDEAS, true)

        assertEquals(ConsentOffer(AiFeature.MEAL_IDEAS), viewModel.state.value.consentOffer)
        assertFalse(viewModel.state.value.consentGranted)
    }

    @Test
    fun `offline nothing can be changed`() = runTest {
        connectivity.online.value = false
        preferences.preferences = aiPreferences(consent = true)

        viewModel.onToggle(AiFeature.WEEKLY_SUMMARY, false)

        assertTrue(preferences.updates.isEmpty())
        assertFalse(viewModel.state.value.canChange)
    }

    @Test
    fun `the use case refuses to turn a feature on without consent`() = runTest {
        val result = ChangeAiFeatureUseCase(preferences)(12, AiPreferences.NoneGranted, AiFeature.WEEKLY_SUMMARY, true)

        assertEquals(
            DomainError.Conflict(ChangeAiFeatureUseCase.CODE_CONSENT_REQUIRED),
            result.domainErrorOrNull(),
        )
        assertTrue(preferences.updates.isEmpty())
    }
}
