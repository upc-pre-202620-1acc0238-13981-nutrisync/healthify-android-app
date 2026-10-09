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
 * Variantes del logo de la sección «Marca — Healthify» del Figma (BR).
 * - [White]: sobre fondos de color o oscuros (S1, S2, S4, S6 normal).
 * - [Color]: a color, sobre fondos claros (S3, S5, S6 sin conexión).
 */
enum class HealthifyLogoVariant(@param:DrawableRes val drawableRes: Int) {
    White(R.drawable.healthify_logo_white),
    Color(R.drawable.healthify_logo_color),
}

/**
 * Isotipo de Healthify (corazón con hoja). Cada pantalla lo dibuja con el tamaño de su frame; la imagen se ajusta
 * sin deformarse.
 *
 * @param decorative `true` si al lado ya hay un texto con el nombre (TalkBack no lo repite).
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

@Preview(name = "Logo · variantes", widthDp = 360)
@Composable
private fun HealthifyLogoPreview() {
    ComponentPreview {
        Row(horizontalArrangement = Arrangement.spacedBy(HealthifyTheme.dimens.space16)) {
            HealthifyLogo(HealthifyLogoVariant.Color, DpSize(72.dp, 63.dp))
        }
    }
}
