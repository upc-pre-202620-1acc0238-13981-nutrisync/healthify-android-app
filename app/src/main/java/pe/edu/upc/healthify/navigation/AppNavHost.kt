package pe.edu.upc.healthify.navigation

import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.toRoute
import pe.edu.upc.healthify.R
import pe.edu.upc.healthify.features.carerelationship.presentation.navigation.InvitePatientRoute
import pe.edu.upc.healthify.features.main.presentation.navigation.PractitionerPatientRoute
import pe.edu.upc.healthify.features.main.presentation.screen.PatientSnackbarRequest
import pe.edu.upc.healthify.features.main.presentation.screen.PractitionerPatientScreen
import pe.edu.upc.healthify.features.main.presentation.screen.PractitionerShellCallbacks
import pe.edu.upc.healthify.features.nutritionalcare.presentation.navigation.BaselineRoute
import pe.edu.upc.healthify.features.nutritionalcare.presentation.navigation.ConsultationNavigationCallbacks
import pe.edu.upc.healthify.features.nutritionalcare.presentation.navigation.consultationDestinations
import kotlinx.serialization.Serializable
import pe.edu.upc.healthify.features.carerelationship.presentation.navigation.DischargePatientRoute
import pe.edu.upc.healthify.features.foodcatalog.presentation.navigation.AddLocalFoodRoute
import pe.edu.upc.healthify.features.foodcatalog.presentation.navigation.FoodCatalogNavigationCallbacks
import pe.edu.upc.healthify.features.foodcatalog.presentation.navigation.FoodCatalogRoute
import pe.edu.upc.healthify.features.foodcatalog.presentation.navigation.KEY_ADDED_LOCAL_FOOD
import pe.edu.upc.healthify.features.foodcatalog.presentation.navigation.foodCatalogDestinations
import pe.edu.upc.healthify.features.main.presentation.navigation.PractitionerSignOutConfirmationRoute
import pe.edu.upc.healthify.features.main.presentation.screen.PractitionerShellMessage
import pe.edu.upc.healthify.features.monitoring.presentation.navigation.PractitionerAgendaNavigationCallbacks
import pe.edu.upc.healthify.features.monitoring.presentation.navigation.RecordReferralRoute
import pe.edu.upc.healthify.features.monitoring.presentation.navigation.ScheduleFollowUpRoute
import pe.edu.upc.healthify.features.monitoring.presentation.navigation.practitionerAgendaDestinations
import pe.edu.upc.healthify.features.nutritionalcare.presentation.navigation.AdjustProposalRoute
import pe.edu.upc.healthify.features.nutritionalcare.presentation.navigation.ReviewItemRoute
import pe.edu.upc.healthify.features.nutritionalcare.presentation.navigation.ReviewNavigationCallbacks
import pe.edu.upc.healthify.features.nutritionalcare.presentation.navigation.reviewDestinations
import pe.edu.upc.healthify.features.nutritionalcare.presentation.screen.SummaryManagementActions
import java.time.Instant
import pe.edu.upc.healthify.core.designsystem.component.ObserveEvents
import pe.edu.upc.healthify.core.designsystem.component.PatientTab
import pe.edu.upc.healthify.features.carerelationship.presentation.navigation.AiFeaturesRoute
import pe.edu.upc.healthify.features.carerelationship.presentation.navigation.CareRelationshipNavigationCallbacks
import pe.edu.upc.healthify.features.carerelationship.presentation.navigation.ConsentRoute
import pe.edu.upc.healthify.features.carerelationship.presentation.navigation.ConsentWithdrawnRoute
import pe.edu.upc.healthify.features.carerelationship.presentation.navigation.PendingConsentRoute
import pe.edu.upc.healthify.features.carerelationship.presentation.navigation.ScanInvitationRoute
import pe.edu.upc.healthify.features.carerelationship.presentation.navigation.SwitchPractitionerRoute
import pe.edu.upc.healthify.features.carerelationship.presentation.navigation.WithdrawConsentRoute
import pe.edu.upc.healthify.features.carerelationship.presentation.navigation.careRelationshipDestinations
import pe.edu.upc.healthify.features.iam.presentation.navigation.IamNavigationCallbacks
import pe.edu.upc.healthify.features.iam.presentation.navigation.SessionExpiredRoute
import pe.edu.upc.healthify.features.iam.presentation.navigation.ShellLoadingRoute
import pe.edu.upc.healthify.features.iam.presentation.navigation.SignInRoute
import pe.edu.upc.healthify.features.iam.presentation.navigation.SignUpRoute
import pe.edu.upc.healthify.features.iam.presentation.navigation.iamDestinations
import pe.edu.upc.healthify.features.iam.presentation.state.ShellDestination
import pe.edu.upc.healthify.features.intake.presentation.navigation.ConfirmEntryRoute
import pe.edu.upc.healthify.features.intake.presentation.navigation.IntakeNavigationCallbacks
import pe.edu.upc.healthify.features.intake.presentation.navigation.ManualMealRoute
import pe.edu.upc.healthify.features.intake.presentation.navigation.MealIdeasRoute
import pe.edu.upc.healthify.features.intake.presentation.navigation.MealPhotoRoute
import pe.edu.upc.healthify.features.intake.presentation.navigation.PendingSyncRoute
import pe.edu.upc.healthify.features.intake.presentation.navigation.PickedFoodResult
import pe.edu.upc.healthify.features.intake.presentation.navigation.RemindersRoute
import pe.edu.upc.healthify.features.intake.presentation.navigation.SelfWeighInRoute
import pe.edu.upc.healthify.features.intake.presentation.navigation.intakeDestinations
import pe.edu.upc.healthify.features.main.presentation.navigation.PatientShellRoute
import pe.edu.upc.healthify.features.main.presentation.navigation.PractitionerShellRoute
import pe.edu.upc.healthify.features.main.presentation.navigation.SignOutConfirmationRoute
import pe.edu.upc.healthify.features.main.presentation.screen.PatientShellCallbacks
import pe.edu.upc.healthify.features.main.presentation.screen.PatientShellScreen
import pe.edu.upc.healthify.features.main.presentation.screen.PractitionerShellScreen
import pe.edu.upc.healthify.features.main.presentation.screen.SignOutConfirmationScreen
import pe.edu.upc.healthify.features.monitoring.presentation.navigation.CheckInRoute
import pe.edu.upc.healthify.features.monitoring.presentation.navigation.ConsistencyEscalationInfoRoute
import pe.edu.upc.healthify.features.monitoring.presentation.navigation.FollowUpPreparationRoute
import pe.edu.upc.healthify.features.monitoring.presentation.navigation.MyConsultationsRoute
import pe.edu.upc.healthify.features.monitoring.presentation.navigation.ConsistencyNoticeRoute
import pe.edu.upc.healthify.features.monitoring.presentation.navigation.HowAmITodayRoute
import pe.edu.upc.healthify.features.monitoring.presentation.navigation.MonitoringNavigationCallbacks
import pe.edu.upc.healthify.features.monitoring.presentation.navigation.WeeklySummaryRoute
import pe.edu.upc.healthify.features.monitoring.presentation.navigation.monitoringDestinations
import pe.edu.upc.healthify.features.nutritionalcare.presentation.navigation.MyPlanRoute
import pe.edu.upc.healthify.features.nutritionalcare.presentation.navigation.NutritionalCareNavigationCallbacks
import pe.edu.upc.healthify.features.nutritionalcare.presentation.navigation.PlanVersionsRoute
import pe.edu.upc.healthify.features.nutritionalcare.presentation.navigation.nutritionalCareDestinations
import pe.edu.upc.healthify.features.onboarding.presentation.navigation.SplashRoute
import pe.edu.upc.healthify.features.onboarding.presentation.navigation.WelcomeRoute
import pe.edu.upc.healthify.features.onboarding.presentation.navigation.onboardingDestinations
import pe.edu.upc.healthify.features.onboarding.presentation.state.SplashDestination

/** Grafo de entrada: S1 Splash, S2 Bienvenida, S3 Registro, S4 Login, S5 Cargando y S6 Sesión expirada. */
@Serializable
data object AuthNavGraphRoute

/** Clave del `SavedStateHandle` del shell del paciente: pestaña que se pide abrir al volver a él. */
private const val KEY_REQUESTED_PATIENT_TAB = "requested_patient_tab"

/**
 * Grafo raíz: el grafo de entrada (onboarding + iam), CareRelationship del paciente (PT1, PT2, PT2.1, PT21.V, PT21.IA,
 * PT23, PT24), los dos shells y las pantallas completas que se abren desde las pestañas del paciente (PT4, PT4.1,
 * PT13.2, PT15, PT16, PT17, PT21.1, PT25, PT25.1, PT25.2 y, de Intake, PT5–PT8, PT9/PT10, PT12, PT14.4/PT14.5, PT19 y
 * PT22) y, del nutricionista, PR2, la ficha del paciente (PAC-0 a PAC-4), la consulta guiada (EV-1 a EV-5), PR14,
 * PR14.IA, PR14.IA-A, PR15, PR15.1, PR16, PR17, PR18 y PR20.1.
 *
 * Entrar a un shell, cerrar sesión o expirar la sesión **limpian todo el back stack** con este NavController: con
 * «atrás» nunca se vuelve a una pantalla de otra sesión ni al login desde dentro de la app.
 */
@Composable
fun AppNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    viewModel: AppNavigationViewModel = hiltViewModel(),
) {
    val activity = LocalActivity.current
    ObserveEvents(viewModel.sessionExpired) {
        if (navController.currentDestination?.hasRoute(SessionExpiredRoute::class) != true) {
            navController.navigateClearingBackStack(SessionExpiredRoute)
        }
    }

    NavHost(
        navController = navController,
        startDestination = AuthNavGraphRoute,
        modifier = modifier,
    ) {
        navigation<AuthNavGraphRoute>(startDestination = SplashRoute) {
            onboardingDestinations(
                onSplashResolved = { destination ->
                    navController.navigateClearingBackStack(
                        when (destination) {
                            SplashDestination.Welcome -> WelcomeRoute
                            SplashDestination.ShellLoading -> ShellLoadingRoute
                            SplashDestination.SessionExpired -> SessionExpiredRoute
                        },
                    )
                },
                onCreateAccount = { navController.navigate(SignUpRoute) { launchSingleTop = true } },
                onHaveAccount = { navController.navigate(SignInRoute) { launchSingleTop = true } },
            )
            iamDestinations(
                IamNavigationCallbacks(
                    onSignedIn = { navController.navigateClearingBackStack(ShellLoadingRoute) },
                    // S3 ↔ S4 se reemplazan entre sí: «atrás» vuelve a S2 sin acumular registro/login.
                    onGoToSignIn = {
                        navController.navigate(SignInRoute) {
                            popUpTo<SignUpRoute> { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                    onGoToSignUp = {
                        navController.navigate(SignUpRoute) {
                            popUpTo<SignInRoute> { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                    onShellSelected = { destination ->
                        navController.navigateClearingBackStack(
                            when (destination) {
                                ShellDestination.PatientScanInvitation -> ScanInvitationRoute()
                                ShellDestination.PatientPendingConsent -> PendingConsentRoute
                                ShellDestination.PatientHome -> PatientShellRoute
                                ShellDestination.PractitionerHome -> PractitionerShellRoute
                            },
                        )
                    },
                    onExpiredSessionSignIn = { navController.navigateClearingBackStack(SignInRoute) },
                    onNoSession = { navController.navigateClearingBackStack(WelcomeRoute) },
                    onCloseApp = { activity?.finish() },
                ),
            )
        }

        val onSignedOut = { navController.navigateClearingBackStack(SignInRoute) }
        careRelationshipDestinations(
            CareRelationshipNavigationCallbacks(
                onBack = { navController.popBackStack() },
                onInvitationRedeemed = { careLinkId -> navController.navigateClearingBackStack(ConsentRoute(careLinkId)) },
                onReviewConsent = { careLinkId ->
                    navController.navigate(ConsentRoute(careLinkId, fromPendingConsent = true)) { launchSingleTop = true }
                },
                onConsentPostponed = { navController.navigateClearingBackStack(PendingConsentRoute) },
                onConsentGranted = { navController.navigateClearingBackStack(PatientShellRoute) },
                onScanNewInvitation = { navController.navigateClearingBackStack(ScanInvitationRoute()) },
                onSwitchPractitionerConfirmed = {
                    navController.navigate(ScanInvitationRoute(replaceActiveLink = true)) {
                        popUpTo<SwitchPractitionerRoute> { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onConsentWithdrawn = { navController.navigateClearingBackStack(ConsentWithdrawnRoute) },
                onSignedOut = onSignedOut,
                onExitApp = { activity?.finish() },
                // PR18 → PR1 con el Snackbar del alta (la ficha del paciente sale del back stack).
                onPatientDischarged = { name ->
                    navController.returnToPractitionerShell(PractitionerShellMessage.PatientDischarged(name))
                },
            ),
        )
        composable<PatientShellRoute> { entry ->
            val requestedTabName by entry.savedStateHandle
                .getStateFlow<String?>(KEY_REQUESTED_PATIENT_TAB, null)
                .collectAsStateWithLifecycle()
            PatientShellScreen(
                callbacks = PatientShellCallbacks(
                    onSignOut = { navController.navigateSingleTop(SignOutConfirmationRoute) },
                    onSwitchPractitioner = { navController.navigateSingleTop(SwitchPractitionerRoute) },
                    onWithdrawConsent = { navController.navigateSingleTop(WithdrawConsentRoute) },
                    onOpenMyPlan = { navController.navigateSingleTop(MyPlanRoute) },
                    onOpenHowAmIToday = { navController.navigateSingleTop(HowAmITodayRoute) },
                    onOpenConsistencyNotice = { navController.navigateSingleTop(ConsistencyNoticeRoute) },
                    onOpenFollowUp = { id -> navController.navigateSingleTop(FollowUpPreparationRoute(id)) },
                    onRegisterMeal = { navController.navigateSingleTop(MealPhotoRoute()) },
                    onConfirmPortion = { id, date -> navController.navigateSingleTop(ConfirmEntryRoute(id, date.toString())) },
                    onOpenMealIdeas = { navController.navigateSingleTop(MealIdeasRoute) },
                    onOpenPendingSync = { navController.navigateSingleTop(PendingSyncRoute) },
                    onAnalyzePendingPhoto = { id -> navController.navigateSingleTop(MealPhotoRoute(pendingPhotoId = id)) },
                    onRegisterManually = { navController.navigateSingleTop(ManualMealRoute()) },
                    onRegisterSelfWeighIn = { navController.navigateSingleTop(SelfWeighInRoute) },
                    onOpenWeeklySummary = { navController.navigateSingleTop(WeeklySummaryRoute) },
                    onOpenMyConsultations = { navController.navigateSingleTop(MyConsultationsRoute) },
                    onOpenReminders = { navController.navigateSingleTop(RemindersRoute) },
                    onOpenAiFeatures = { navController.navigateSingleTop(AiFeaturesRoute) },
                ),
                requestedTab = PatientTab.entries.firstOrNull { it.name == requestedTabName },
                onRequestedTabHandled = { entry.savedStateHandle[KEY_REQUESTED_PATIENT_TAB] = null },
            )
        }
        nutritionalCareDestinations(
            NutritionalCareNavigationCallbacks(
                onBack = { navController.popBackStack() },
                onOpenPlanVersions = { navController.navigateSingleTop(PlanVersionsRoute) },
            ),
        )
        monitoringDestinations(
            MonitoringNavigationCallbacks(
                onBack = { navController.popBackStack() },
                // PT16 «Revisar mi diario»: vuelve al shell y abre la pestaña Diario.
                onReviewDiary = { navController.returnToPatientShell(PatientTab.Diary) },
                onOpenEscalationInfo = { navController.navigateSingleTop(ConsistencyEscalationInfoRoute) },
                // PT25.1 «Ver mis consultas»: si PT25 ya está debajo se vuelve a ella (no se apila otra copia).
                onOpenMyConsultations = {
                    if (!navController.popBackStack<MyConsultationsRoute>(inclusive = false)) {
                        navController.navigateSingleTop(MyConsultationsRoute)
                    }
                },
                onOpenCheckIn = { id -> navController.navigateSingleTop(CheckInRoute(id)) },
                onOpenPlanVersions = { navController.navigateSingleTop(PlanVersionsRoute) },
            ),
        )
        composable<SignOutConfirmationRoute> {
            SignOutConfirmationScreen(onCancel = { navController.popBackStack() }, onSignedOut = onSignedOut)
        }
        intakeDestinations(
            IntakeNavigationCallbacks(
                onBack = { navController.popBackStack() },
                onMealLogged = { navController.returnToPatientShell(PatientTab.Diary) },
                onOpenManualInsteadOfPhoto = {
                    navController.navigate(ManualMealRoute()) {
                        popUpTo<MealPhotoRoute> { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onPickFood = { navController.navigate(ManualMealRoute(pickFoodOnly = true)) },
                onFoodPicked = { id, name ->
                    navController.previousBackStackEntry?.savedStateHandle?.let { handle ->
                        handle[PickedFoodResult.KEY_NAME] = name
                        handle[PickedFoodResult.KEY_ID] = id
                    }
                    navController.popBackStack()
                },
                onSelfWeighInSaved = { navController.returnToPatientShell(PatientTab.Progress) },
            ),
        )
        composable<PractitionerShellRoute> { entry ->
            val shellMessage by entry.savedStateHandle
                .getStateFlow<String?>(KEY_PRACTITIONER_SHELL_MESSAGE, null)
                .collectAsStateWithLifecycle()
            PractitionerShellScreen(
                callbacks = PractitionerShellCallbacks(
                    onInvitePatient = { navController.navigateSingleTop(InvitePatientRoute) },
                    onOpenPatient = { patientId, careLinkId, name ->
                        navController.navigateSingleTop(PractitionerPatientRoute(patientId, careLinkId, name))
                    },
                    onOpenReviewItem = { id -> navController.navigateSingleTop(ReviewItemRoute(id)) },
                    onScheduleFollowUp = { navController.navigateSingleTop(ScheduleFollowUpRoute()) },
                    onRescheduleFollowUp = { row ->
                        navController.navigateSingleTop(
                            ScheduleFollowUpRoute(
                                patientId = row.patientId,
                                patientName = row.patientName.orEmpty(),
                                followUpId = row.followUpId,
                                scheduledForEpochSecond = row.scheduledFor.epochSecond,
                                preparation = row.preparation.joinToString(",") { it.code },
                            ),
                        )
                    },
                    onOpenFoodCatalog = { navController.navigateSingleTop(FoodCatalogRoute) },
                    onSignOut = { navController.navigateSingleTop(PractitionerSignOutConfirmationRoute) },
                ),
                message = shellMessage?.let(PractitionerShellMessage::decode),
                onMessageHandled = { entry.savedStateHandle[KEY_PRACTITIONER_SHELL_MESSAGE] = null },
            )
        }
        composable<PractitionerPatientRoute> { entry ->
            val route = entry.toRoute<PractitionerPatientRoute>()
            val snackbar by entry.savedStateHandle
                .getStateFlow<String?>(KEY_PATIENT_SNACKBAR, null)
                .collectAsStateWithLifecycle()
            PractitionerPatientScreen(
                patientId = route.patientId,
                patientName = route.patientName,
                onBack = { navController.popBackStack() },
                onNavigate = { navController.navigateSingleTop(it) },
                snackbarRequest = snackbar?.let(::patientSnackbarRequest),
                onSnackbarHandled = { entry.savedStateHandle[KEY_PATIENT_SNACKBAR] = null },
                management = SummaryManagementActions(
                    onSchedule = {
                        navController.navigateSingleTop(ScheduleFollowUpRoute(route.patientId, route.patientName))
                    },
                    onReferral = { navController.navigateSingleTop(RecordReferralRoute(route.patientId, route.patientName)) },
                    onDischarge = {
                        navController.navigateSingleTop(DischargePatientRoute(route.careLinkId, route.patientName))
                    },
                ),
            )
        }
        reviewDestinations(
            ReviewNavigationCallbacks(
                onBack = { navController.popBackStack() },
                // PR14 / PR14.IA / PR14.IA-A → PR13.1 «Señal resuelta» (cierra lo que se abrió encima de la bandeja).
                onResolved = { name ->
                    navController.returnToPractitionerShell(PractitionerShellMessage.SignalResolved(name))
                },
                onClose = { navController.returnToPractitionerShell(message = null) },
                onOpenAdjust = { id -> navController.navigateSingleTop(AdjustProposalRoute(id)) },
                onOpenPatient = { patientId, careLinkId, name ->
                    navController.navigateSingleTop(PractitionerPatientRoute(patientId, careLinkId, name))
                },
            ),
        )
        practitionerAgendaDestinations(
            PractitionerAgendaNavigationCallbacks(
                onBack = { navController.popBackStack() },
                onScheduled = { scheduledFor, rescheduled ->
                    // Desde PAC-1 vuelve a la ficha; desde la Agenda, a la pestaña Agenda (PR17.0-S).
                    val fromPatient = navController.previousBackStackEntry?.destination
                        ?.hasRoute(PractitionerPatientRoute::class) == true
                    if (fromPatient) {
                        navController.returnToPractitionerPatient(snackbar = "$SNACKBAR_FOLLOW_UP_PREFIX${scheduledFor.epochSecond}")
                    } else {
                        navController.returnToPractitionerShell(
                            PractitionerShellMessage.FollowUpScheduled(scheduledFor, rescheduled),
                        )
                    }
                },
                onReferralRecorded = { specialty ->
                    navController.returnToPractitionerPatient(snackbar = "$SNACKBAR_REFERRAL_PREFIX$specialty")
                },
            ),
        )
        foodCatalogDestinations(
            FoodCatalogNavigationCallbacks(
                onBack = { navController.popBackStack() },
                onAddLocalFood = { navController.navigateSingleTop(AddLocalFoodRoute) },
                onLocalFoodAdded = { name ->
                    navController.previousBackStackEntry?.savedStateHandle?.set(KEY_ADDED_LOCAL_FOOD, name)
                    navController.popBackStack()
                },
            ),
        )
        composable<PractitionerSignOutConfirmationRoute> {
            SignOutConfirmationScreen(
                onCancel = { navController.popBackStack() },
                onSignedOut = onSignedOut,
                bodyRes = R.string.practitioner_sign_out_body,
            )
        }
        consultationDestinations(
            ConsultationNavigationCallbacks(
                onBack = { navController.popBackStack() },
                // EV-1 «Guardar e iniciar consulta»: el paso reemplaza a EV-1 («‹» desde el paso 1 abre EV-2.S).
                onBaselineSavedStartConsultation = { route ->
                    navController.navigate(route) {
                        popUpTo<BaselineRoute> { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onOpenStep = { navController.navigateSingleTop(it) },
                onPreviousStep = { previous ->
                    // Al reanudar en un paso avanzado, el anterior no está debajo: se pone en lugar del actual.
                    if (!navController.popBackStack(previous, inclusive = false)) {
                        val current = navController.currentDestination?.id
                        navController.navigate(previous) {
                            if (current != null) popUpTo(current) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                },
                onExitConsultation = { navController.returnToPractitionerPatient(snackbar = null) },
                onEditBaseline = { navController.navigateSingleTop(it) },
                onPublished = { navController.returnToPractitionerPatient(snackbar = SNACKBAR_PLAN_PUBLISHED) },
            ),
        )
    }
}

/** Clave del `SavedStateHandle` de la ficha del paciente: snackbar que se pide mostrar al volver (PAC-1-S1/S3). */
private const val KEY_PATIENT_SNACKBAR = "patient_snackbar"
private const val SNACKBAR_PLAN_PUBLISHED = "PlanPublished"
private const val SNACKBAR_REFERRAL_PREFIX = "ReferralRegistered:"
private const val SNACKBAR_FOLLOW_UP_PREFIX = "FollowUpScheduled:"

/** Clave del `SavedStateHandle` del shell del nutricionista: Snackbar que se pide al volver (PR13.1, PR17.0-S, alta). */
private const val KEY_PRACTITIONER_SHELL_MESSAGE = "practitioner_shell_message"

private fun patientSnackbarRequest(value: String): PatientSnackbarRequest? = when {
    value == SNACKBAR_PLAN_PUBLISHED -> PatientSnackbarRequest.PlanPublished
    value.startsWith(SNACKBAR_REFERRAL_PREFIX) ->
        PatientSnackbarRequest.ReferralRegistered(value.removePrefix(SNACKBAR_REFERRAL_PREFIX))
    value.startsWith(SNACKBAR_FOLLOW_UP_PREFIX) -> value.removePrefix(SNACKBAR_FOLLOW_UP_PREFIX).toLongOrNull()
        ?.let { PatientSnackbarRequest.FollowUpScheduled(Instant.ofEpochSecond(it)) }
    else -> null
}

/** Vuelve al shell del nutricionista (cerrando lo que se abrió encima) y le pide mostrar [message]. */
private fun NavHostController.returnToPractitionerShell(message: PractitionerShellMessage?) {
    val shell = try {
        getBackStackEntry<PractitionerShellRoute>()
    } catch (_: IllegalArgumentException) {
        popBackStack()
        return
    }
    if (message != null) shell.savedStateHandle[KEY_PRACTITIONER_SHELL_MESSAGE] = message.encode()
    popBackStack<PractitionerShellRoute>(inclusive = false)
}

/**
 * Vuelve a la ficha del paciente cerrando lo que se abrió encima (la consulta EV-1 a EV-5, PR16 o PR17): se relee y
 * muestra PAC-1.C o el Snackbar pedido (PAC-1-S1, PAC-1-S3, consulta agendada).
 */
private fun NavHostController.returnToPractitionerPatient(snackbar: String?) {
    val patient = try {
        getBackStackEntry<PractitionerPatientRoute>()
    } catch (_: IllegalArgumentException) {
        popBackStack()
        return
    }
    if (snackbar != null) patient.savedStateHandle[KEY_PATIENT_SNACKBAR] = snackbar
    popBackStack<PractitionerPatientRoute>(inclusive = false)
}

private fun NavHostController.navigateSingleTop(route: Any) {
    navigate(route) { launchSingleTop = true }
}

/** Vuelve al shell del paciente (cerrando lo que se abrió encima) y le pide abrir [tab]. */
private fun NavHostController.returnToPatientShell(tab: PatientTab) {
    val shell = try {
        getBackStackEntry<PatientShellRoute>()
    } catch (_: IllegalArgumentException) {
        // El shell ya no está debajo (no debería pasar desde PT16): solo se cierra la pantalla actual.
        popBackStack()
        return
    }
    shell.savedStateHandle[KEY_REQUESTED_PATIENT_TAB] = tab.name
    popBackStack<PatientShellRoute>(inclusive = false)
}

/** Navega a [route] dejándola como única entrada del back stack. */
private fun NavHostController.navigateClearingBackStack(route: Any) {
    navigate(route) {
        popUpTo(graph.id) { inclusive = true }
        launchSingleTop = true
    }
}
