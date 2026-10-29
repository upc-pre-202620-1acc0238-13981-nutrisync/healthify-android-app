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
 * Field data type: picks the keyboard (and for [Password], the masking with a show button).
 */
enum class HealthifyTextFieldType { Text, Name, Email, Password, Number, Decimal, Phone }

// Height of the Figma "Lines=Multi" field.
private val MultiLineFieldMinHeight = 112.dp
private const val MULTI_LINE_MIN_LINES = 3

/**
 * Figma TextField: visible label on top (never placeholder only), 56 dp outlined field and
 * helper or error text below (the error has an icon).
 *
 * States: Enabled (empty, with [placeholder]), Focused (2 dp `primary` border), Filled, Error ([errorText]
 * not null: border and message in `error`) and Disabled.
 *
 * @param isError Error state without a message (e.g. the sign-in password, whose message goes under the email).
 * @param singleLine `false` = "Lines=Multi" variant (min. 112 dp, text keyboard with line breaks).
 * @param labelColor label color on dark backgrounds; `null` = `onSurface`.
 * @param trailingIcon optional trailing icon (48 dp touch target). Ignored for [HealthifyTextFieldType.Password],
 *   which shows the show/hide button.
 * @param suffix unit at the end of the field («kg», «cm», «%»), Label/Large in `onSurfaceVariant`. It goes as text
 *   with a 16 dp end margin, without the [trailingIcon] touch box, so it does not get cut off in narrow fields.
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
    suffix: String? = null,
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
        suffix = suffix,
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
    suffix: String? = null,
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
                // Open eye = "show"; crossed eye = "hide" (the password is already visible).
                icon = if (passwordVisible) HealthifyIcons.VisibilityOff else HealthifyIcons.Visibility,
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
                        .padding(
                            start = dimens.space16,
                            end = when {
                                trailing != null -> dimens.space0
                                suffix != null -> dimens.space16
                                else -> dimens.space12
                            },
                        ),
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
                    if (suffix != null) {
                        // The weighted text is measured last: the unit always keeps its width and is never cut off.
                        Text(
                            text = suffix,
                            style = MaterialTheme.typography.labelLarge,
                            color = if (enabled) scheme.onSurfaceVariant else ext.onDisabled,
                            maxLines = 1,
                            softWrap = false,
                            modifier = if (singleLine) Modifier else Modifier.padding(top = dimens.space16),
                        )
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

@Preview(name = "TextField · single line (Enabled/Focused/Filled/Error/Disabled)", widthDp = 360, heightDp = 800)
@Composable
private fun TextFieldSinglePreview() {
    ComponentPreview { TextFieldStates(singleLine = true) }
}

@Preview(name = "TextField · multiline", widthDp = 360, heightDp = 1000)
@Composable
private fun TextFieldMultiPreview() {
    ComponentPreview { TextFieldStates(singleLine = false) }
}

@Preview(name = "TextField · with unit (half column)", widthDp = 360)
@Composable
private fun TextFieldSuffixPreview() {
    ComponentPreview {
        HealthifyTextField(
            value = "74.2",
            onValueChange = {},
            label = "Peso",
            type = HealthifyTextFieldType.Decimal,
            suffix = "kg",
            modifier = Modifier.fillMaxWidth(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(HealthifyTheme.dimens.space8)) {
            HealthifyTextField(
                value = "92",
                onValueChange = {},
                label = "Cintura",
                type = HealthifyTextFieldType.Decimal,
                suffix = "cm",
                modifier = Modifier.weight(1f),
            )
            HealthifyTextField(
                value = "",
                onValueChange = {},
                label = "Grasa corporal",
                placeholder = "Ej. 24",
                type = HealthifyTextFieldType.Decimal,
                suffix = "%",
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Preview(name = "TextField · password", widthDp = 360)
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
