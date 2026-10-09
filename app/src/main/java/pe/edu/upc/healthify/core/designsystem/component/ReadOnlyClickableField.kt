package pe.edu.upc.healthify.core.designsystem.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role

/** Un TextField de solo lectura que actúa como botón (Alimento de PT8, «¿Cuándo comiste?», fecha de nacimiento de EV-1). */
@Composable
fun ReadOnlyClickableField(
    value: String,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
    errorText: String? = null,
    enabled: Boolean = true,
    trailingIcon: ImageVector? = null,
) {
    Box(modifier = modifier.fillMaxWidth()) {
        HealthifyTextField(
            value = value,
            onValueChange = {},
            label = label,
            supportingText = supportingText,
            errorText = errorText,
            enabled = enabled,
            trailingIcon = trailingIcon?.let { icon ->
                { Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) }
            },
            modifier = Modifier.fillMaxWidth(),
        )
        // Capa que recibe el toque: el campo no abre el teclado.
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable(
                    enabled = enabled,
                    role = Role.Button,
                    onClickLabel = label,
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick,
                ),
        )
    }
}
