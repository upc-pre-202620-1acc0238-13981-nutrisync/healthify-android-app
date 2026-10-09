package pe.edu.upc.healthify.features.intake.presentation.screen

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import pe.edu.upc.healthify.R
import pe.edu.upc.healthify.core.designsystem.component.FailureState
import pe.edu.upc.healthify.core.designsystem.component.HealthifyButton
import pe.edu.upc.healthify.core.designsystem.component.HealthifyButtonStyle
import pe.edu.upc.healthify.core.designsystem.component.HealthifyCard
import pe.edu.upc.healthify.core.designsystem.component.HealthifyDialog
import pe.edu.upc.healthify.core.designsystem.component.HealthifyTopAppBar
import pe.edu.upc.healthify.core.designsystem.component.ObserveEvents
import pe.edu.upc.healthify.core.designsystem.component.SystemBarsAppearance
import pe.edu.upc.healthify.core.designsystem.component.ViewfinderCorners
import pe.edu.upc.healthify.core.designsystem.icon.HealthifyIcons
import pe.edu.upc.healthify.core.designsystem.theme.HealthifyTheme
import pe.edu.upc.healthify.features.intake.domain.entity.MealLogOutcome
import pe.edu.upc.healthify.features.intake.domain.entity.MealPhotoAlternative
import pe.edu.upc.healthify.features.intake.domain.entity.MealPhotoAnalysis
import pe.edu.upc.healthify.features.intake.domain.valueobject.MealFood
import pe.edu.upc.healthify.features.intake.domain.valueobject.MealPhotoAnalysisId
import pe.edu.upc.healthify.features.intake.presentation.components.AdjustPortionForm
import pe.edu.upc.healthify.features.intake.presentation.components.MealCameraPreview
import pe.edu.upc.healthify.features.intake.presentation.components.MealCameraState
import pe.edu.upc.healthify.features.intake.presentation.components.MealTimePickerDialog
import pe.edu.upc.healthify.features.intake.presentation.components.PlanAdherenceCard
import pe.edu.upc.healthify.features.intake.presentation.components.confidencePercent
import pe.edu.upc.healthify.features.intake.presentation.components.gramsText
import pe.edu.upc.healthify.features.intake.presentation.components.rememberMealCameraState
import pe.edu.upc.healthify.features.intake.presentation.state.AnalysisFailure
import pe.edu.upc.healthify.features.intake.presentation.state.CameraPermissionStatus
import pe.edu.upc.healthify.features.intake.presentation.state.MealFormState
import pe.edu.upc.healthify.features.intake.presentation.state.MealPhotoEvent
import pe.edu.upc.healthify.features.intake.presentation.state.MealPhotoStep
import pe.edu.upc.healthify.features.intake.presentation.state.MealPhotoUiState
import pe.edu.upc.healthify.features.intake.presentation.viewmodel.MealPhotoViewModel
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime

// Medidas de PT5: visor de 262 dp con esquinas, disparador de 84 dp (anillo de 3 dp) con círculo de 72 dp.
private val ViewfinderSize = 262.dp
private val ShutterSize = 84.dp
private val ShutterInnerSize = 72.dp
private val ShutterRingWidth = 3.dp

// PT7: ícono del plato de 41 dp y anillo de confianza de 72 dp (8 dp de grosor).
private val DishIconSize = 41.dp
private val ConfidenceRingSize = 72.dp
private val ConfidenceRingWidth = 8.dp
private const val FULL_SWEEP = 360f
private const val START_ANGLE = -90f

data class MealPhotoActions(
    val onBack: () -> Unit = {},
    val onCaptureStarted: () -> Unit = {},
    val onPhotoCaptured: (String) -> Unit = {},
    val onCaptureFailed: () -> Unit = {},
    val onRegisterManually: () -> Unit = {},
    val onOpenSettings: () -> Unit = {},
    val onRetake: () -> Unit = {},
    val onUsePhoto: () -> Unit = {},
    val onRetryAnalysis: () -> Unit = {},
    val onAlternativeSelected: (Int?) -> Unit = {},
    val onPlanAnswer: (Boolean) -> Unit = {},
    val onConfirmProposal: () -> Unit = {},
    val onAdjust: () -> Unit = {},
    val onPortionChange: (String) -> Unit = {},
    val onMealTimeClick: () -> Unit = {},
    val onMealTimeSelected: (LocalDateTime) -> Unit = {},
    val onMealTimeDismiss: () -> Unit = {},
    val onChangeFood: () -> Unit = {},
    val onConfirmAdjustment: () -> Unit = {},
    val onRetrySave: () -> Unit = {},
)

/**
 * Registro por foto: PT5 Cámara (+ PT5.M), PT6, PT6.1, PT7 (+ PT7.2, PT7.3) y PT8. La foto nunca se guarda en el
 * servidor; en el teléfono vive en `cacheDir` hasta terminar.
 */
@Composable
fun MealPhotoScreen(
    onExit: () -> Unit,
    onOpenManual: () -> Unit,
    onPickFood: () -> Unit,
    onLogged: (MealLogOutcome) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MealPhotoViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    ObserveEvents(viewModel.events) { event ->
        when (event) {
            MealPhotoEvent.Exit -> onExit()
            MealPhotoEvent.OpenManual -> onOpenManual()
            MealPhotoEvent.PickFood -> onPickFood()
            is MealPhotoEvent.Logged -> onLogged(event.outcome)
        }
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        viewModel.onCameraPermissionResult(it)
    }
    LaunchedEffect(state.step, state.cameraPermission) {
        if (state.step == MealPhotoStep.CAMERA && state.cameraPermission == CameraPermissionStatus.UNKNOWN) {
            if (context.hasCameraPermission()) {
                viewModel.onCameraPermissionResult(true)
            } else {
                permissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }
    BackHandler(onBack = viewModel::onBack)
    MealPhotoContent(
        state = state,
        hasCamera = remember(context) { context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY) },
        actions = MealPhotoActions(
            onBack = viewModel::onBack,
            onCaptureStarted = viewModel::onCaptureStarted,
            onPhotoCaptured = viewModel::onPhotoCaptured,
            onCaptureFailed = viewModel::onCaptureFailed,
            onRegisterManually = viewModel::onRegisterManually,
            onOpenSettings = { context.openAppSettings() },
            onRetake = viewModel::onRetake,
            onUsePhoto = viewModel::onUsePhoto,
            onRetryAnalysis = viewModel::onRetryAnalysis,
            onAlternativeSelected = viewModel::onAlternativeSelected,
            onPlanAnswer = viewModel::onPlanAnswer,
            onConfirmProposal = viewModel::onConfirmProposal,
            onAdjust = viewModel::onAdjust,
            onPortionChange = viewModel::onPortionChange,
            onMealTimeClick = viewModel::onMealTimeClick,
            onMealTimeSelected = viewModel::onMealTimeSelected,
            onMealTimeDismiss = viewModel::onMealTimeDismiss,
            onChangeFood = viewModel::onChangeFood,
            onConfirmAdjustment = viewModel::onConfirmAdjustment,
            onRetrySave = viewModel::onRetrySave,
        ),
        modifier = modifier,
        cameraContent = { cameraModifier, holder ->
            val cameraState = rememberMealCameraState()
            SideEffect { holder.value = cameraState }
            MealCameraPreview(state = cameraState, modifier = cameraModifier)
        },
    )
}

/** Puente entre la vista de cámara (solo en el dispositivo) y el botón disparador. */
class CameraHolder {
    var value: MealCameraState? = null
}

@Composable
fun MealPhotoContent(
    state: MealPhotoUiState,
    actions: MealPhotoActions,
    modifier: Modifier = Modifier,
    hasCamera: Boolean = true,
    cameraContent: (@Composable (Modifier, CameraHolder) -> Unit)? = null,
) {
    when (state.step) {
        MealPhotoStep.RESOLVING -> CameraStep(state, actions, modifier, hasCamera, cameraContent = null)
        MealPhotoStep.CAMERA -> CameraStep(state, actions, modifier, hasCamera, cameraContent)
        MealPhotoStep.PREVIEW -> PreviewStep(state, actions, modifier)
        MealPhotoStep.ANALYZING -> LightScaffold(stringResource(R.string.meal_photo_preview_title), actions.onBack, modifier) {
            AnalyzingBody()
        }
        MealPhotoStep.SAVED_OFFLINE -> LightScaffold(stringResource(R.string.meal_photo_preview_title), actions.onBack, modifier) {
            MessageBody(
                title = stringResource(R.string.meal_photo_offline_title),
                text = stringResource(R.string.meal_photo_offline_body),
            )
            HealthifyButton(
                text = stringResource(R.string.meal_photo_offline_later),
                onClick = actions.onBack,
                modifier = Modifier.fillMaxWidth(),
            )
            HealthifyButton(
                text = stringResource(R.string.meal_photo_permission_manual),
                onClick = actions.onRegisterManually,
                style = HealthifyButtonStyle.Text,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        MealPhotoStep.PROPOSAL -> LightScaffold(stringResource(R.string.meal_photo_proposal_title), actions.onBack, modifier) {
            state.analysis?.let { ProposalBody(state, it, actions) }
        }
        MealPhotoStep.ADJUST -> LightScaffold(stringResource(R.string.adjust_title), actions.onBack, modifier) {
            val analysis = state.analysis
            if (analysis != null) {
                AdjustPortionForm(
                    foodName = (state.adjustFood ?: analysis.food).name,
                    form = state.form,
                    today = state.today,
                    proposedGrams = analysis.estimatedGrams,
                    onChangeFood = actions.onChangeFood,
                    onPortionChange = actions.onPortionChange,
                    onMealTimeClick = actions.onMealTimeClick,
                    onPlanAnswer = actions.onPlanAnswer,
                    onConfirm = actions.onConfirmAdjustment,
                    foodNotResolved = state.foodNotResolved,
                    confirming = state.isSaving,
                )
            }
        }
        MealPhotoStep.SAVE_FAILED -> FailureScaffold(modifier) {
            FailureState(
                title = stringResource(R.string.meal_photo_save_failed_title),
                text = stringResource(R.string.meal_photo_save_failed_body),
                actionLabel = stringResource(R.string.meal_photo_retry),
                onAction = actions.onRetrySave,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        MealPhotoStep.NOT_RECOGNIZED -> NotRecognizedStep(actions, modifier)
        MealPhotoStep.ANALYSIS_FAILED -> FailureScaffold(modifier) { AnalysisFailedBody(state.failure, actions) }
    }
    if (state.form.showTimePicker) {
        MealTimePickerDialog(
            initial = state.form.mealTime.toLocalDateTime(),
            today = state.today,
            earliestDay = state.earliestDay,
            onConfirm = actions.onMealTimeSelected,
            onDismiss = actions.onMealTimeDismiss,
        )
    }
}

// ----- PT5 · Cámara (+ PT5.M) -----

@Composable
private fun CameraStep(
    state: MealPhotoUiState,
    actions: MealPhotoActions,
    modifier: Modifier,
    hasCamera: Boolean,
    cameraContent: (@Composable (Modifier, CameraHolder) -> Unit)?,
) {
    val dimens = HealthifyTheme.dimens
    val scheme = MaterialTheme.colorScheme
    val holder = remember { CameraHolder() }
    SystemBarsAppearance(darkBackground = true)
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(scheme.inverseSurface)
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        HealthifyTopAppBar(
            title = stringResource(R.string.meal_photo_camera_title),
            onBack = actions.onBack,
            containerColor = scheme.inverseSurface,
            contentColor = scheme.inverseOnSurface,
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = dimens.space16),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(dimens.space64 + dimens.space48))
            Text(
                text = stringResource(R.string.meal_photo_hint),
                style = MaterialTheme.typography.bodySmall,
                color = scheme.inverseOnSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(dimens.space32))
            Box(
                modifier = Modifier
                    .widthIn(max = ViewfinderSize)
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(MaterialTheme.shapes.large)
                    .background(scheme.inverseOnSurface.copy(alpha = VIEWFINDER_ALPHA)),
            ) {
                val showCamera = state.step == MealPhotoStep.CAMERA &&
                    state.cameraPermission == CameraPermissionStatus.GRANTED && hasCamera
                if (showCamera && cameraContent != null) cameraContent(Modifier.fillMaxSize(), holder)
                ViewfinderCorners(color = scheme.secondary, modifier = Modifier.fillMaxSize())
            }
            if (state.captureFailed) {
                Text(
                    text = stringResource(R.string.meal_photo_capture_failed),
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.inverseOnSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = dimens.space16),
                )
            }
            Spacer(Modifier.height(dimens.space64 + dimens.space48))
        }
        Shutter(
            enabled = state.step == MealPhotoStep.CAMERA && state.cameraPermission == CameraPermissionStatus.GRANTED &&
                !state.isCapturing,
            onClick = {
                val camera = holder.value ?: return@Shutter
                actions.onCaptureStarted()
                camera.capture(onSaved = actions.onPhotoCaptured, onError = actions.onCaptureFailed)
            },
        )
        HealthifyButton(
            text = stringResource(R.string.meal_photo_manual_instead),
            onClick = actions.onRegisterManually,
            style = HealthifyButtonStyle.Text,
            contentColor = scheme.inversePrimary,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = dimens.space16, vertical = dimens.space8),
        )
    }
    if (!hasCamera && state.step == MealPhotoStep.CAMERA) {
        HealthifyDialog(
            title = stringResource(R.string.meal_photo_permission_title),
            text = stringResource(R.string.meal_photo_no_camera),
            confirmLabel = stringResource(R.string.meal_photo_permission_manual),
            onConfirm = actions.onRegisterManually,
            onDismiss = actions.onRegisterManually,
            dismissLabel = null,
        )
    } else if (state.cameraPermission == CameraPermissionStatus.DENIED) {
        // PT5.M · secundario «Registrar a mano» (PT9), principal «Abrir ajustes».
        HealthifyDialog(
            title = stringResource(R.string.meal_photo_permission_title),
            text = stringResource(R.string.meal_photo_permission_body),
            confirmLabel = stringResource(R.string.camera_permission_open_settings),
            onConfirm = actions.onOpenSettings,
            onDismiss = actions.onRegisterManually,
            dismissLabel = stringResource(R.string.meal_photo_permission_manual),
        )
    }
}

/** Disparador: anillo `secondary` de 84 dp y círculo claro de 72 dp. */
@Composable
private fun Shutter(enabled: Boolean, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val description = stringResource(R.string.meal_photo_cd_capture)
    Box(
        modifier = Modifier
            .size(ShutterSize)
            .clip(CircleShape)
            .border(ShutterRingWidth, scheme.secondary, CircleShape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(ShutterInnerSize)
                .clip(CircleShape)
                .background(if (enabled) scheme.surfaceContainerLowest else scheme.outline),
        )
    }
}

// ----- PT6 · Previsualización -----

@Composable
private fun PreviewStep(state: MealPhotoUiState, actions: MealPhotoActions, modifier: Modifier) {
    val dimens = HealthifyTheme.dimens
    LightScaffold(
        title = stringResource(R.string.meal_photo_preview_title),
        onBack = actions.onBack,
        modifier = modifier,
        bottomBar = {
            HealthifyButton(
                text = stringResource(R.string.meal_photo_retake),
                onClick = actions.onRetake,
                style = HealthifyButtonStyle.Text,
                modifier = Modifier.fillMaxWidth(),
            )
        },
    ) {
        HealthifyCard(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.extraLarge) {
            Box(
                modifier = Modifier
                    .padding(dimens.space16)
                    .fillMaxWidth()
                    .aspectRatio(PREVIEW_ASPECT)
                    .clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.surfaceContainer),
                contentAlignment = Alignment.Center,
            ) {
                val path = state.photoPath
                if (path != null) {
                    AsyncImage(
                        model = File(path),
                        contentDescription = stringResource(R.string.meal_photo_cd_preview),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(dimens.space16)) {
                        Icon(HealthifyIcons.Camera, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text(
                            text = stringResource(R.string.meal_photo_captured),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
        HealthifyButton(
            text = stringResource(R.string.meal_photo_use),
            onClick = actions.onUsePhoto,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

// ----- PT6.1 · Viendo tu foto… -----

@Composable
private fun AnalyzingBody() {
    val dimens = HealthifyTheme.dimens
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {},
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(dimens.space24),
    ) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(dimens.touchMin))
        Text(
            text = stringResource(R.string.meal_photo_analyzing),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Text(
            // DECISIÓN PT6.1 (IN-7): el frame dice «La estimación se hace en tu teléfono», pero la hace el servidor.
            text = stringResource(R.string.meal_photo_analyzing_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

// ----- PT7 · Estimación propuesta -----

@Composable
private fun ProposalBody(state: MealPhotoUiState, analysis: MealPhotoAnalysis, actions: MealPhotoActions) {
    val dimens = HealthifyTheme.dimens
    val scheme = MaterialTheme.colorScheme
    HealthifyCard(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.extraLarge) {
        Column(
            modifier = Modifier
                .padding(dimens.space16)
                .fillMaxWidth()
                .semantics(mergeDescendants = true) {},
            verticalArrangement = Arrangement.spacedBy(dimens.space16),
        ) {
            Icon(HealthifyIcons.Bowl, contentDescription = null, tint = scheme.primary, modifier = Modifier.size(DishIconSize))
            Text(
                text = stringResource(
                    R.string.format_separator_dot,
                    analysis.food.name,
                    gramsText(analysis.estimatedGrams, approximate = true),
                ),
                style = MaterialTheme.typography.titleMedium,
                color = scheme.onSurface,
            )
            ConfidenceRing(confidence = analysis.confidence)
            Text(
                // DECISIÓN PT7 (IN-7): «estimado a partir de tu foto» (no «en tu teléfono»: lo estima el servidor).
                text = stringResource(R.string.meal_photo_confidence_caption),
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant,
            )
        }
    }
    if (analysis.alternatives.isNotEmpty()) {
        AlternativesCard(analysis = analysis, selected = state.selectedAlternative, onSelect = actions.onAlternativeSelected)
    }
    PlanAdherenceCard(answer = state.form.inPlan, onAnswer = actions.onPlanAnswer, showError = state.form.showPlanError)
    Column(verticalArrangement = Arrangement.spacedBy(dimens.space12)) {
        HealthifyButton(
            text = stringResource(
                if (state.selectedAlternative == null) R.string.meal_photo_confirm else R.string.meal_photo_confirm_alternative,
            ),
            onClick = actions.onConfirmProposal,
            loading = state.isSaving,
            modifier = Modifier.fillMaxWidth(),
        )
        HealthifyButton(
            text = stringResource(R.string.meal_photo_adjust),
            onClick = actions.onAdjust,
            style = HealthifyButtonStyle.Text,
            enabled = !state.isSaving,
            modifier = Modifier.fillMaxWidth(),
        )
    }
    Text(
        text = stringResource(R.string.meal_photo_leave_note),
        style = MaterialTheme.typography.bodySmall,
        color = scheme.onSurfaceVariant,
    )
}

/** Anillo de confianza (72 dp) con el porcentaje en Metric/Medium. Confianza siempre visible. */
@Composable
private fun ConfidenceRing(confidence: Double) {
    val scheme = MaterialTheme.colorScheme
    val percent = confidencePercent(confidence)
    val description = stringResource(R.string.meal_photo_cd_confidence, percent)
    Box(
        modifier = Modifier
            .size(ConfidenceRingSize)
            .clearAndSetSemantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        val track = scheme.surfaceContainer
        val progress = scheme.primary
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = ConfidenceRingWidth.toPx()
            val inset = stroke / 2
            val arcSize = Size(size.width - stroke, size.height - stroke)
            drawArc(track, 0f, FULL_SWEEP, false, Offset(inset, inset), arcSize, style = Stroke(stroke))
            drawArc(progress, START_ANGLE, FULL_SWEEP * percent / PERCENT, false, Offset(inset, inset), arcSize, style = Stroke(stroke))
        }
        Text(
            text = stringResource(R.string.meal_photo_confidence_value, percent),
            style = HealthifyTheme.extendedTypography.metricMedium,
            color = scheme.onSurface,
        )
    }
}

/** «¿Es otro plato?»: las alternativas de la IA; las que no están en el catálogo se ven pero no se eligen. */
@Composable
private fun AlternativesCard(analysis: MealPhotoAnalysis, selected: Int?, onSelect: (Int?) -> Unit) {
    val dimens = HealthifyTheme.dimens
    val scheme = MaterialTheme.colorScheme
    HealthifyCard(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.extraLarge) {
        Column(modifier = Modifier.padding(dimens.space16), verticalArrangement = Arrangement.spacedBy(dimens.space8)) {
            Text(
                text = stringResource(R.string.meal_photo_alternatives_title),
                style = MaterialTheme.typography.titleSmall,
                color = scheme.onSurface,
            )
            val selectable = analysis.selectableAlternatives
            analysis.alternatives.forEach { alternative ->
                val index = selectable.indexOf(alternative).takeIf { it >= 0 }
                AlternativeRow(
                    alternative = alternative,
                    isSelected = index != null && index == selected,
                    onClick = index?.let { { onSelect(if (it == selected) null else it) } },
                )
            }
        }
    }
}

@Composable
private fun AlternativeRow(alternative: MealPhotoAlternative, isSelected: Boolean, onClick: (() -> Unit)?) {
    val dimens = HealthifyTheme.dimens
    val scheme = MaterialTheme.colorScheme
    val shape = MaterialTheme.shapes.medium
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (isSelected) scheme.primaryContainer else scheme.surfaceContainer)
            .then(if (isSelected) Modifier.border(dimens.borderThick, scheme.primary, shape) else Modifier)
            .then(if (onClick != null) Modifier.clickable(role = Role.RadioButton, onClick = onClick) else Modifier)
            .padding(horizontal = dimens.space16, vertical = dimens.space12)
            .semantics(mergeDescendants = true) { this.selected = isSelected },
        horizontalArrangement = Arrangement.spacedBy(dimens.space8),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.format_separator_dot, alternative.name, gramsText(alternative.grams, approximate = true)),
                style = MaterialTheme.typography.bodyMedium,
                color = if (isSelected) scheme.primary else scheme.onSurface,
            )
            if (onClick == null) {
                Text(
                    text = stringResource(R.string.meal_photo_alternative_unavailable),
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                )
            }
        }
        if (isSelected) Icon(HealthifyIcons.Check, contentDescription = null, tint = scheme.primary)
    }
}

// ----- PT7.3 · No pudimos estimar -----

@Composable
private fun NotRecognizedStep(actions: MealPhotoActions, modifier: Modifier) {
    LightScaffold(
        title = stringResource(R.string.meal_photo_proposal_title),
        onBack = actions.onBack,
        modifier = modifier,
        bottomBar = {
            Column(verticalArrangement = Arrangement.spacedBy(HealthifyTheme.dimens.space12)) {
                HealthifyButton(
                    text = stringResource(R.string.meal_photo_permission_manual),
                    onClick = actions.onRegisterManually,
                    modifier = Modifier.fillMaxWidth(),
                )
                HealthifyButton(
                    text = stringResource(R.string.meal_photo_retake_other),
                    onClick = actions.onRetake,
                    style = HealthifyButtonStyle.Text,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
    ) {
        MessageBody(
            title = stringResource(R.string.meal_photo_not_recognized_title),
            text = stringResource(R.string.meal_photo_not_recognized_body),
        )
    }
}

@Composable
private fun AnalysisFailedBody(failure: AnalysisFailure?, actions: MealPhotoActions) {
    val manual = stringResource(R.string.meal_photo_permission_manual)
    when (failure) {
        AnalysisFailure.RATE_LIMITED -> FailureState(
            title = stringResource(R.string.meal_photo_analysis_failed_title),
            text = stringResource(R.string.meal_photo_rate_limited_body),
            actionLabel = manual,
            onAction = actions.onRegisterManually,
            modifier = Modifier.fillMaxWidth(),
        )
        AnalysisFailure.UNUSABLE_PHOTO, AnalysisFailure.EXPIRED -> FailureState(
            title = stringResource(R.string.meal_photo_analysis_failed_title),
            text = stringResource(
                if (failure == AnalysisFailure.EXPIRED) R.string.meal_photo_expired_body else R.string.meal_photo_unusable_body,
            ),
            actionLabel = stringResource(R.string.meal_photo_retake_other),
            onAction = actions.onRetryAnalysis,
            secondaryLabel = manual,
            onSecondary = actions.onRegisterManually,
            modifier = Modifier.fillMaxWidth(),
        )
        AnalysisFailure.GENERIC, null -> FailureState(
            title = stringResource(R.string.meal_photo_analysis_failed_title),
            text = stringResource(R.string.meal_photo_analysis_failed_body),
            actionLabel = stringResource(R.string.meal_photo_retry),
            onAction = actions.onRetryAnalysis,
            secondaryLabel = manual,
            onSecondary = actions.onRegisterManually,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

// ----- Estructura común -----

/** Pantalla clara con barra «atrás», contenido con scroll (16 dp, separación 24) y acciones fijas opcionales. */
@Composable
private fun LightScaffold(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    bottomBar: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val dimens = HealthifyTheme.dimens
    SystemBarsAppearance(darkBackground = false)
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .navigationBarsPadding()
            .imePadding(),
    ) {
        HealthifyTopAppBar(title = title, onBack = onBack)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(start = dimens.space16, top = dimens.space16, end = dimens.space16, bottom = dimens.space40),
            verticalArrangement = Arrangement.spacedBy(dimens.space24),
            content = content,
        )
        if (bottomBar != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(dimens.space16),
            ) { bottomBar() }
        }
    }
}

/** PT7.2 / errores: sin barra superior, centrado. */
@Composable
private fun FailureScaffold(modifier: Modifier, content: @Composable () -> Unit) {
    SystemBarsAppearance(darkBackground = false)
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = HealthifyTheme.dimens.space16, vertical = HealthifyTheme.dimens.space40),
        contentAlignment = Alignment.Center,
    ) { content() }
}

@Composable
private fun MessageBody(title: String, text: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = HealthifyTheme.dimens.space64),
        verticalArrangement = Arrangement.spacedBy(HealthifyTheme.dimens.space4),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.Center)
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

private fun Context.hasCameraPermission(): Boolean =
    ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

private fun Context.openAppSettings() {
    startActivity(
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
    )
}

private const val VIEWFINDER_ALPHA = 0.04f
private const val PREVIEW_ASPECT = 296f / 194f
private const val PERCENT = 100f

// ----- Previews -----

private val PreviewForm = MealFormState(mealTime = OffsetDateTime.parse("2026-09-09T13:10:00-05:00"))
private val PreviewToday = LocalDate.parse("2026-09-09")
private val PreviewAnalysis = MealPhotoAnalysis(
    id = MealPhotoAnalysisId("8f14e45f-ceea-467a-9a3b-2c0b1e5f9d10"),
    food = MealFood(12, "Lomo saltado"),
    estimatedGrams = 320.0,
    confidence = 0.82,
    alternatives = listOf(
        MealPhotoAlternative("Tallarín saltado", 300.0, MealFood(31, "Tallarín saltado")),
        MealPhotoAlternative("Pollo saltado", 300.0, null),
    ),
    expiresAt = Instant.parse("2026-09-10T18:10:00Z"),
)

private fun previewState(step: MealPhotoStep) = MealPhotoUiState(
    today = PreviewToday,
    form = PreviewForm.copy(portionText = "300"),
    step = step,
    cameraPermission = CameraPermissionStatus.GRANTED,
    analysis = PreviewAnalysis,
    adjustFood = PreviewAnalysis.food,
)

@Preview(name = "PT5 · Cámara", widthDp = 360, heightDp = 800)
@Composable
private fun CameraPreview() {
    HealthifyTheme { MealPhotoContent(previewState(MealPhotoStep.CAMERA), MealPhotoActions()) }
}

@Preview(name = "PT5.M · Cámara — permiso denegado", widthDp = 360, heightDp = 800)
@Composable
private fun CameraDeniedPreview() {
    HealthifyTheme {
        MealPhotoContent(previewState(MealPhotoStep.CAMERA).copy(cameraPermission = CameraPermissionStatus.DENIED), MealPhotoActions())
    }
}

@Preview(name = "PT6 · Previsualización", widthDp = 360, heightDp = 800)
@Composable
private fun PreviewStepPreview() {
    HealthifyTheme { MealPhotoContent(previewState(MealPhotoStep.PREVIEW), MealPhotoActions()) }
}

@Preview(name = "PT6.1 · Viendo tu foto…", widthDp = 360, heightDp = 800)
@Composable
private fun AnalyzingPreview() {
    HealthifyTheme { MealPhotoContent(previewState(MealPhotoStep.ANALYZING), MealPhotoActions()) }
}

@Preview(name = "PT6.1 · sin conexión (foto guardada)", widthDp = 360, heightDp = 800)
@Composable
private fun SavedOfflinePreview() {
    HealthifyTheme { MealPhotoContent(previewState(MealPhotoStep.SAVED_OFFLINE), MealPhotoActions()) }
}

@Preview(name = "PT7 · Estimación propuesta", widthDp = 360, heightDp = 1000)
@Composable
private fun ProposalPreview() {
    HealthifyTheme { MealPhotoContent(previewState(MealPhotoStep.PROPOSAL), MealPhotoActions()) }
}

@Preview(name = "PT7.2 · No se pudo registrar", widthDp = 360, heightDp = 800)
@Composable
private fun SaveFailedPreview() {
    HealthifyTheme { MealPhotoContent(previewState(MealPhotoStep.SAVE_FAILED), MealPhotoActions()) }
}

@Preview(name = "PT7.3 · No pudimos estimar", widthDp = 360, heightDp = 800)
@Composable
private fun NotRecognizedPreview() {
    HealthifyTheme { MealPhotoContent(previewState(MealPhotoStep.NOT_RECOGNIZED), MealPhotoActions()) }
}

@Preview(name = "PT8 · Confirmar o ajustar", widthDp = 360, heightDp = 800)
@Composable
private fun AdjustPreview() {
    HealthifyTheme { MealPhotoContent(previewState(MealPhotoStep.ADJUST), MealPhotoActions()) }
}

@Preview(name = "Foto · error de la estimación", widthDp = 360, heightDp = 800)
@Composable
private fun AnalysisFailedPreview() {
    HealthifyTheme {
        MealPhotoContent(previewState(MealPhotoStep.ANALYSIS_FAILED).copy(failure = AnalysisFailure.GENERIC), MealPhotoActions())
    }
}
