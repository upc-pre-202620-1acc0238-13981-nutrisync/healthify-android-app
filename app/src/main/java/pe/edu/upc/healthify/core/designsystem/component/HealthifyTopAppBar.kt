package pe.edu.upc.healthify.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import pe.edu.upc.healthify.R
import pe.edu.upc.healthify.core.designsystem.icon.HealthifyIcons
import pe.edu.upc.healthify.core.designsystem.theme.HealthifyTheme

// Medidas del TopAppBar del Figma.
private val TopAppBarHeight = 64.dp
private val AvatarSize = 40.dp

/**
 * TopAppBar pequeña del Figma (64 dp), título en Anton.
 * - «Atrás»: [onBack] no nulo → IconButton 48 dp con arrow-back (llama a `popBackStack` desde la pantalla;
 *   el gesto/botón atrás del sistema hace lo mismo).
 * - «Raíz»: [onBack] nulo → destinos de la NavigationBar, sin atrás.
 *
 * @param actions IconButtons de acción a la derecha (p. ej. Icon/bell).
 * @param containerColor fondo de la barra; las pantallas oscuras del Figma (PT1) usan `inverseSurface`.
 * @param contentColor título e ícono de atrás sobre [containerColor].
 */
@Composable
fun HealthifyTopAppBar(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    windowInsets: WindowInsets = WindowInsets.statusBars,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
) {
    val dimens = HealthifyTheme.dimens
    TopAppBarContainer(
        modifier = modifier,
        windowInsets = windowInsets,
        startPadding = if (onBack != null) dimens.space4 else dimens.space16,
        containerColor = containerColor,
    ) {
        if (onBack != null) {
            HealthifyIconButton(
                icon = HealthifyIcons.ArrowBack,
                contentDescription = stringResource(R.string.ds_cd_back),
                onClick = onBack,
                tint = contentColor,
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .semantics { heading() },
        )
        actions()
    }
}

/**
 * TopAppBar «Saludo» del Figma (Inicio del paciente): avatar con la inicial, saludo y título.
 *
 * @param avatarInitial inicial del nombre (decorativa: se oculta a TalkBack porque [greeting] ya nombra).
 */
@Composable
fun GreetingTopAppBar(
    greeting: String,
    title: String,
    avatarInitial: String,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {},
    windowInsets: WindowInsets = WindowInsets.statusBars,
) {
    val dimens = HealthifyTheme.dimens
    val scheme = MaterialTheme.colorScheme
    TopAppBarContainer(modifier = modifier, windowInsets = windowInsets, startPadding = dimens.space16) {
        Box(
            modifier = Modifier
                .size(AvatarSize)
                .clip(CircleShape)
                .background(scheme.primary)
                .clearAndSetSemantics {},
            contentAlignment = Alignment.Center,
        ) {
            Text(text = avatarInitial, style = MaterialTheme.typography.titleMedium, color = scheme.onPrimary)
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = dimens.space8),
        ) {
            Text(
                text = greeting,
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = scheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.semantics { heading() },
            )
        }
        actions()
    }
}

@Composable
private fun TopAppBarContainer(
    modifier: Modifier,
    windowInsets: WindowInsets,
    startPadding: Dp,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    content: @Composable RowScope.() -> Unit,
) {
    val dimens = HealthifyTheme.dimens
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(containerColor)
            .windowInsetsPadding(windowInsets)
            .heightIn(min = TopAppBarHeight)
            .padding(start = startPadding, end = dimens.space4),
        horizontalArrangement = Arrangement.spacedBy(dimens.space4),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

@Preview(name = "TopAppBar · Atrás / Raíz / Saludo", widthDp = 360)
@Composable
private fun TopAppBarPreview() {
    ComponentPreview {
        HealthifyTopAppBar(title = "TÍTULO DE PANTALLA", onBack = {}, windowInsets = WindowInsets(0))
        HealthifyTopAppBar(
            title = "TÍTULO DE PANTALLA",
            windowInsets = WindowInsets(0),
            actions = { HealthifyIconButton(HealthifyIcons.Bell, contentDescription = null, onClick = {}) },
        )
        GreetingTopAppBar(greeting = "Hola, María", title = "HOY", avatarInitial = "M", windowInsets = WindowInsets(0))
    }
}
