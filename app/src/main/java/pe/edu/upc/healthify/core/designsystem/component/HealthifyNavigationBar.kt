package pe.edu.upc.healthify.core.designsystem.component

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import pe.edu.upc.healthify.R
import pe.edu.upc.healthify.core.designsystem.icon.HealthifyIcons
import pe.edu.upc.healthify.core.designsystem.theme.HealthifyTheme

/** Destinos de la NavigationBar del paciente (Figma «NavigationBar/Paciente»). */
enum class PatientTab(@param:StringRes val labelRes: Int) {
    Home(R.string.nav_home),
    Diary(R.string.nav_diary),
    Progress(R.string.nav_progress),
    Record(R.string.nav_record),
    Settings(R.string.nav_settings),
}

/** Destinos de la NavigationBar del nutricionista (Figma «NavigationBar/Nutricionista»). */
enum class PractitionerTab(@param:StringRes val labelRes: Int) {
    Patients(R.string.nav_patients),
    Inbox(R.string.nav_inbox),
    Agenda(R.string.nav_agenda),
    Settings(R.string.nav_settings),
}

/** Un destino genérico de [HealthifyNavigationBar]. */
@Immutable
data class NavigationBarEntry(val label: String, val icon: ImageVector)

// Medidas de «NavigationBar/Item» del Figma.
private val NavigationBarHeight = 80.dp
private val IndicatorSize = DpSize(64.dp, 32.dp)

/**
 * NavigationBar del Figma (80 dp + inset de navegación por gestos, que dibuja el sistema). El destino
 * activo lleva el indicador `primaryContainer` y la etiqueta en negrita.
 */
@Composable
fun HealthifyNavigationBar(
    items: List<NavigationBarEntry>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    windowInsets: WindowInsets = WindowInsets.navigationBars,
) {
    val dimens = HealthifyTheme.dimens
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            .windowInsetsPadding(windowInsets),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = NavigationBarHeight)
                .padding(horizontal = dimens.space8)
                .selectableGroup(),
        ) {
            items.forEachIndexed { index, item ->
                NavigationBarDestination(
                    item = item,
                    selected = index == selectedIndex,
                    onClick = { onSelect(index) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun NavigationBarDestination(
    item: NavigationBarEntry,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dimens = HealthifyTheme.dimens
    val scheme = MaterialTheme.colorScheme
    val labelStyle = MaterialTheme.typography.labelMedium
    Column(
        modifier = modifier
            .selectable(selected = selected, role = Role.Tab, onClick = onClick)
            .padding(top = dimens.space12, bottom = dimens.space16),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(dimens.space4),
    ) {
        Box(
            modifier = Modifier
                .size(IndicatorSize)
                .clip(CircleShape)
                .then(if (selected) Modifier.background(scheme.primaryContainer) else Modifier),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = null,
                tint = if (selected) scheme.onPrimaryContainer else scheme.onSurfaceVariant,
                modifier = Modifier.size(dimens.icon),
            )
        }
        Text(
            text = item.label,
            style = if (selected) labelStyle.copy(fontWeight = FontWeight.Bold) else labelStyle,
            color = if (selected) scheme.onSurface else scheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** NavigationBar del paciente: Inicio · Diario · Progreso · Expediente · Ajustes. */
@Composable
fun PatientNavigationBar(
    selected: PatientTab,
    onSelect: (PatientTab) -> Unit,
    modifier: Modifier = Modifier,
    windowInsets: WindowInsets = WindowInsets.navigationBars,
) {
    val tabs = PatientTab.entries
    HealthifyNavigationBar(
        items = tabs.map { NavigationBarEntry(stringResource(it.labelRes), it.icon) },
        selectedIndex = tabs.indexOf(selected),
        onSelect = { onSelect(tabs[it]) },
        modifier = modifier,
        windowInsets = windowInsets,
    )
}

/** NavigationBar del nutricionista: Pacientes · Bandeja · Agenda · Ajustes. */
@Composable
fun PractitionerNavigationBar(
    selected: PractitionerTab,
    onSelect: (PractitionerTab) -> Unit,
    modifier: Modifier = Modifier,
    windowInsets: WindowInsets = WindowInsets.navigationBars,
) {
    val tabs = PractitionerTab.entries
    HealthifyNavigationBar(
        items = tabs.map { NavigationBarEntry(stringResource(it.labelRes), it.icon) },
        selectedIndex = tabs.indexOf(selected),
        onSelect = { onSelect(tabs[it]) },
        modifier = modifier,
        windowInsets = windowInsets,
    )
}

private val PatientTab.icon: ImageVector
    get() = when (this) {
        PatientTab.Home -> HealthifyIcons.Home
        PatientTab.Diary -> HealthifyIcons.Diary
        PatientTab.Progress -> HealthifyIcons.Progress
        PatientTab.Record -> HealthifyIcons.Record
        PatientTab.Settings -> HealthifyIcons.Settings
    }

private val PractitionerTab.icon: ImageVector
    get() = when (this) {
        PractitionerTab.Patients -> HealthifyIcons.Patients
        PractitionerTab.Inbox -> HealthifyIcons.Inbox
        PractitionerTab.Agenda -> HealthifyIcons.Agenda
        PractitionerTab.Settings -> HealthifyIcons.Settings
    }

@Preview(name = "NavigationBar/Paciente · Activo", widthDp = 360, heightDp = 560)
@Composable
private fun PatientNavigationBarPreview() {
    ComponentPreview {
        PatientTab.entries.forEach { PatientNavigationBar(selected = it, onSelect = {}, windowInsets = WindowInsets(0)) }
    }
}

@Preview(name = "NavigationBar/Nutricionista · Activo", widthDp = 360, heightDp = 460)
@Composable
private fun PractitionerNavigationBarPreview() {
    ComponentPreview {
        PractitionerTab.entries.forEach {
            PractitionerNavigationBar(selected = it, onSelect = {}, windowInsets = WindowInsets(0))
        }
    }
}
