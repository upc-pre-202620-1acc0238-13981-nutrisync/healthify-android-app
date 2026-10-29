package pe.edu.upc.healthify.features.monitoring.presentation.components

import androidx.annotation.StringRes
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import pe.edu.upc.healthify.R
import pe.edu.upc.healthify.features.monitoring.domain.valueobject.ComplianceOutcome

/*
 * Texts and color for the day's outcome. Invitation tone: never "you went over", and a day without logs is never
 * drawn as a failure (no red, no error): it is "You haven't logged anything today yet".
 */

/** Sentence for the home "How am I doing today" row (no trailing period, as in the frame). */
@get:StringRes
val ComplianceOutcome.homeSummaryRes: Int
    get() = when (this) {
        ComplianceOutcome.MET -> R.string.home_outcome_met
        ComplianceOutcome.EXCEEDED -> R.string.home_outcome_exceeded
        ComplianceOutcome.SHORT -> R.string.home_outcome_short
        ComplianceOutcome.UNLOGGED -> R.string.home_outcome_unlogged
    }

/** Sentence for the How am I doing today card. */
@get:StringRes
val ComplianceOutcome.messageRes: Int
    get() = when (this) {
        ComplianceOutcome.MET -> R.string.today_outcome_met
        ComplianceOutcome.EXCEEDED -> R.string.today_outcome_exceeded
        ComplianceOutcome.SHORT -> R.string.today_outcome_short
        ComplianceOutcome.UNLOGGED -> R.string.today_outcome_unlogged
    }

@get:StringRes
val ComplianceOutcome.glyphRes: Int
    get() = when (this) {
        ComplianceOutcome.MET -> R.string.today_glyph_met
        ComplianceOutcome.EXCEEDED -> R.string.today_glyph_exceeded
        ComplianceOutcome.SHORT -> R.string.today_glyph_short
        ComplianceOutcome.UNLOGGED -> R.string.today_glyph_unlogged
    }

/**
 * Card accent. The frame only draws `Met` (`tertiary`); `Exceeded`/`Short` use `secondary`
 * (the brand orange, not an error color) and `Unlogged` the neutral `outlineVariant`.
 */
@Composable
@ReadOnlyComposable
fun ComplianceOutcome.accentColor(): Color = when (this) {
    ComplianceOutcome.MET -> MaterialTheme.colorScheme.tertiary
    ComplianceOutcome.EXCEEDED, ComplianceOutcome.SHORT -> MaterialTheme.colorScheme.secondary
    ComplianceOutcome.UNLOGGED -> MaterialTheme.colorScheme.outlineVariant
}
