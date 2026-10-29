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

/** Items of the practitioner's main menu, in order. "Sign out" is separate, after the divider. */
enum class PractitionerMenuItem(@param:StringRes val labelRes: Int, val icon: ImageVector) {
    FoodCatalog(R.string.menu_food_catalog, HealthifyIcons.Cutlery),
    Settings(R.string.menu_settings, HealthifyIcons.Settings),
}

/**
 * Content of the practitioner's main menu: header, Food catalog · Settings and, after the divider,
 * Sign out.
 *
 * @param selected item of the current screen (highlighted); `null` from the tabs, which are not in the menu.
 */
@Composable
fun ColumnScope.PractitionerMenuContent(
    fullName: String,
    avatarInitial: String,
    selected: PractitionerMenuItem?,
    onSelect: (PractitionerMenuItem) -> Unit,
    onSignOut: () -> Unit,
) {
    DrawerHeader(name = fullName, role = stringResource(R.string.menu_role_practitioner), avatarInitial = avatarInitial)
    PractitionerMenuItem.entries.forEach { item ->
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
