package pe.edu.upc.healthify.features.intake.presentation.screen

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pe.edu.upc.healthify.R
import pe.edu.upc.healthify.core.designsystem.component.HealthifyButton
import pe.edu.upc.healthify.core.designsystem.component.HealthifyButtonStyle
import pe.edu.upc.healthify.core.designsystem.component.HealthifyCard
import pe.edu.upc.healthify.core.designsystem.component.HealthifyDialog
import pe.edu.upc.healthify.core.designsystem.component.HealthifySwitchRow
import pe.edu.upc.healthify.core.designsystem.component.HealthifyTopAppBar
import pe.edu.upc.healthify.core.designsystem.component.SkeletonCard
import pe.edu.upc.healthify.core.designsystem.component.SystemBarsAppearance
import pe.edu.upc.healthify.core.designsystem.component.currentLocale
import pe.edu.upc.healthify.core.designsystem.theme.HealthifyTheme
import pe.edu.upc.healthify.features.intake.domain.entity.ReminderKind
import pe.edu.upc.healthify.features.intake.domain.entity.ReminderSettings
import pe.edu.upc.healthify.features.intake.presentation.state.RemindersUiState
import pe.edu.upc.healthify.features.intake.presentation.viewmodel.RemindersViewModel
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

data class RemindersActions(
    val onBack: () -> Unit,
    /** Encender pide antes el permiso (la pantalla); apagar no pide nada. */
    val onToggle: (ReminderKind, Boolean) -> Unit = { _, _ -> },
    val onOpenNotificationSettings: () -> Unit = {},
    val onDismissPermissionDialog: () -> Unit = {},
)

/** PT22 · Recordatorios (+ PT22.M). Se llega desde PT21 «Recordatorios». */
@Composable
fun RemindersScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RemindersViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var pendingKind by rememberSaveable { mutableStateOf<ReminderKind?>(null) }
    var notificationsAllowed by rememberSaveable { mutableStateOf(true) }
    LifecycleResumeEffect(Unit) {
        notificationsAllowed = context.notificationsAllowed()
        onPauseOrDispose {}
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        val kind = pendingKind
        pendingKind = null
        notificationsAllowed = context.notificationsAllowed()
        when {
            kind == null -> Unit
            granted && notificationsAllowed -> viewModel.onToggle(kind, true)
            else -> viewModel.onPermissionDenied()
        }
    }
    RemindersContent(
        state = state,
        notificationsAllowed = notificationsAllowed,
        actions = RemindersActions(
            onBack = onBack,
            onToggle = { kind, enabled ->
                when {
                    !enabled -> viewModel.onToggle(kind, false)
                    context.notificationsAllowed() -> viewModel.onToggle(kind, true)
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !context.hasNotificationPermission() -> {
                        pendingKind = kind
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                    // Antes de Android 13 (o con el permiso dado) los avisos están apagados en los ajustes del sistema.
                    else -> viewModel.onPermissionDenied()
                }
            },
            onOpenNotificationSettings = {
                viewModel.onDismissPermissionDialog()
                context.openNotificationSettings()
            },
            onDismissPermissionDialog = viewModel::onDismissPermissionDialog,
        ),
        modifier = modifier,
    )
}

@Composable
fun RemindersContent(
    state: RemindersUiState,
    notificationsAllowed: Boolean,
    actions: RemindersActions,
    modifier: Modifier = Modifier,
) {
    val dimens = HealthifyTheme.dimens
    val scheme = MaterialTheme.colorScheme
    SystemBarsAppearance(darkBackground = false)
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(scheme.surface)
            .navigationBarsPadding(),
    ) {
        HealthifyTopAppBar(title = stringResource(R.string.reminders_title), onBack = actions.onBack)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = dimens.space16, top = dimens.space16, end = dimens.space16, bottom = dimens.space40),
            verticalArrangement = Arrangement.spacedBy(dimens.space24),
        ) {
            if (state.isLoading) {
                SkeletonCard(modifier = Modifier.fillMaxWidth())
                return@Column
            }
            HealthifyCard(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.extraLarge) {
                Column(modifier = Modifier.padding(dimens.space16), verticalArrangement = Arrangement.spacedBy(dimens.space16)) {
                    ReminderKind.entries.forEach { kind ->
                        Column(verticalArrangement = Arrangement.spacedBy(dimens.space4)) {
                            HealthifySwitchRow(
                                label = stringResource(kind.labelRes),
                                checked = state.settings.isEnabled(kind),
                                onCheckedChange = { actions.onToggle(kind, it) },
                            )
                            Text(
                                text = kind.timesText(),
                                style = MaterialTheme.typography.bodySmall,
                                color = scheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
            val anyEnabled = ReminderKind.entries.any(state.settings::isEnabled)
            if (anyEnabled && !notificationsAllowed) {
                Column(verticalArrangement = Arrangement.spacedBy(dimens.space8)) {
                    Text(
                        text = stringResource(R.string.reminders_notifications_off),
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant,
                    )
                    HealthifyButton(
                        text = stringResource(R.string.reminders_permission_open_settings),
                        onClick = actions.onOpenNotificationSettings,
                        style = HealthifyButtonStyle.Text,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
    if (state.showPermissionDialog) {
        HealthifyDialog(
            title = stringResource(R.string.reminders_permission_title),
            text = stringResource(R.string.reminders_permission_body),
            confirmLabel = stringResource(R.string.reminders_permission_open_settings),
            onConfirm = actions.onOpenNotificationSettings,
            onDismiss = actions.onDismissPermissionDialog,
            dismissLabel = stringResource(R.string.reminders_permission_not_now),
        )
    }
}

private val ReminderKind.labelRes: Int
    get() = when (this) {
        ReminderKind.SELF_WEIGH_IN -> R.string.reminders_weigh_in
        ReminderKind.MEALS -> R.string.reminders_meals
    }

/** «7:30 a. m.» · «1:00 p. m. y 8:00 p. m.» en el formato de hora del idioma. */
@Composable
private fun ReminderKind.timesText(): String {
    val formatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(currentLocale())
    val texts = times.map { time: LocalTime -> formatter.format(time) }
    return when (texts.size) {
        1 -> texts.first()
        else -> stringResource(R.string.reminders_times_pair, texts[0], texts[1])
    }
}

private fun Context.hasNotificationPermission(): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

private fun Context.notificationsAllowed(): Boolean =
    hasNotificationPermission() && NotificationManagerCompat.from(this).areNotificationsEnabled()

/** PT22.M «Abrir ajustes»: los ajustes de notificaciones de la app (o los de la app en versiones viejas). */
private fun Context.openNotificationSettings() {
    val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
    } else {
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null))
    }
    try {
        startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        startActivity(Intent(Settings.ACTION_SETTINGS))
    }
}

@Preview(name = "PT22 · Recordatorios", widthDp = 360, heightDp = 800)
@Composable
private fun RemindersPreview() {
    HealthifyTheme {
        RemindersContent(
            state = RemindersUiState(isLoading = false, settings = ReminderSettings(selfWeighInEnabled = true, mealsEnabled = true)),
            notificationsAllowed = true,
            actions = RemindersActions(onBack = {}),
        )
    }
}

@Preview(name = "PT22.M · Permiso de notificaciones", widthDp = 360, heightDp = 800)
@Composable
private fun RemindersPermissionPreview() {
    HealthifyTheme {
        RemindersContent(
            state = RemindersUiState(isLoading = false, showPermissionDialog = true),
            notificationsAllowed = false,
            actions = RemindersActions(onBack = {}),
        )
    }
}

@Preview(name = "PT22 · Avisos apagados en el teléfono", widthDp = 360, heightDp = 800)
@Composable
private fun RemindersBlockedPreview() {
    HealthifyTheme {
        RemindersContent(
            state = RemindersUiState(isLoading = false, settings = ReminderSettings(selfWeighInEnabled = true)),
            notificationsAllowed = false,
            actions = RemindersActions(onBack = {}),
        )
    }
}

@Preview(name = "PT22 · Cargando", widthDp = 360, heightDp = 800)
@Composable
private fun RemindersLoadingPreview() {
    HealthifyTheme { RemindersContent(state = RemindersUiState(), notificationsAllowed = true, actions = RemindersActions(onBack = {})) }
}
