package pe.edu.upc.healthify.features.main.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import pe.edu.upc.healthify.R
import pe.edu.upc.healthify.core.designsystem.component.HealthifyButton
import pe.edu.upc.healthify.core.designsystem.component.HealthifyButtonStyle
import pe.edu.upc.healthify.core.designsystem.icon.HealthifyIcons
import pe.edu.upc.healthify.core.designsystem.theme.HealthifyTheme
import pe.edu.upc.healthify.features.iam.domain.valueobject.PreferredLanguage

/** PT21.I / PR20.I · hoja «IDIOMA»: Español y English; la opción actual lleva el check. */
@Composable
internal fun ColumnScope.LanguageSheetContent(
    selected: PreferredLanguage,
    onSelect: (PreferredLanguage) -> Unit,
    onClose: () -> Unit,
) {
    val dimens = HealthifyTheme.dimens
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(start = dimens.space24, end = dimens.space24, top = dimens.space16, bottom = dimens.space24),
        verticalArrangement = Arrangement.spacedBy(dimens.space16),
    ) {
        Text(
            text = stringResource(R.string.settings_language_sheet_title),
            style = MaterialTheme.typography.headlineSmall,
            color = scheme.onSurface,
            modifier = Modifier.semantics { heading() },
        )
        Column {
            PreferredLanguage.entries.forEachIndexed { index, language ->
                if (index > 0) HorizontalDivider(color = scheme.outlineVariant, thickness = dimens.borderThin)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = dimens.button)
                        .selectable(selected = language == selected, role = Role.RadioButton, onClick = { onSelect(language) }),
                    horizontalArrangement = Arrangement.spacedBy(dimens.space8),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Cada idioma se nombra en su propio idioma (no se traduce).
                    Text(
                        text = stringResource(language.labelRes),
                        style = MaterialTheme.typography.bodyLarge,
                        color = scheme.onSurface,
                        modifier = Modifier.weight(1f),
                    )
                    if (language == selected) {
                        Icon(imageVector = HealthifyIcons.Check, contentDescription = null, tint = scheme.primary)
                    }
                }
            }
        }
        HealthifyButton(
            text = stringResource(R.string.common_close),
            onClick = onClose,
            style = HealthifyButtonStyle.Text,
            modifier = Modifier.align(Alignment.Start),
        )
    }
}

internal val PreferredLanguage.labelRes: Int
    get() = when (this) {
        PreferredLanguage.SPANISH -> R.string.language_spanish
        PreferredLanguage.ENGLISH -> R.string.language_english
    }
