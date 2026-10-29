package pe.edu.upc.healthify.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import pe.edu.upc.healthify.R
import pe.edu.upc.healthify.core.designsystem.icon.HealthifyIcons
import pe.edu.upc.healthify.core.designsystem.theme.HealthifyTheme
import pe.edu.upc.healthify.core.designsystem.theme.elevation

/**
 * "Style" variants of the Figma Button component.
 * - [Filled]: main CTA (orange `secondary`).
 * - [Tonal], [Outlined], [Text]: secondary actions.
 * - [Danger]: irreversible actions.
 */
enum class HealthifyButtonStyle { Filled, Tonal, Outlined, Text, Danger }

// Figma Button component measurements.
private val LoadingIndicatorSize = 20.dp
private val LoadingIndicatorStroke = 2.dp

/**
 * Figma Button (adapted M3). 56 dp tall (Text: 48 dp), pill shape.
 *
 * @param loading shows the circular progress and blocks taps; pass the mockup's loading text in [text]
 *   (e.g. "Saving…").
 * @param contentColor text color when enabled, for the Figma variants that change it on dark backgrounds
 *   (e.g. the welcome screen's "I already have an account" Text button in `inversePrimary`). `null` = the style's.
 */
@Composable
fun HealthifyButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: HealthifyButtonStyle = HealthifyButtonStyle.Filled,
    enabled: Boolean = true,
    loading: Boolean = false,
    icon: ImageVector? = null,
    interactionSource: MutableInteractionSource? = null,
    contentColor: Color? = null,
) {
    HealthifyButtonImpl(
        text = text,
        onClick = onClick,
        modifier = modifier,
        style = style,
        enabled = enabled,
        loading = loading,
        icon = icon,
        interactionSource = interactionSource,
        forcePressed = false,
        contentColor = contentColor,
    )
}

@Composable
internal fun HealthifyButtonImpl(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: HealthifyButtonStyle = HealthifyButtonStyle.Filled,
    enabled: Boolean = true,
    loading: Boolean = false,
    icon: ImageVector? = null,
    interactionSource: MutableInteractionSource? = null,
    forcePressed: Boolean = false,
    contentColor: Color? = null,
) {
    val dimens = HealthifyTheme.dimens
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val pressed = source.isPressed(forcePressed) && enabled && !loading
    val colors = buttonColors(style, enabled, pressed).let { base ->
        if (contentColor != null && enabled) base.copy(content = contentColor) else base
    }
    val shape = CircleShape
    val isText = style == HealthifyButtonStyle.Text
    val loadingDescription = stringResource(R.string.ds_cd_loading)

    Row(
        modifier = modifier
            .heightIn(min = if (isText) dimens.touchMin else dimens.button)
            .elevation(if (style == HealthifyButtonStyle.Filled && enabled) dimens.elevation1 else null, shape)
            .clip(shape)
            .background(colors.container)
            .then(
                if (colors.border != null) Modifier.border(dimens.borderThin, colors.border, shape) else Modifier,
            )
            .clickable(
                interactionSource = source,
                indication = null,
                enabled = enabled && !loading,
                role = Role.Button,
                onClick = onClick,
            )
            .padding(horizontal = if (isText) dimens.space12 else dimens.space24),
        horizontalArrangement = Arrangement.spacedBy(dimens.space8, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier
                    .size(LoadingIndicatorSize)
                    .semantics { contentDescription = loadingDescription },
                color = colors.content,
                strokeWidth = LoadingIndicatorStroke,
            )
        } else if (icon != null) {
            Icon(imageVector = icon, contentDescription = null, tint = colors.content, modifier = Modifier.size(dimens.icon))
        }
        Text(
            text = text,
            style = if (isText) MaterialTheme.typography.labelLarge else HealthifyTheme.extendedTypography.buttonLarge,
            color = colors.content,
            textAlign = TextAlign.Center,
        )
    }
}

private data class ButtonColors(val container: Color, val content: Color, val border: Color?)

@Composable
private fun buttonColors(style: HealthifyButtonStyle, enabled: Boolean, pressed: Boolean): ButtonColors {
    val scheme = MaterialTheme.colorScheme
    val ext = HealthifyTheme.extendedColors
    return when (style) {
        HealthifyButtonStyle.Filled -> ButtonColors(
            container = when {
                !enabled -> ext.disabledContainer
                pressed -> ext.secondaryPressed
                else -> scheme.secondary
            },
            content = if (enabled) scheme.onSecondary else ext.onDisabled,
            border = null,
        )
        HealthifyButtonStyle.Tonal -> ButtonColors(
            container = when {
                !enabled -> ext.disabledContainer
                pressed -> ext.primaryContainerPressed
                else -> scheme.primaryContainer
            },
            content = if (enabled) scheme.onPrimaryContainer else ext.onDisabled,
            border = null,
        )
        HealthifyButtonStyle.Outlined -> ButtonColors(
            container = if (pressed) ext.primaryOverlayPressed else Color.Transparent,
            content = if (enabled) scheme.primary else ext.onDisabled,
            border = if (enabled) scheme.outline else ext.disabledContainer,
        )
        HealthifyButtonStyle.Text -> ButtonColors(
            container = if (pressed) ext.primaryOverlayPressed else Color.Transparent,
            content = if (enabled) scheme.primary else ext.onDisabled,
            border = null,
        )
        HealthifyButtonStyle.Danger -> ButtonColors(
            container = if (pressed) ext.errorOverlayPressed else Color.Transparent,
            content = if (enabled) scheme.error else ext.onDisabled,
            border = if (enabled) scheme.error else ext.disabledContainer,
        )
    }
}

@Composable
private fun ButtonStatesRow(style: HealthifyButtonStyle) {
    Row(horizontalArrangement = Arrangement.spacedBy(HealthifyTheme.dimens.space8)) {
        HealthifyButtonImpl(text = "Botón", onClick = {}, style = style)
        HealthifyButtonImpl(text = "Botón", onClick = {}, style = style, forcePressed = true)
        HealthifyButtonImpl(text = "Botón", onClick = {}, style = style, enabled = false)
    }
    HealthifyButtonImpl(text = "Guardando…", onClick = {}, style = style, loading = true)
}

@Preview(name = "Button · Filled", widthDp = 360)
@Composable
private fun ButtonFilledPreview() {
    ComponentPreview { ButtonStatesRow(HealthifyButtonStyle.Filled) }
}

@Preview(name = "Button · Tonal", widthDp = 360)
@Composable
private fun ButtonTonalPreview() {
    ComponentPreview { ButtonStatesRow(HealthifyButtonStyle.Tonal) }
}

@Preview(name = "Button · Outlined", widthDp = 360)
@Composable
private fun ButtonOutlinedPreview() {
    ComponentPreview { ButtonStatesRow(HealthifyButtonStyle.Outlined) }
}

@Preview(name = "Button · Text", widthDp = 360)
@Composable
private fun ButtonTextPreview() {
    ComponentPreview { ButtonStatesRow(HealthifyButtonStyle.Text) }
}

@Preview(name = "Button · Danger", widthDp = 360)
@Composable
private fun ButtonDangerPreview() {
    ComponentPreview { ButtonStatesRow(HealthifyButtonStyle.Danger) }
}

@Preview(name = "Button · full width with icon", widthDp = 360)
@Composable
private fun ButtonFullWidthPreview() {
    ComponentPreview {
        HealthifyButton(text = "Agregar comida", onClick = {}, icon = HealthifyIcons.Add, modifier = Modifier.fillMaxWidth())
    }
}
