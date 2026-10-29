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
    /** Meal logged or queued: back to the shell on the Diary tab (the diary shows the Snackbar). */
    val onMealLogged: () -> Unit,
    /** From the photo flow to logging by hand (the manual screen replaces the camera in the back stack). */
    val onOpenManualInsteadOfPhoto: () -> Unit,
    /** "Not this dish?" → the food search in pick mode. */
    val onPickFood: () -> Unit,
    /** Food picked: handed to the previous screen ([PickedFoodResult]) and the food search closes. */
    val onFoodPicked: (referenceFoodId: Long, name: String) -> Unit,
    /** Self weigh-in saved or queued: back to the shell on the Progress tab (the trend screen shows the Snackbar). */
    val onSelfWeighInSaved: () -> Unit,
)

/** The patient's Intake destinations in the root graph (full screens over the shell). */
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
