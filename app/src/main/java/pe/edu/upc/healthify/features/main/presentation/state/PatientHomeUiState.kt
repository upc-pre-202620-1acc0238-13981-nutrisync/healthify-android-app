package pe.edu.upc.healthify.features.main.presentation.state

import pe.edu.upc.healthify.features.monitoring.domain.valueobject.ComplianceOutcome
import java.time.Instant
import java.time.LocalDate

/**
 * Home (+ loading, first time, offline, consistency notice, logging gap, energy target exceeded and targets changed).
 *
 * @param targets current targets; `null` while loading or with no targets ([hasNoTargets]).
 * @param cachedAt when the shown copy was stored; `null` if the targets come from the backend.
 * @param consumedKcal energy logged today (confirmed); `null` if it could not be read yet (offline).
 * @param todayOutcome today's result for the "How I'm doing today" row; `null` = row hidden.
 * @param macrosLeft what is left of each macro today (computed by the backend); `null` without a reading: the
 *   gauge shows the targets in grams with "Your target today".
 * @param week "Your week": which of the last 7 days have a log; `null` without a reading (the card hides).
 * @param nextFollowUp "Next appointment"; `null` = row hidden (none scheduled).
 * @param showConsistencyCard "Something doesn't add up" notice (active consistency alert or unseen notice).
 * @param showLoggingGapCard the "We miss you around here" reminder.
 * @param targetsChangedVersion published version the patient has not reviewed yet.
 */
data class PatientHomeUiState(
    val greetingName: String = "",
    val avatarInitial: String = "",
    val isLoading: Boolean = true,
    val isOffline: Boolean = false,
    val targets: HomeTargets? = null,
    val cachedAt: Instant? = null,
    val hasNoTargets: Boolean = false,
    val consumedKcal: Double? = null,
    val macrosLeft: HomeMacros? = null,
    val week: HomeWeek? = null,
    val todayOutcome: ComplianceOutcome? = null,
    val nextFollowUp: HomeFollowUp? = null,
    val showConsistencyCard: Boolean = false,
    val showLoggingGapCard: Boolean = false,
    val targetsChangedVersion: Int? = null,
) {
    /** Offline the card simply does not show. */
    val consistencyCardVisible: Boolean get() = showConsistencyCard && !isOffline && targets != null

    val loggingGapCardVisible: Boolean get() = showLoggingGapCard && targets != null

    /**
     * "Tomorrow is another day" notice: the day already came out EXCEEDED or the logged energy exceeds the target.
     * Offline or without targets it does not show (the day's progress is only read online, from Monitoring).
     */
    val energyExceededCardVisible: Boolean
        get() {
            val target = targets?.energyKcal ?: return false
            if (isOffline) return false
            val consumed = consumedKcal
            return todayOutcome == ComplianceOutcome.EXCEEDED || (consumed != null && consumed > target)
        }

    /** Offline, "How I'm doing today" and "Next appointment" are not shown (they live in Monitoring). */
    val howAmIDoingVisible: Boolean get() = !isOffline && todayOutcome != null
    val nextFollowUpVisible: Boolean get() = !isOffline && nextFollowUp != null

    val targetsChangedSheetVisible: Boolean get() = targetsChangedVersion != null && !isOffline && targets != null

    /** Offline, "Your week" is not shown (it lives in Monitoring). */
    val weekVisible: Boolean get() = !isOffline && week != null

    private val cachedNoticeVisible: Boolean get() = isOffline || cachedAt != null

    /**
     * No scroll: Home shows one notice at a time, the highest priority one. "Something doesn't add up" first
     * (asks for a review), then the offline copy (explains why numbers are missing), "We miss you" and "Tomorrow is
     * another day". The others show again once the one above goes away.
     */
    val activeNotice: HomeNotice?
        get() = when {
            consistencyCardVisible -> HomeNotice.CONSISTENCY
            cachedNoticeVisible -> HomeNotice.CACHED_TARGETS
            loggingGapCardVisible -> HomeNotice.LOGGING_GAP
            energyExceededCardVisible -> HomeNotice.ENERGY_EXCEEDED
            else -> null
        }

    /** Kcal left today (never below 0); `null` without today's reading. */
    val energyLeftKcal: Double?
        get() {
            val target = targets?.energyKcal ?: return null
            return consumedKcal?.let { (target - it).coerceAtLeast(0.0) }
        }

    /** Target met: the logged energy already reached the energy target (full arc, no red, no reproach). */
    val energyTargetReached: Boolean
        get() {
            val target = targets?.energyKcal ?: return false
            return consumedKcal?.let { it >= target } == true
        }
}

/** Home notices, from highest to lowest priority ([PatientHomeUiState.activeNotice]). */
enum class HomeNotice { CONSISTENCY, CACHED_TARGETS, LOGGING_GAP, ENERGY_EXCEEDED }

/** Grams of each macro (what is left today). */
data class HomeMacros(val proteinG: Double, val carbG: Double, val fatG: Double)

/** "Your week": the 7 days up to today, oldest first. */
data class HomeWeek(val days: List<HomeDay>) {
    val loggedDays: Int get() = days.count { it.isLogged }
}

data class HomeDay(val date: LocalDate, val isLogged: Boolean, val isToday: Boolean)

/** Daily targets shown on Home (the full detail is in My plan). */
data class HomeTargets(
    val planVersion: Int,
    val energyKcal: Double,
    val proteinG: Double,
    val carbG: Double,
    val fatG: Double,
)

data class HomeFollowUp(val id: Long, val scheduledFor: Instant)
