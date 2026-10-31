package pe.edu.upc.healthify.features.iam.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import pe.edu.upc.healthify.R
import pe.edu.upc.healthify.core.common.legal.LegalLinks
import pe.edu.upc.healthify.core.designsystem.component.HealthifyCheckboxRow
import pe.edu.upc.healthify.core.designsystem.theme.HealthifyTheme

/**
 * "I accept the terms and conditions" on sign-up. Only "terms and conditions" is a link: it opens
 * [LegalLinks.TERMS_URL] in the browser; the rest of the row toggles the box.
 */
@Composable
fun TermsAcceptance(
    accepted: Boolean,
    onAcceptedChange: (Boolean) -> Unit,
    isError: Boolean,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val scheme = MaterialTheme.colorScheme
    val linkText = stringResource(R.string.sign_up_terms_link)
    val template = stringResource(R.string.sign_up_terms_accept, linkText)
    val linkStart = template.indexOf(linkText)
    val linkStyle = TextLinkStyles(
        style = SpanStyle(
            color = scheme.primary,
            fontWeight = FontWeight.SemiBold,
            textDecoration = TextDecoration.Underline,
        ),
    )
    val label = buildAnnotatedString {
        if (linkStart < 0) {
            append(template)
        } else {
            append(template.substring(0, linkStart))
            withLink(LinkAnnotation.Url(LegalLinks.TERMS_URL, linkStyle)) { append(linkText) }
            append(template.substring(linkStart + linkText.length))
        }
    }
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(HealthifyTheme.dimens.space4)) {
        HealthifyCheckboxRow(
            label = label,
            checked = accepted,
            onCheckedChange = onAcceptedChange,
            enabled = enabled,
            isError = isError,
        )
        if (isError) {
            Text(
                text = stringResource(R.string.sign_up_error_terms),
                style = MaterialTheme.typography.bodySmall,
                color = scheme.error,
            )
        }
    }
}
