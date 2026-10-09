package pe.edu.upc.healthify.features.main.presentation.viewmodel

import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import pe.edu.upc.healthify.core.common.error.DomainError
import pe.edu.upc.healthify.features.carerelationship.application.usecase.GetAiPreferencesUseCase
import pe.edu.upc.healthify.features.iam.application.usecase.ApplyAppLanguageUseCase
import pe.edu.upc.healthify.features.iam.application.usecase.ChangePreferredLanguageUseCase
import pe.edu.upc.healthify.features.iam.application.usecase.GetAppLanguageUseCase
import pe.edu.upc.healthify.features.iam.application.usecase.ObserveCurrentUserUseCase
import pe.edu.upc.healthify.features.iam.domain.repository.AppLanguageRepository
import pe.edu.upc.healthify.features.iam.domain.repository.UserRepository
import pe.edu.upc.healthify.features.iam.domain.valueobject.PreferredLanguage
import pe.edu.upc.healthify.features.iam.domain.valueobject.UserId
import pe.edu.upc.healthify.testing.FakeAiPreferencesRepository
import pe.edu.upc.healthify.testing.FakeConnectivityObserver
import pe.edu.upc.healthify.testing.FakeSessionRepository
import pe.edu.upc.healthify.testing.MainDispatcherRule
import pe.edu.upc.healthify.testing.aiPreferences
import pe.edu.upc.healthify.testing.failureOf

/** PT21 / PT21.I · cambio de idioma: se aplica en el teléfono al instante y se guarda en la cuenta (IAM-3). */
class PatientSettingsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private class FakeAppLanguage(var language: PreferredLanguage = PreferredLanguage.SPANISH) : AppLanguageRepository {
        val applied = mutableListOf<PreferredLanguage>()
        override fun current() = language
        override fun apply(language: PreferredLanguage) {
            applied += language
            this.language = language
        }
    }

    private class FakeUserRepository : UserRepository {
        var results: MutableList<Result<Unit>> = mutableListOf()
        val changes = mutableListOf<PreferredLanguage>()
        override suspend fun changePreferredLanguage(userId: UserId, language: PreferredLanguage): Result<Unit> {
            changes += language
            return if (results.isEmpty()) Result.success(Unit) else results.removeAt(0)
        }
    }

    private val session = FakeSessionRepository()
    private val appLanguage = FakeAppLanguage()
    private val users = FakeUserRepository()
    private val connectivity = FakeConnectivityObserver()
    private val aiPreferences = FakeAiPreferencesRepository(aiPreferences(consent = true))

    private val viewModel by lazy {
        PatientSettingsViewModel(
            observeCurrentUser = ObserveCurrentUserUseCase(session),
            getAiPreferences = GetAiPreferencesUseCase(aiPreferences),
            getAppLanguage = GetAppLanguageUseCase(appLanguage),
            applyAppLanguage = ApplyAppLanguageUseCase(appLanguage),
            changePreferredLanguage = ChangePreferredLanguageUseCase(session, users),
            connectivityObserver = connectivity,
        )
    }

    @Test
    fun `the account, the current language and the AI status are shown`() = runTest {
        val state = viewModel.state.value

        assertEquals("María Flores", state.fullName)
        assertEquals("maria@mail.com", state.email)
        assertEquals(PreferredLanguage.SPANISH, state.language)
        assertEquals(true, state.aiFeaturesActive)
    }

    @Test
    fun `choosing English applies it on the phone, closes the sheet and saves it in the account`() = runTest {
        viewModel.onOpenLanguage()
        assertTrue(viewModel.state.value.showLanguageSheet)

        viewModel.events.test {
            viewModel.onLanguageSelected(PreferredLanguage.ENGLISH)
            assertEquals(PatientSettingsEvent.LanguageApplied, awaitItem())
        }

        assertEquals(listOf(PreferredLanguage.ENGLISH), appLanguage.applied)
        assertEquals(listOf(PreferredLanguage.ENGLISH), users.changes)
        assertFalse(viewModel.state.value.showLanguageSheet)
        assertEquals(PreferredLanguage.ENGLISH, viewModel.state.value.language)
    }

    @Test
    fun `choosing the current language changes nothing`() = runTest {
        viewModel.onLanguageSelected(PreferredLanguage.SPANISH)

        assertTrue(appLanguage.applied.isEmpty())
        // La cuenta ya está en español (usuario de la sesión): no se llama al backend.
        assertTrue(users.changes.isEmpty())
    }

    @Test
    fun `offline the app still changes language and the account is updated when the connection returns`() = runTest {
        connectivity.online.value = false
        users.results = mutableListOf(failureOf(DomainError.Network))
        viewModel.state.value // crea el ViewModel sin conexión

        viewModel.onLanguageSelected(PreferredLanguage.ENGLISH)
        assertEquals(listOf(PreferredLanguage.ENGLISH), appLanguage.applied)
        assertEquals(1, users.changes.size)

        connectivity.online.value = true

        assertEquals(listOf(PreferredLanguage.ENGLISH, PreferredLanguage.ENGLISH), users.changes)
    }

    @Test
    fun `downloading a copy of the data shows Coming soon because RM-6 is not in the backend`() = runTest {
        viewModel.onDataExport()
        assertTrue(viewModel.state.value.showDataExportDialog)

        viewModel.onDismissDataExport()
        assertFalse(viewModel.state.value.showDataExportDialog)
    }
}
