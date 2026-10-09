package pe.edu.upc.healthify.core.designsystem.catalog

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import pe.edu.upc.healthify.R
import pe.edu.upc.healthify.core.designsystem.component.BottomSheetContent
import pe.edu.upc.healthify.core.designsystem.component.BottomSheetStaticFrame
import pe.edu.upc.healthify.core.designsystem.component.EmptyState
import pe.edu.upc.healthify.core.designsystem.component.ErrorState
import pe.edu.upc.healthify.core.designsystem.component.GreetingTopAppBar
import pe.edu.upc.healthify.core.designsystem.component.HealthifyBottomSheet
import pe.edu.upc.healthify.core.designsystem.component.HealthifyButtonImpl
import pe.edu.upc.healthify.core.designsystem.component.HealthifyButtonStyle
import pe.edu.upc.healthify.core.designsystem.component.HealthifyDialog
import pe.edu.upc.healthify.core.designsystem.component.HealthifyDialogContent
import pe.edu.upc.healthify.core.designsystem.component.HealthifyIconButtonImpl
import pe.edu.upc.healthify.core.designsystem.component.HealthifyIconButtonStyle
import pe.edu.upc.healthify.core.designsystem.component.HealthifySnackbarContent
import pe.edu.upc.healthify.core.designsystem.component.HealthifySnackbarHost
import pe.edu.upc.healthify.core.designsystem.component.HealthifyTextField
import pe.edu.upc.healthify.core.designsystem.component.HealthifyTextFieldImpl
import pe.edu.upc.healthify.core.designsystem.component.HealthifyTextFieldType
import pe.edu.upc.healthify.core.designsystem.component.HealthifyTopAppBar
import pe.edu.upc.healthify.core.designsystem.component.ListCardImpl
import pe.edu.upc.healthify.core.designsystem.component.ListCardStyle
import pe.edu.upc.healthify.core.designsystem.component.OfflineBanner
import pe.edu.upc.healthify.core.designsystem.component.OfflineBannerType
import pe.edu.upc.healthify.core.designsystem.component.PatientNavigationBar
import pe.edu.upc.healthify.core.designsystem.component.PatientTab
import pe.edu.upc.healthify.core.designsystem.component.PractitionerNavigationBar
import pe.edu.upc.healthify.core.designsystem.component.PractitionerTab
import pe.edu.upc.healthify.core.designsystem.component.SegmentedOption
import pe.edu.upc.healthify.core.designsystem.component.SegmentedSelector
import pe.edu.upc.healthify.core.designsystem.component.SkeletonCard
import pe.edu.upc.healthify.core.designsystem.component.SkeletonChart
import pe.edu.upc.healthify.core.designsystem.component.SkeletonLine
import pe.edu.upc.healthify.core.designsystem.component.SkeletonListItem
import pe.edu.upc.healthify.core.designsystem.component.StatusChip
import pe.edu.upc.healthify.core.designsystem.component.StatusChipType
import pe.edu.upc.healthify.core.designsystem.component.YesNoSelector
import pe.edu.upc.healthify.core.designsystem.icon.HealthifyIcons
import pe.edu.upc.healthify.core.designsystem.theme.HealthifyTheme
import pe.edu.upc.healthify.core.designsystem.theme.elevation

@Composable
fun DesignSystemCatalogScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var dialog by rememberSaveable { mutableStateOf<Boolean?>(null) }
    var showSheet by rememberSaveable { mutableStateOf(false) }
    val snackbarMessage = stringResource(R.string.catalog_sample_snackbar)
    val snackbarAction = stringResource(R.string.catalog_sample_snackbar_action)

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = { HealthifyTopAppBar(title = stringResource(R.string.catalog_title), onBack = onBack) },
        snackbarHost = { HealthifySnackbarHost(snackbarHostState) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            item(key = "colors") { Section(R.string.catalog_section_colors) { ColorRoles() } }
            item(key = "type") { Section(R.string.catalog_section_typography) { TypeScale() } }
            item(key = "dimens") { Section(R.string.catalog_section_dimens) { DimensSamples() } }
            item(key = "icons") { Section(R.string.catalog_section_icons) { IconGrid() } }
            item(key = "button") { Section(R.string.catalog_section_button) { ButtonMatrix() } }
            item(key = "iconButton") { Section(R.string.catalog_section_icon_button) { IconButtonMatrix() } }
            item(key = "textField") { Section(R.string.catalog_section_text_field) { TextFieldSamples() } }
            item(key = "segmented") { Section(R.string.catalog_section_segmented) { SegmentedSamples() } }
            item(key = "yesNo") { Section(R.string.catalog_section_yes_no) { YesNoSample() } }
            item(key = "card") { Section(R.string.catalog_section_card) { CardMatrix() } }
            item(key = "chip") { Section(R.string.catalog_section_chip) { ChipRow() } }
            item(key = "nav") { Section(R.string.catalog_section_navigation, padded = false) { NavigationSamples() } }
            item(key = "topBar") { Section(R.string.catalog_section_top_app_bar, padded = false) { TopBarSamples() } }
            item(key = "overlays") {
                Section(R.string.catalog_section_overlays) {
                    OverlaySamples(
                        onShowSnackbar = {
                            scope.launch { snackbarHostState.showSnackbar(snackbarMessage, snackbarAction) }
                        },
                        onOpenDialog = { destructive -> dialog = destructive },
                        onOpenSheet = { showSheet = true },
                    )
                }
            }
            item(key = "skeleton") {
                Section(R.string.catalog_section_skeleton) {
                    SkeletonLine()
                    SkeletonCard()
                    SkeletonListItem()
                    SkeletonChart()
                }
            }
            item(key = "banner") {
                Section(R.string.catalog_section_banner) { OfflineBannerType.entries.forEach { OfflineBanner(it) } }
            }
            item(key = "empty") {
                Section(R.string.catalog_section_empty) {
                    EmptyState(
                        title = stringResource(R.string.catalog_sample_empty_title),
                        text = stringResource(R.string.catalog_sample_empty_body),
                        actionLabel = stringResource(R.string.ds_retry),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            item(key = "error") {
                Section(R.string.catalog_section_error) {
                    ErrorState(
                        onRetry = {},
                        secondaryLabel = stringResource(R.string.catalog_sample_close),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(HealthifyTheme.dimens.space48))
                }
            }
        }
    }

    dialog?.let { destructive ->
        HealthifyDialog(
            title = stringResource(R.string.catalog_sample_dialog_title),
            text = stringResource(R.string.catalog_sample_dialog_body),
            confirmLabel = stringResource(R.string.catalog_sample_confirm),
            onConfirm = { dialog = null },
            onDismiss = { dialog = null },
            destructive = destructive,
        )
    }
    if (showSheet) {
        HealthifyBottomSheet(onDismissRequest = { showSheet = false }) {
            BottomSheetContent(
                title = stringResource(R.string.catalog_sample_sheet_title),
                text = stringResource(R.string.catalog_sample_sheet_body),
                primaryLabel = stringResource(R.string.catalog_sample_accept),
                onPrimary = { showSheet = false },
                secondaryLabel = stringResource(R.string.catalog_sample_later),
                onSecondary = { showSheet = false },
            )
        }
    }
}

@Composable
private fun Section(@StringRes title: Int, padded: Boolean = true, content: @Composable ColumnScope.() -> Unit) {
    val dimens = HealthifyTheme.dimens
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = dimens.space16),
        verticalArrangement = Arrangement.spacedBy(dimens.space12),
    ) {
        Text(
            text = stringResource(title),
            style = HealthifyTheme.extendedTypography.overlineSection,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = dimens.screenHorizontal),
        )
        Column(
            modifier = if (padded) Modifier.padding(horizontal = dimens.screenHorizontal) else Modifier,
            verticalArrangement = Arrangement.spacedBy(dimens.space12),
            content = content,
        )
    }
}

@Composable
private fun ColorRoles() {
    val c = MaterialTheme.colorScheme
    val e = HealthifyTheme.extendedColors
    // Nombres de las variables de la colección «Color» del Figma.
    val roles = listOf(
        "primary" to c.primary, "onPrimary" to c.onPrimary, "primaryContainer" to c.primaryContainer,
        "onPrimaryContainer" to c.onPrimaryContainer, "secondary" to c.secondary, "onSecondary" to c.onSecondary,
        "secondaryContainer" to c.secondaryContainer, "onSecondaryContainer" to c.onSecondaryContainer,
        "tertiary" to c.tertiary, "surface" to c.surface, "surfaceContainerLowest" to c.surfaceContainerLowest,
        "surfaceContainer" to c.surfaceContainer, "surfaceContainerHigh" to c.surfaceContainerHigh,
        "onSurface" to c.onSurface, "onSurfaceVariant" to c.onSurfaceVariant, "outline" to c.outline,
        "outlineVariant" to c.outlineVariant, "error" to c.error, "onError" to c.onError,
        "errorContainer" to c.errorContainer, "onErrorContainer" to c.onErrorContainer,
        "inverseSurface" to c.inverseSurface, "inverseOnSurface" to c.inverseOnSurface,
        "inversePrimary" to c.inversePrimary, "scrim" to c.scrim, "brand/decor" to e.brandDecor,
        "brand/decorLight" to e.brandDecorLight, "disabledContainer" to e.disabledContainer,
        "onDisabled" to e.onDisabled, "secondaryPressed" to e.secondaryPressed,
        "primaryContainerPressed" to e.primaryContainerPressed, "primaryOverlayPressed" to e.primaryOverlayPressed,
        "errorOverlayPressed" to e.errorOverlayPressed, "cardPressed" to e.cardPressed,
    )
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(HealthifyTheme.dimens.space8),
        verticalArrangement = Arrangement.spacedBy(HealthifyTheme.dimens.space8),
    ) {
        roles.forEach { (name, color) -> Swatch(name, color) }
    }
}

@Composable
private fun Swatch(name: String, color: Color) {
    val dimens = HealthifyTheme.dimens
    Column(modifier = Modifier.width(SwatchWidth), verticalArrangement = Arrangement.spacedBy(dimens.space4)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(dimens.space40)
                .background(color, MaterialTheme.shapes.small)
                .border(dimens.borderThin, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.small),
        )
        Text(text = name, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private val SwatchWidth = 100.dp

@Composable
private fun TypeScale() {
    val t = MaterialTheme.typography
    val x = HealthifyTheme.extendedTypography
    // Nombres de los 16 estilos de texto del Figma.
    val styles: List<Pair<String, TextStyle>> = listOf(
        "Display/Small" to t.displaySmall, "Headline/Medium" to t.headlineMedium, "Headline/Small" to t.headlineSmall,
        "Title/Large" to t.titleLarge, "Title/Medium" to t.titleMedium, "Title/Small" to t.titleSmall,
        "Body/Large" to t.bodyLarge, "Body/Medium" to t.bodyMedium, "Body/Small" to t.bodySmall,
        "Label/Large" to t.labelLarge, "Label/Medium" to t.labelMedium, "Label/Small" to t.labelSmall,
        "Button/Large" to x.buttonLarge, "Overline/Section" to x.overlineSection, "Metric/Large" to x.metricLarge,
        "Metric/Medium" to x.metricMedium,
    )
    val sample = stringResource(R.string.catalog_sample_typography)
    styles.forEach { (name, style) ->
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = name,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(TypeNameWidth),
            )
            Text(text = sample, style = style, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

private val TypeNameWidth = 120.dp

@Composable
private fun DimensSamples() {
    val d = HealthifyTheme.dimens
    val spaces = listOf(d.space4, d.space8, d.space12, d.space16, d.space24, d.space32, d.space40, d.space48, d.space64)
    Row(horizontalArrangement = Arrangement.spacedBy(d.space8), verticalAlignment = Alignment.Bottom) {
        spaces.forEach { space ->
            Box(
                Modifier
                    .size(space)
                    .background(MaterialTheme.colorScheme.primaryContainer),
            )
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(d.space16)) {
        listOf(d.elevation1, d.elevation2, d.elevation3).forEach { level ->
            Box(
                Modifier
                    .size(d.space64)
                    .elevation(level, MaterialTheme.shapes.large)
                    .background(MaterialTheme.colorScheme.surfaceContainerLowest, MaterialTheme.shapes.large),
            )
        }
    }
}

@Composable
private fun IconGrid() {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(HealthifyTheme.dimens.space8),
        verticalArrangement = Arrangement.spacedBy(HealthifyTheme.dimens.space12),
    ) {
        HealthifyIcons.all.forEach { (name, icon) ->
            Column(modifier = Modifier.width(IconCellWidth), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
                Text(text = name, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

private val IconCellWidth = 72.dp

@Composable
private fun ButtonMatrix() {
    val label = stringResource(R.string.catalog_sample_button)
    val saving = stringResource(R.string.catalog_sample_saving)
    HealthifyButtonStyle.entries.forEach { style ->
        Text(text = style.name, style = MaterialTheme.typography.labelMedium)
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(HealthifyTheme.dimens.space8),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            HealthifyButtonImpl(text = label, onClick = {}, style = style)
            HealthifyButtonImpl(text = label, onClick = {}, style = style, forcePressed = true)
            HealthifyButtonImpl(text = label, onClick = {}, style = style, enabled = false)
            HealthifyButtonImpl(text = saving, onClick = {}, style = style, loading = true)
        }
    }
    HealthifyButtonImpl(text = label, onClick = {}, icon = HealthifyIcons.Add, modifier = Modifier.fillMaxWidth())
}

@Composable
private fun IconButtonMatrix() {
    HealthifyIconButtonStyle.entries.forEach { style ->
        Row(horizontalArrangement = Arrangement.spacedBy(HealthifyTheme.dimens.space16)) {
            HealthifyIconButtonImpl(HealthifyIcons.ArrowBack, null, {}, style = style)
            HealthifyIconButtonImpl(HealthifyIcons.ArrowBack, null, {}, style = style, forcePressed = true)
            HealthifyIconButtonImpl(HealthifyIcons.ArrowBack, null, {}, style = style, enabled = false)
        }
    }
}

@Composable
private fun TextFieldSamples() {
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var weight by rememberSaveable { mutableStateOf("") }
    var notes by rememberSaveable { mutableStateOf("") }
    val label = stringResource(R.string.catalog_sample_label)
    val placeholder = stringResource(R.string.catalog_sample_placeholder)
    val help = stringResource(R.string.catalog_sample_help)
    val full = Modifier.fillMaxWidth()

    // Interactivos: el teclado cambia según el tipo.
    HealthifyTextField(
        value = email,
        onValueChange = { email = it },
        label = stringResource(R.string.catalog_sample_email),
        type = HealthifyTextFieldType.Email,
        imeAction = ImeAction.Next,
        modifier = full,
    )
    HealthifyTextField(
        value = password,
        onValueChange = { password = it },
        label = stringResource(R.string.catalog_sample_password),
        type = HealthifyTextFieldType.Password,
        modifier = full,
    )
    HealthifyTextField(
        value = weight,
        onValueChange = { weight = it },
        label = stringResource(R.string.catalog_sample_weight),
        type = HealthifyTextFieldType.Decimal,
        errorText = if (weight.isNotEmpty() && weight.toDoubleOrNull() == null) {
            stringResource(R.string.catalog_sample_error)
        } else {
            null
        },
        modifier = full,
    )
    HealthifyTextField(
        value = notes,
        onValueChange = { notes = it },
        label = stringResource(R.string.catalog_sample_notes),
        placeholder = placeholder,
        supportingText = help,
        singleLine = false,
        modifier = full,
    )

    // Estados del Figma.
    HealthifyTextFieldImpl("", {}, label, placeholder = placeholder, supportingText = help, modifier = full)
    HealthifyTextFieldImpl(placeholder, {}, label, supportingText = help, forceFocused = true, modifier = full)
    HealthifyTextFieldImpl(placeholder, {}, label, supportingText = help, modifier = full)
    HealthifyTextFieldImpl(placeholder, {}, label, errorText = help, modifier = full)
    HealthifyTextFieldImpl(placeholder, {}, label, supportingText = help, enabled = false, modifier = full)
    HealthifyTextFieldImpl(placeholder, {}, label, errorText = help, singleLine = false, modifier = full)
}

@Composable
private fun SegmentedSamples() {
    val options = listOf(R.string.catalog_sample_option_a, R.string.catalog_sample_option_b)
    var selected by rememberSaveable { mutableStateOf<Int?>(null) }
    SegmentedSelector(
        options = options,
        selected = selected,
        onSelect = { selected = it },
        optionLabel = { stringResource(it) },
    )
    val option = stringResource(R.string.catalog_sample_option_a)
    Row(horizontalArrangement = Arrangement.spacedBy(HealthifyTheme.dimens.space8)) {
        SegmentedOption(option, selected = true, onClick = {}, enabled = false, modifier = Modifier.weight(1f))
        SegmentedOption(option, selected = false, onClick = {}, enabled = false, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun YesNoSample() {
    var answer by rememberSaveable { mutableStateOf<Boolean?>(null) }
    var validate by rememberSaveable { mutableStateOf(false) }
    YesNoSelector(
        question = stringResource(R.string.catalog_sample_question),
        answer = answer,
        onAnswer = { answer = it },
        isError = validate,
    )
    HealthifyButtonImpl(
        text = stringResource(R.string.catalog_validate),
        onClick = { validate = true },
        style = HealthifyButtonStyle.Text,
    )
}

@Composable
private fun CardMatrix() {
    val title = stringResource(R.string.catalog_sample_card_title)
    val support = stringResource(R.string.catalog_sample_card_support)
    ListCardStyle.entries.forEach { style ->
        ListCardImpl(title, Modifier.fillMaxWidth(), support, HealthifyIcons.Folder, style, onClick = {})
        ListCardImpl(title, Modifier.fillMaxWidth(), support, HealthifyIcons.Folder, style, onClick = {}, forcePressed = true)
    }
}

@Composable
private fun ChipRow() {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(HealthifyTheme.dimens.space8),
        verticalArrangement = Arrangement.spacedBy(HealthifyTheme.dimens.space8),
    ) {
        StatusChipType.entries.forEach { StatusChip(it) }
    }
}

@Composable
private fun NavigationSamples() {
    var patientTab by rememberSaveable { mutableStateOf(PatientTab.Home) }
    var practitionerTab by rememberSaveable { mutableStateOf(PractitionerTab.Patients) }
    PatientNavigationBar(selected = patientTab, onSelect = { patientTab = it }, windowInsets = WindowInsets(0))
    PractitionerNavigationBar(
        selected = practitionerTab,
        onSelect = { practitionerTab = it },
        windowInsets = WindowInsets(0),
    )
}

@Composable
private fun TopBarSamples() {
    val title = stringResource(R.string.catalog_sample_screen_title)
    HealthifyTopAppBar(title = title, onBack = {}, windowInsets = WindowInsets(0))
    HealthifyTopAppBar(title = title, windowInsets = WindowInsets(0))
    GreetingTopAppBar(
        greeting = stringResource(R.string.catalog_sample_greeting),
        title = stringResource(R.string.catalog_sample_today),
        avatarInitial = stringResource(R.string.catalog_sample_initial),
        windowInsets = WindowInsets(0),
    )
}

@Composable
private fun OverlaySamples(onShowSnackbar: () -> Unit, onOpenDialog: (Boolean) -> Unit, onOpenSheet: () -> Unit) {
    HealthifyButtonImpl(stringResource(R.string.catalog_show_snackbar), onShowSnackbar, style = HealthifyButtonStyle.Tonal)
    HealthifyButtonImpl(stringResource(R.string.catalog_open_dialog), { onOpenDialog(false) }, style = HealthifyButtonStyle.Tonal)
    HealthifyButtonImpl(
        stringResource(R.string.catalog_open_destructive_dialog),
        { onOpenDialog(true) },
        style = HealthifyButtonStyle.Danger,
    )
    HealthifyButtonImpl(stringResource(R.string.catalog_open_sheet), onOpenSheet, style = HealthifyButtonStyle.Tonal)

    HealthifySnackbarContent(message = stringResource(R.string.catalog_sample_snackbar))
    HealthifySnackbarContent(
        message = stringResource(R.string.catalog_sample_snackbar),
        actionLabel = stringResource(R.string.catalog_sample_snackbar_action),
    )
    HealthifyDialogContent(
        title = stringResource(R.string.catalog_sample_dialog_title),
        text = stringResource(R.string.catalog_sample_dialog_body),
        confirmLabel = stringResource(R.string.catalog_sample_confirm),
        onConfirm = {},
        onDismiss = {},
        destructive = true,
    )
    BottomSheetStaticFrame {
        BottomSheetContent(
            title = stringResource(R.string.catalog_sample_sheet_title),
            text = stringResource(R.string.catalog_sample_sheet_body),
            primaryLabel = stringResource(R.string.catalog_sample_accept),
            onPrimary = {},
            secondaryLabel = stringResource(R.string.catalog_sample_later),
        )
    }
}

@Preview(widthDp = 360, heightDp = 800)
@Composable
private fun DesignSystemCatalogScreenPreview() {
    HealthifyTheme { DesignSystemCatalogScreen(onBack = {}) }
}
