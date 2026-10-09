package pe.edu.upc.healthify.features.monitoring.presentation.components

import androidx.annotation.StringRes
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import pe.edu.upc.healthify.R
import pe.edu.upc.healthify.features.monitoring.domain.valueobject.ComplianceOutcome

/*
 * Textos y color del resultado del día (nota de PT15). Tono de invitación: nunca «te pasaste», y un día sin
 * registro nunca se pinta como falla (ni rojo ni error): es «Todavía no registraste nada hoy».
 */

/** Frase de la fila «Cómo voy hoy» de PT3 (sin punto final, como el frame). */
@get:StringRes
val ComplianceOutcome.homeSummaryRes: Int
    get() = when (this) {
        ComplianceOutcome.MET -> R.string.home_outcome_met
        ComplianceOutcome.EXCEEDED -> R.string.home_outcome_exceeded
        ComplianceOutcome.SHORT -> R.string.home_outcome_short
        ComplianceOutcome.UNLOGGED -> R.string.home_outcome_unlogged
    }

/** Frase de la tarjeta de PT15. */
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
 * Acento de la tarjeta. DECISIÓN PT15: el frame solo dibuja `Met` (`tertiary`); `Exceeded`/`Short` usan `secondary`
 * (el naranja de la marca, no un color de error) y `Unlogged` el neutro `outlineVariant`.
 */
@Composable
@ReadOnlyComposable
fun ComplianceOutcome.accentColor(): Color = when (this) {
    ComplianceOutcome.MET -> MaterialTheme.colorScheme.tertiary
    ComplianceOutcome.EXCEEDED, ComplianceOutcome.SHORT -> MaterialTheme.colorScheme.secondary
    ComplianceOutcome.UNLOGGED -> MaterialTheme.colorScheme.outlineVariant
}
