package pe.edu.upc.healthify.features.main.presentation.components

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import pe.edu.upc.healthify.R
import pe.edu.upc.healthify.core.designsystem.component.DrawerDivider
import pe.edu.upc.healthify.core.designsystem.component.DrawerHeader
import pe.edu.upc.healthify.core.designsystem.component.DrawerItem
import pe.edu.upc.healthify.core.designsystem.icon.HealthifyIcons

/** Items of the patient's main menu, in order. "Sign out" is separate, after the divider. */
enum class PatientMenuItem(@param:StringRes val labelRes: Int, val icon: ImageVector) {
    MyPlan(R.string.menu_my_plan, HealthifyIcons.Evaluation),
    MyConsultations(R.string.menu_my_consultations, HealthifyIcons.Calendar),
    Pending(R.string.menu_pending, HealthifyIcons.Clock),
    Settings(R.string.menu_settings, HealthifyIcons.Settings),
}

/**
 * Content of the patient's main menu: header, My plan · My appointments · Pending · Settings and, after the
 * divider, Sign out.
 *
 * @param selected item of the current screen (highlighted); `null` from the tabs, which are not in the menu.
 */
@Composable
fun ColumnScope.PatientMenuContent(
    fullName: String,
    avatarInitial: String,
    selected: PatientMenuItem?,
    onSelect: (PatientMenuItem) -> Unit,
    onSignOut: () -> Unit,
) {
    DrawerHeader(name = fullName, role = stringResource(R.string.menu_role_patient), avatarInitial = avatarInitial)
    PatientMenuItem.entries.forEach { item ->
        DrawerItem(
            icon = item.icon,
            label = stringResource(item.labelRes),
            selected = item == selected,
            onClick = { onSelect(item) },
        )
    }
    DrawerDivider()
    DrawerItem(
        icon = HealthifyIcons.Logout,
        label = stringResource(R.string.menu_sign_out),
        selected = false,
        onClick = onSignOut,
    )
}
