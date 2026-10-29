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

/** Ítems del menú principal del nutricionista (PR1.D), en su orden. «Cerrar sesión» va aparte, tras el divisor. */
enum class PractitionerMenuItem(@param:StringRes val labelRes: Int, val icon: ImageVector) {
    FoodCatalog(R.string.menu_food_catalog, HealthifyIcons.Cutlery),
    Settings(R.string.menu_settings, HealthifyIcons.Settings),
}

/**
 * Contenido del menú principal del nutricionista: encabezado, Catálogo de alimentos · Ajustes y, tras el divisor,
 * Cerrar sesión.
 *
 * @param selected ítem de la pantalla actual (se resalta); `null` desde las pestañas, que no están en el menú.
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
