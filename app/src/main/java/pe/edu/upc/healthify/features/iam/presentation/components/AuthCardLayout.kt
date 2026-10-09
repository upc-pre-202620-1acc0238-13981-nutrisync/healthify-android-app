package pe.edu.upc.healthify.features.iam.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import pe.edu.upc.healthify.core.designsystem.component.SystemBarsAppearance
import pe.edu.upc.healthify.core.designsystem.theme.HealthifyTheme
import pe.edu.upc.healthify.core.designsystem.theme.elevation

// Decoración de fondo de S3/S4 («Decoración · forma»).
private val LimeShapeSize = 200.dp
private val LimeShapeOffsetX = 230.dp
private val LimeShapeOffsetY = (-85).dp
private val GreenShapeSize = 140.dp
private val GreenShapeOffsetX = (-55).dp
private const val LIME_SHAPE_ALPHA = 0.28f
private const val GREEN_SHAPE_ALPHA = 0.25f

/**
 * Patrón de S3 y S4: fondo de color con dos formas decorativas, un encabezado opcional (logo de S4) y una tarjeta
 * blanca con esquinas superiores de 28 dp que llega al borde inferior.
 *
 * Teclado (S3.K / S4.3): el contenido se desplaza con `imePadding` y, mientras el teclado está abierto, la acción
 * principal [keyboardAction] queda fija sobre él («Acción sobre el teclado», Elevation/2). Con el teclado cerrado
 * la acción va dentro de la tarjeta; [content] recibe `true` cuando debe omitirla.
 *
 * @param greenShapeOffsetY S3 la ubica en y = 9 y S4 en y = 75 (relativo al frame).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AuthCardLayout(
    background: Color,
    greenShapeOffsetY: Dp,
    keyboardAction: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    header: (@Composable ColumnScope.() -> Unit)? = null,
    content: @Composable ColumnScope.(keyboardOpen: Boolean) -> Unit,
) {
    val dimens = HealthifyTheme.dimens
    val scheme = MaterialTheme.colorScheme
    val keyboardOpen = WindowInsets.isImeVisible
    val cardShape = RoundedCornerShape(topStart = dimens.radiusDialog, topEnd = dimens.radiusDialog)
    val navigationBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val density = LocalDensity.current
    var headerHeight by remember { mutableStateOf(dimens.space0) }
    SystemBarsAppearance(darkBackground = false)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(background),
    ) {
        Box(
            modifier = Modifier
                .offset(LimeShapeOffsetX, LimeShapeOffsetY)
                .size(LimeShapeSize)
                .background(scheme.tertiary.copy(alpha = LIME_SHAPE_ALPHA), CircleShape),
        )
        Box(
            modifier = Modifier
                .offset(GreenShapeOffsetX, greenShapeOffsetY)
                .size(GreenShapeSize)
                .background(HealthifyTheme.extendedColors.brandDecor.copy(alpha = GREEN_SHAPE_ALPHA), CircleShape),
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding(),
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) {
                // La tarjeta llega al borde inferior aunque su contenido sea corto, y crece con scroll si es largo
                // (errores, fuente al 200 %): su alto mínimo es lo que queda bajo el encabezado.
                val headerSpace = if (header != null) headerHeight + dimens.space24 else dimens.space0
                val cardMinHeight = (maxHeight - statusBarTop - dimens.space24 - headerSpace).coerceAtLeast(dimens.space0)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .statusBarsPadding()
                        .padding(top = dimens.space24),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    if (header != null) {
                        Column(
                            modifier = Modifier.onSizeChanged { headerHeight = with(density) { it.height.toDp() } },
                            horizontalAlignment = Alignment.CenterHorizontally,
                            content = header,
                        )
                        Spacer(modifier = Modifier.height(dimens.space24))
                    }
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = cardMinHeight)
                            .elevation(dimens.elevation1, cardShape)
                            .background(scheme.surfaceContainerLowest, cardShape)
                            .padding(
                                start = dimens.space16,
                                top = dimens.space16,
                                end = dimens.space16,
                                bottom = dimens.space16 + if (keyboardOpen) dimens.space0 else navigationBottom,
                            ),
                        verticalArrangement = Arrangement.spacedBy(dimens.space16),
                    ) {
                        content(keyboardOpen)
                    }
                }
            }
            if (keyboardOpen) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .elevation(dimens.elevation2)
                        .background(scheme.surfaceContainerLowest)
                        .padding(dimens.space16),
                ) {
                    keyboardAction()
                }
            }
        }
    }
}
