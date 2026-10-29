package pe.edu.upc.healthify.core.designsystem.component

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import pe.edu.upc.healthify.R
import pe.edu.upc.healthify.core.designsystem.icon.HealthifyIcons
import pe.edu.upc.healthify.core.designsystem.theme.HealthifyTheme

// Main menu (right side panel) measurements.
private val DrawerWidth = 296.dp
private val DrawerItemHeight = 56.dp
private val DrawerAvatarSize = 48.dp
private const val DRAWER_SCRIM_ALPHA = 0.4f

/**
 * Side panel that slides in from the end edge (right), over a 40 % black scrim. It covers the whole screen,
 * bottom bar included; it closes with [onDismissRequest] (tap the scrim or back). [content] is the shell
 * content underneath.
 */
// M3's ModalNavigationDrawer only opens from the start (left); the patient menu opens from the right, so
// the panel is drawn here with the same enter and exit animations.
@Composable
fun HealthifyEndDrawer(
    open: Boolean,
    onDismissRequest: () -> Unit,
    drawerContent: @Composable ColumnScope.() -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val dimens = HealthifyTheme.dimens
    val scheme = MaterialTheme.colorScheme
    BackHandler(enabled = open, onBack = onDismissRequest)
    Box(modifier = modifier.fillMaxSize()) {
        content()
        AnimatedVisibility(visible = open, enter = fadeIn(), exit = fadeOut()) {
            val closeLabel = stringResource(R.string.ds_cd_close)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(scheme.scrim.copy(alpha = DRAWER_SCRIM_ALPHA))
                    .clickable(interactionSource = null, indication = null, onClickLabel = closeLabel, onClick = onDismissRequest),
            )
        }
        AnimatedVisibility(
            visible = open,
            enter = slideInHorizontally { it },
            exit = slideOutHorizontally { it },
            modifier = Modifier.align(Alignment.CenterEnd),
        ) {
            val shape = RoundedCornerShape(topStart = dimens.radiusLg, bottomStart = dimens.radiusLg)
            Column(
                modifier = Modifier
                    .width(DrawerWidth)
                    .fillMaxHeight()
                    .clip(shape)
                    .background(scheme.surface)
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = dimens.space12, vertical = dimens.space24),
                verticalArrangement = Arrangement.spacedBy(dimens.space4),
                content = drawerContent,
            )
        }
    }
}

/** Menu header: avatar with the initial, name and role. */
@Composable
fun DrawerHeader(name: String, role: String, avatarInitial: String, modifier: Modifier = Modifier) {
    val dimens = HealthifyTheme.dimens
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = dimens.space16, end = dimens.space16, bottom = dimens.space24),
        horizontalArrangement = Arrangement.spacedBy(dimens.space12),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(DrawerAvatarSize)
                .clip(CircleShape)
                .background(scheme.primary)
                .clearAndSetSemantics {},
            contentAlignment = Alignment.Center,
        ) {
            Text(text = avatarInitial, style = MaterialTheme.typography.titleMedium, color = scheme.onPrimary)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                style = MaterialTheme.typography.titleMedium,
                color = scheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(text = role, style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant)
        }
    }
}

/** Menu item (56 dp): the current screen's item sits in a `primaryContainer` pill with `primary` text. */
@Composable
fun DrawerItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dimens = HealthifyTheme.dimens
    val scheme = MaterialTheme.colorScheme
    val labelStyle = MaterialTheme.typography.bodyLarge
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = DrawerItemHeight)
            .clip(RoundedCornerShape(dimens.radiusFull))
            .then(if (selected) Modifier.background(scheme.primaryContainer) else Modifier)
            .selectable(selected = selected, role = Role.Button, onClick = onClick)
            .padding(horizontal = dimens.space16),
        horizontalArrangement = Arrangement.spacedBy(dimens.space12),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (selected) scheme.primary else scheme.onSurface,
            modifier = Modifier.size(dimens.icon),
        )
        Text(
            text = label,
            style = if (selected) labelStyle.copy(fontWeight = FontWeight.Bold) else labelStyle,
            color = if (selected) scheme.primary else scheme.onSurface,
            modifier = Modifier.weight(1f),
        )
    }
}

/** Divider between groups of menu items. */
@Composable
fun DrawerDivider(modifier: Modifier = Modifier) {
    HorizontalDivider(
        modifier = modifier.padding(horizontal = HealthifyTheme.dimens.space16, vertical = HealthifyTheme.dimens.space8),
        color = MaterialTheme.colorScheme.outlineVariant,
        thickness = HealthifyTheme.dimens.borderThin,
    )
}

@Preview(name = "Side menu · open", widthDp = 360, heightDp = 640)
@Composable
private fun HealthifyEndDrawerPreview() {
    HealthifyTheme {
        HealthifyEndDrawer(
            open = true,
            onDismissRequest = {},
            drawerContent = {
                DrawerHeader(name = "María Flores", role = "Paciente", avatarInitial = "M")
                DrawerItem(icon = HealthifyIcons.Calendar, label = "Mis consultas", selected = false, onClick = {})
                DrawerItem(icon = HealthifyIcons.Settings, label = "Ajustes", selected = true, onClick = {})
                DrawerDivider()
                DrawerItem(icon = HealthifyIcons.Logout, label = "Cerrar sesión", selected = false, onClick = {})
            },
        ) {
            Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface))
        }
    }
}
