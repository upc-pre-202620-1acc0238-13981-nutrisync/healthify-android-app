package pe.edu.upc.healthify.core.designsystem.component

import android.provider.Settings
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import pe.edu.upc.healthify.R
import pe.edu.upc.healthify.core.designsystem.theme.HealthifyTheme

// Medidas de las variantes del Skeleton del Figma.
private val LineHeight = 16.dp
private val SmallLineHeight = 12.dp
private val CardTitleHeight = 20.dp
private val CardCaptionHeight = 14.dp
private val CardBodyHeight = 96.dp
private val ChartHeight = 180.dp
private val AvatarSize = 40.dp
private const val PULSE_MIN_ALPHA = 0.4f
private const val PULSE_DURATION_MS = 900

/** Línea: [lines] bloques de texto de 16 dp. */
@Composable
fun SkeletonLine(modifier: Modifier = Modifier, lines: Int = 2) {
    SkeletonContainer(modifier = modifier, padded = false) {
        repeat(lines) { SkeletonBlock(height = LineHeight) }
    }
}

/** Tarjeta: título, bloque de contenido y pie. */
@Composable
fun SkeletonCard(modifier: Modifier = Modifier) {
    SkeletonContainer(modifier = modifier) {
        SkeletonBlock(height = CardTitleHeight, widthFraction = 0.5f)
        SkeletonBlock(height = CardBodyHeight, shape = MaterialTheme.shapes.medium)
        SkeletonBlock(height = CardCaptionHeight, widthFraction = 0.8f)
    }
}

/** Ítem de lista: avatar circular y dos líneas. */
@Composable
fun SkeletonListItem(modifier: Modifier = Modifier) {
    val dimens = HealthifyTheme.dimens
    SkeletonPulse(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.large)
                .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                .padding(dimens.space16),
            horizontalArrangement = Arrangement.spacedBy(dimens.space16),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(AvatarSize)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(dimens.space8)) {
                SkeletonBlock(height = LineHeight, widthFraction = 0.8f)
                SkeletonBlock(height = SmallLineHeight, widthFraction = 0.5f)
            }
        }
    }
}

/** Gráfico: área del gráfico y leyenda. */
@Composable
fun SkeletonChart(modifier: Modifier = Modifier) {
    SkeletonContainer(modifier = modifier) {
        SkeletonBlock(height = ChartHeight, shape = MaterialTheme.shapes.medium)
        SkeletonBlock(height = SmallLineHeight, widthFraction = 0.55f)
    }
}

/** Bloque suelto del Skeleton (`surfaceContainerHigh`) para armar placeholders propios de una pantalla. */
@Composable
fun SkeletonBlock(
    height: Dp,
    modifier: Modifier = Modifier,
    widthFraction: Float = 1f,
    shape: Shape = RoundedCornerShape(HealthifyTheme.dimens.radiusSkeletonText),
) {
    Box(
        modifier = modifier
            .fillMaxWidth(widthFraction)
            .height(height)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
    )
}

@Composable
private fun SkeletonContainer(
    modifier: Modifier,
    padded: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    val dimens = HealthifyTheme.dimens
    SkeletonPulse(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (padded) {
                        Modifier
                            .clip(MaterialTheme.shapes.large)
                            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                            .padding(dimens.space16)
                    } else {
                        Modifier
                    },
                ),
            verticalArrangement = Arrangement.spacedBy(dimens.space8),
            content = content,
        )
    }
}

/**
 * Pulso de opacidad del placeholder. Respeta «reducir movimiento» (escala de animación del sistema en 0):
 * en ese caso queda estático. TalkBack lo anuncia una sola vez como «Cargando».
 */
@Composable
private fun SkeletonPulse(modifier: Modifier, content: @Composable () -> Unit) {
    val context = LocalContext.current
    val animationsEnabled = remember(context) {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f
    }
    val loading = stringResource(R.string.ds_cd_loading)
    val alphaModifier = if (animationsEnabled) {
        val transition = rememberInfiniteTransition(label = "skeleton")
        val alpha by transition.animateFloat(
            initialValue = 1f,
            targetValue = PULSE_MIN_ALPHA,
            animationSpec = infiniteRepeatable(tween(PULSE_DURATION_MS), RepeatMode.Reverse),
            label = "skeletonAlpha",
        )
        Modifier.graphicsLayer { this.alpha = alpha }
    } else {
        Modifier
    }
    Box(
        modifier = modifier
            .then(alphaModifier)
            .clearAndSetSemantics { contentDescription = loading },
    ) {
        content()
    }
}

@Preview(name = "Skeleton · Línea / Tarjeta / ListItem / Gráfico", widthDp = 360, heightDp = 720)
@Composable
private fun SkeletonPreview() {
    ComponentPreview {
        SkeletonLine()
        SkeletonCard()
        SkeletonListItem()
        SkeletonChart()
    }
}
