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

// Figma TopAppBar measurements.
private val TopAppBarHeight = 64.dp
private val AvatarSize = 40.dp

/**
 * Figma small TopAppBar (64 dp), title in Anton.
 * - "Back": [onBack] not null → 48 dp IconButton with arrow-back (call `popBackStack` from the screen;
 *   the system back gesture/button does the same).
 * - "Root": [onBack] null → NavigationBar destinations, no back.
 *
 * @param actions action IconButtons on the right (e.g. Icon/bell).
 * @param containerColor bar background; the dark Figma screens (e.g. the QR scanner) use `inverseSurface`.
 * @param contentColor title and back icon on [containerColor].
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
 * Figma "Greeting" TopAppBar (patient Home): avatar with the initial, greeting and title.
 *
 * @param avatarInitial initial of the name (decorative: hidden from TalkBack because [greeting] already has the name).
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

@Preview(name = "TopAppBar · Back / Root / Greeting", widthDp = 360)
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
