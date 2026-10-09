package pe.edu.upc.healthify.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import pe.edu.upc.healthify.R
import pe.edu.upc.healthify.core.designsystem.icon.HealthifyIcons
import pe.edu.upc.healthify.core.designsystem.theme.HealthifyTheme

/**
 * Tipo de dato del campo: decide el teclado (y en [Password] el enmascarado con botón de mostrar).
 */
enum class HealthifyTextFieldType { Text, Name, Email, Password, Number, Decimal, Phone }

// Alto del campo «Lines=Multi» del Figma.
private val MultiLineFieldMinHeight = 112.dp
private const val MULTI_LINE_MIN_LINES = 3

/**
 * TextField del Figma: etiqueta visible arriba (nunca solo placeholder), campo de 56 dp con borde y
 * texto de ayuda o error debajo (el error lleva ícono).
 *
 * Estados: Enabled (vacío, con [placeholder]), Focused (borde `primary` 2 dp), Filled, Error ([errorText]
 * no nulo: borde y mensaje en `error`) y Disabled.
 *
 * @param isError estado Error sin mensaje (p. ej. la contraseña de S4.1, cuyo mensaje va en el correo).
 * @param singleLine `false` = variante «Lines=Multi» (mín. 112 dp, teclado de texto con saltos de línea).
 * @param labelColor color de la etiqueta sobre fondos oscuros (PR18); `null` = `onSurface`.
 * @param trailingIcon ícono final opcional (48 dp táctil). En [HealthifyTextFieldType.Password] se ignora
 *   y se muestra el botón de mostrar/ocultar.
 */
@Composable
fun HealthifyTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    supportingText: String? = null,
    errorText: String? = null,
    isError: Boolean = errorText != null,
    enabled: Boolean = true,
    singleLine: Boolean = true,
    type: HealthifyTextFieldType = HealthifyTextFieldType.Text,
    imeAction: ImeAction = ImeAction.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    trailingIcon: (@Composable () -> Unit)? = null,
    interactionSource: MutableInteractionSource? = null,
    labelColor: Color? = null,
) {
    HealthifyTextFieldImpl(
        value = value,
        onValueChange = onValueChange,
        label = label,
        modifier = modifier,
        placeholder = placeholder,
        supportingText = supportingText,
        errorText = errorText,
        isError = isError,
        enabled = enabled,
        singleLine = singleLine,
        type = type,
        imeAction = imeAction,
        keyboardActions = keyboardActions,
        trailingIcon = trailingIcon,
        interactionSource = interactionSource,
        forceFocused = false,
        labelColor = labelColor,
    )
}

@Composable
internal fun HealthifyTextFieldImpl(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    supportingText: String? = null,
    errorText: String? = null,
    isError: Boolean = errorText != null,
    enabled: Boolean = true,
    singleLine: Boolean = true,
    type: HealthifyTextFieldType = HealthifyTextFieldType.Text,
    imeAction: ImeAction = ImeAction.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    trailingIcon: (@Composable () -> Unit)? = null,
    interactionSource: MutableInteractionSource? = null,
    forceFocused: Boolean = false,
    labelColor: Color? = null,
) {
    val dimens = HealthifyTheme.dimens
    val scheme = MaterialTheme.colorScheme
    val ext = HealthifyTheme.extendedColors
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val focusedState by source.collectIsFocusedAsState()
    val focused = (focusedState || forceFocused) && enabled
    val showError = isError && enabled
    val isPassword = type == HealthifyTextFieldType.Password
    var passwordVisible by rememberSaveable { mutableStateOf(false) }

    val borderColor = when {
        !enabled -> ext.disabledContainer
        showError -> scheme.error
        focused -> scheme.primary
        else -> scheme.outline
    }
    val borderWidth = if (showError || focused) dimens.borderThick else dimens.borderThin
    val textColor = if (enabled) scheme.onSurface else ext.onDisabled
    val shape = MaterialTheme.shapes.medium

    val trailing: (@Composable () -> Unit)? = if (isPassword) {
        {
            HealthifyIconButton(
                icon = HealthifyIcons.Visibility,
                contentDescription = stringResource(
                    if (passwordVisible) R.string.ds_hide_password else R.string.ds_show_password,
                ),
                onClick = { passwordVisible = !passwordVisible },
                enabled = enabled,
            )
        }
    } else {
        trailingIcon
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(dimens.space8)) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            color = if (enabled) labelColor ?: scheme.onSurface else ext.onDisabled,
        )
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .semantics {
                    contentDescription = label
                    if (errorText != null) error(errorText)
                },
            enabled = enabled,
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = textColor),
            keyboardOptions = keyboardOptionsFor(type, singleLine, imeAction),
            keyboardActions = keyboardActions,
            singleLine = singleLine,
            minLines = if (singleLine) 1 else MULTI_LINE_MIN_LINES,
            visualTransformation = if (isPassword && !passwordVisible) {
                PasswordVisualTransformation()
            } else {
                VisualTransformation.None
            },
            interactionSource = source,
            cursorBrush = SolidColor(if (showError) scheme.error else scheme.primary),
            decorationBox = { innerTextField ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = if (singleLine) dimens.textField else MultiLineFieldMinHeight)
                        .background(scheme.surfaceContainer, shape)
                        .border(borderWidth, borderColor, shape)
                        .padding(start = dimens.space16, end = if (trailing != null) dimens.space0 else dimens.space12),
                    verticalAlignment = if (singleLine) Alignment.CenterVertically else Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(dimens.space8),
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(vertical = dimens.space16),
                    ) {
                        if (value.isEmpty() && placeholder != null) {
                            Text(
                                text = placeholder,
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (enabled) scheme.onSurfaceVariant else ext.onDisabled,
                            )
                        }
                        innerTextField()
                    }
                    if (trailing != null) {
                        Box(modifier = if (singleLine) Modifier else Modifier.padding(top = dimens.space4)) {
                            trailing()
                        }
                    }
                }
            },
        )
        SupportingRow(supportingText = supportingText, errorText = errorText, enabled = enabled)
    }
}

@Composable
private fun SupportingRow(supportingText: String?, errorText: String?, enabled: Boolean) {
    val dimens = HealthifyTheme.dimens
    val scheme = MaterialTheme.colorScheme
    val text = errorText ?: supportingText ?: return
    val isError = errorText != null && enabled
    val color = when {
        !enabled -> HealthifyTheme.extendedColors.onDisabled
        isError -> scheme.error
        else -> scheme.onSurfaceVariant
    }
    Row(
        modifier = Modifier.padding(start = dimens.space16),
        horizontalArrangement = Arrangement.spacedBy(dimens.space4),
        verticalAlignment = Alignment.Top,
    ) {
        if (isError) {
            Icon(
                imageVector = HealthifyIcons.Error,
                contentDescription = null,
                tint = scheme.error,
                modifier = Modifier.size(dimens.iconSmall),
            )
        }
        Text(text = text, style = MaterialTheme.typography.bodySmall, color = color)
    }
}

private fun keyboardOptionsFor(type: HealthifyTextFieldType, singleLine: Boolean, imeAction: ImeAction): KeyboardOptions {
    if (!singleLine) {
        return KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, keyboardType = KeyboardType.Text)
    }
    return when (type) {
        HealthifyTextFieldType.Text -> KeyboardOptions(
            capitalization = KeyboardCapitalization.Sentences,
            keyboardType = KeyboardType.Text,
            imeAction = imeAction,
        )
        HealthifyTextFieldType.Name -> KeyboardOptions(
            capitalization = KeyboardCapitalization.Words,
            keyboardType = KeyboardType.Text,
            imeAction = imeAction,
        )
        HealthifyTextFieldType.Email -> KeyboardOptions(
            autoCorrectEnabled = false,
            keyboardType = KeyboardType.Email,
            imeAction = imeAction,
        )
        HealthifyTextFieldType.Password -> KeyboardOptions(
            autoCorrectEnabled = false,
            keyboardType = KeyboardType.Password,
            imeAction = imeAction,
        )
        HealthifyTextFieldType.Number -> KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = imeAction)
        HealthifyTextFieldType.Decimal -> KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = imeAction)
        HealthifyTextFieldType.Phone -> KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = imeAction)
    }
}

@Composable
private fun TextFieldStates(singleLine: Boolean) {
    val modifier = Modifier.fillMaxWidth()
    HealthifyTextFieldImpl(
        value = "",
        onValueChange = {},
        label = "Etiqueta",
        placeholder = "Texto de ejemplo",
        supportingText = "Texto de ayuda",
        singleLine = singleLine,
        modifier = modifier,
    )
    HealthifyTextFieldImpl(
        value = "Texto de ejemplo",
        onValueChange = {},
        label = "Etiqueta",
        supportingText = "Texto de ayuda",
        singleLine = singleLine,
        forceFocused = true,
        modifier = modifier,
    )
    HealthifyTextFieldImpl(
        value = "Texto de ejemplo",
        onValueChange = {},
        label = "Etiqueta",
        supportingText = "Texto de ayuda",
        singleLine = singleLine,
        modifier = modifier,
    )
    HealthifyTextFieldImpl(
        value = "Texto de ejemplo",
        onValueChange = {},
        label = "Etiqueta",
        errorText = "Texto de ayuda",
        singleLine = singleLine,
        modifier = modifier,
    )
    HealthifyTextFieldImpl(
        value = "Texto de ejemplo",
        onValueChange = {},
        label = "Etiqueta",
        supportingText = "Texto de ayuda",
        singleLine = singleLine,
        enabled = false,
        modifier = modifier,
    )
}

@Preview(name = "TextField · una línea (Enabled/Focused/Filled/Error/Disabled)", widthDp = 360, heightDp = 800)
@Composable
private fun TextFieldSinglePreview() {
    ComponentPreview { TextFieldStates(singleLine = true) }
}

@Preview(name = "TextField · multilínea", widthDp = 360, heightDp = 1000)
@Composable
private fun TextFieldMultiPreview() {
    ComponentPreview { TextFieldStates(singleLine = false) }
}

@Preview(name = "TextField · contraseña", widthDp = 360)
@Composable
private fun TextFieldPasswordPreview() {
    ComponentPreview {
        HealthifyTextField(
            value = "secreto123",
            onValueChange = {},
            label = "Contraseña",
            type = HealthifyTextFieldType.Password,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
