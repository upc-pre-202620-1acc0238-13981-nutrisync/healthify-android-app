package pe.edu.upc.healthify.features.nutritionalcare.presentation.components

import pe.edu.upc.healthify.R
import pe.edu.upc.healthify.core.designsystem.component.DayMonth
import pe.edu.upc.healthify.core.designsystem.component.DecimalNumber
import pe.edu.upc.healthify.core.designsystem.component.UiText
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.ReviewEvidence
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.ReviewItem
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.DeviationDirection
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.SignalType
import kotlin.math.roundToInt

/*
 * Evidencia de un ítem de la bandeja (X-2): se arma desde `evidenceData` con plantillas es/en de strings.xml, nunca
 * desde la frase en inglés del backend (`evidence`), que solo se muestra tal cual si no hay datos estructurados.
 */

/** La evidencia en el idioma del lector; sin `evidenceData` usable, la frase legada tal cual. */
fun ReviewItem.evidenceText(): UiText = evidence?.toUiText(patientFullName) ?: UiText.Raw(legacyEvidence)

/** Los días sin registro nunca cuentan: se aclara bajo la evidencia que los usa como denominador. */
val ReviewItem.mentionsLoggedDays: Boolean
    get() = when (val data = evidence) {
        is ReviewEvidence.SustainedDeviation -> true
        is ReviewEvidence.ScheduledRecheck -> data.hasCounts
        is ReviewEvidence.ConsistencyEscalation, null -> false
    }

fun ReviewEvidence.toUiText(patientName: String?): UiText = when (this) {
    is ReviewEvidence.SustainedDeviation -> deviationText(patientName)
    is ReviewEvidence.ScheduledRecheck -> recheckText()
    is ReviewEvidence.ConsistencyEscalation -> consistencyText()
}

private fun ReviewEvidence.SustainedDeviation.deviationText(patientName: String?): UiText {
    val percent = averagePercentFromTarget.roundToInt().toDouble()
    val direction = UiText.of(
        when (direction) {
            DeviationDirection.BELOW -> R.string.review_direction_below
            DeviationDirection.ABOVE -> R.string.review_direction_above
        },
    )
    val kcalClause = averageEnergyKcalFromTarget
        ?.let { UiText.of(R.string.review_evidence_kcal_clause, it.roundToInt().toDouble()) }
        ?: UiText.Raw("")
    return if (patientName != null) {
        UiText.of(
            R.string.review_evidence_deviation_named,
            patientName,
            percent,
            direction,
            kcalClause,
            deviatedDays,
            loggedDaysConsidered,
        )
    } else {
        UiText.of(R.string.review_evidence_deviation, percent, direction, kcalClause, deviatedDays, loggedDaysConsidered)
    }
}

private fun ReviewEvidence.ScheduledRecheck.recheckText(): UiText {
    val deviated = deviatedDays
    val logged = loggedDaysConsidered
    return if (deviated != null && logged != null) {
        UiText.of(R.string.review_evidence_recheck_counts, DayMonth(adjustedOn), adjustedPlanVersion, deviated, logged)
    } else {
        UiText.of(R.string.review_evidence_recheck, DayMonth(adjustedOn), adjustedPlanVersion)
    }
}

private fun ReviewEvidence.ConsistencyEscalation.consistencyText(): UiText {
    val since = alertSinceOn
    val weeks = weeksInAlert
    val alertClause = when {
        since == null -> UiText.Raw("")
        weeks == null -> UiText.of(R.string.review_alert_clause, DayMonth(since))
        weeks == 1 -> UiText.of(R.string.review_alert_clause_one_week, DayMonth(since))
        else -> UiText.of(R.string.review_alert_clause_weeks, DayMonth(since), weeks)
    }
    val shownClause = shownToPatientOn?.let { UiText.of(R.string.review_shown_clause, DayMonth(it)) } ?: UiText.Raw("")
    return UiText.of(
        R.string.review_evidence_consistency,
        DecimalNumber(kgPerWeek, KG_PER_WEEK_DECIMALS),
        alertClause,
        shownClause,
    )
}

/** «Desviación sostenida» (chip de PR14) o, con [inline], «desviación sostenida» (fila de PR13). */
fun SignalType.labelText(inline: Boolean = false): UiText = when (this) {
    SignalType.SustainedDeviation ->
        UiText.of(if (inline) R.string.signal_sustained_deviation_inline else R.string.signal_sustained_deviation)
    SignalType.ScheduledRecheck ->
        UiText.of(if (inline) R.string.signal_scheduled_recheck_inline else R.string.signal_scheduled_recheck)
    SignalType.ConsistencyEscalation ->
        UiText.of(if (inline) R.string.signal_consistency_escalation_inline else R.string.signal_consistency_escalation)
    is SignalType.Custom -> UiText.Raw(text)
}

private const val KG_PER_WEEK_DECIMALS = 2
