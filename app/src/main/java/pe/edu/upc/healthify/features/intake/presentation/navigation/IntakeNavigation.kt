package pe.edu.upc.healthify.features.intake.presentation.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import pe.edu.upc.healthify.features.intake.presentation.screen.ConfirmEntryScreen
import pe.edu.upc.healthify.features.intake.presentation.screen.ManualMealScreen
import pe.edu.upc.healthify.features.intake.presentation.screen.MealIdeasScreen
import pe.edu.upc.healthify.features.intake.presentation.screen.MealPhotoScreen
import pe.edu.upc.healthify.features.intake.presentation.screen.PendingSyncScreen
import pe.edu.upc.healthify.features.intake.presentation.screen.RemindersScreen
import pe.edu.upc.healthify.features.intake.presentation.screen.SelfWeighInScreen

data class IntakeNavigationCallbacks(
    val onBack: () -> Unit,
    /** Comida registrada o encolada: vuelve al shell en la pestaña Diario (el diario muestra el Snackbar). */
    val onMealLogged: () -> Unit,
    /** Del flujo por foto a registrar a mano (PT9 reemplaza a PT5 en el back stack). */
    val onOpenManualInsteadOfPhoto: () -> Unit,
    /** PT8 «¿No es este plato?» → PT9 en modo elegir. */
    val onPickFood: () -> Unit,
    /** Alimento elegido: se entrega a la pantalla anterior ([PickedFoodResult]) y se cierra PT9. */
    val onFoodPicked: (referenceFoodId: Long, name: String) -> Unit,
    /** Autopesaje guardado o encolado: vuelve al shell en la pestaña Progreso (PT13 muestra el Snackbar). */
    val onSelfWeighInSaved: () -> Unit,
)

/** Destinos de Intake del paciente en el grafo raíz (pantallas completas sobre el shell). */
fun NavGraphBuilder.intakeDestinations(callbacks: IntakeNavigationCallbacks) {
    composable<MealPhotoRoute> {
        MealPhotoScreen(
            onExit = callbacks.onBack,
            onOpenManual = callbacks.onOpenManualInsteadOfPhoto,
            onPickFood = callbacks.onPickFood,
            onLogged = { callbacks.onMealLogged() },
        )
    }
    composable<ManualMealRoute> {
        ManualMealScreen(
            onBack = callbacks.onBack,
            onLogged = { callbacks.onMealLogged() },
            onFoodPicked = callbacks.onFoodPicked,
        )
    }
    composable<ConfirmEntryRoute> {
        ConfirmEntryScreen(onBack = callbacks.onBack, onPickFood = callbacks.onPickFood, onDone = callbacks.onBack)
    }
    composable<MealIdeasRoute> {
        MealIdeasScreen(onBack = callbacks.onBack, onLogged = { callbacks.onMealLogged() })
    }
    composable<PendingSyncRoute> { PendingSyncScreen(onBack = callbacks.onBack) }
    composable<RemindersRoute> { RemindersScreen(onBack = callbacks.onBack) }
    composable<SelfWeighInRoute> {
        SelfWeighInScreen(onBack = callbacks.onBack, onSaved = { callbacks.onSelfWeighInSaved() })
    }
}
