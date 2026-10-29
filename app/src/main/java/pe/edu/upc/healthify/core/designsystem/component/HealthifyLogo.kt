package pe.edu.upc.healthify.core.designsystem.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import pe.edu.upc.healthify.R
import pe.edu.upc.healthify.core.designsystem.theme.HealthifyTheme

/**
 * Logo variants from the Figma "Brand — Healthify" section.
 * - [White]: on colored or dark backgrounds (splash, welcome, sign-in, session expired when online).
 * - [Color]: in color, on light backgrounds (sign-up, loading, session expired when offline).
 */
enum class HealthifyLogoVariant(@param:DrawableRes val drawableRes: Int) {
    White(R.drawable.healthify_logo_white),
    Color(R.drawable.healthify_logo_color),
}

/**
 * Healthify isotype (heart with a leaf). Each screen draws it at its frame's size; the image scales
 * without distortion.
 *
 * @param decorative `true` if a text with the name is already next to it (TalkBack does not repeat it).
 */
@Composable
fun HealthifyLogo(
    variant: HealthifyLogoVariant,
    size: DpSize,
    modifier: Modifier = Modifier,
    decorative: Boolean = false,
) {
    Image(
        painter = painterResource(variant.drawableRes),
        contentDescription = if (decorative) null else stringResource(R.string.brand_logo_cd),
        contentScale = ContentScale.Fit,
        modifier = modifier.size(size),
    )
}

@Preview(name = "Logo · variants", widthDp = 360)
@Composable
private fun HealthifyLogoPreview() {
    ComponentPreview {
        Row(horizontalArrangement = Arrangement.spacedBy(HealthifyTheme.dimens.space16)) {
            HealthifyLogo(HealthifyLogoVariant.Color, DpSize(72.dp, 63.dp))
        }
    }
}
