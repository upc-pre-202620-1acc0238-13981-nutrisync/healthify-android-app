package pe.edu.upc.healthify.features.monitoring.infrastructure.mapper

import kotlinx.serialization.SerializationException
import pe.edu.upc.healthify.core.network.toInstantOrNull
import pe.edu.upc.healthify.core.network.toLocalDateOrNull
import pe.edu.upc.healthify.features.monitoring.domain.entity.DaysRatio
import pe.edu.upc.healthify.features.monitoring.domain.entity.MonitoringSummary
import pe.edu.upc.healthify.features.monitoring.domain.entity.PanelClinicalMeasurement
import pe.edu.upc.healthify.features.monitoring.domain.entity.PanelDay
import pe.edu.upc.healthify.features.monitoring.domain.entity.PanelDiaryEntry
import pe.edu.upc.healthify.features.monitoring.domain.entity.PanelEntryProvenance
import pe.edu.upc.healthify.features.monitoring.domain.entity.PanelProtocolCheck
import pe.edu.upc.healthify.features.monitoring.domain.entity.PanelWeek
import pe.edu.upc.healthify.features.monitoring.domain.entity.PanelWeightTrend
import pe.edu.upc.healthify.features.monitoring.domain.entity.PatientMonitoringPanel
import pe.edu.upc.healthify.features.monitoring.domain.valueobject.ComplianceOutcome
import pe.edu.upc.healthify.features.monitoring.infrastructure.remote.dto.MonitoringSummaryDto
import pe.edu.upc.healthify.features.monitoring.infrastructure.remote.dto.PatientMonitoringPanelDto

/** Cada sección se lee por separado: una ilegible queda vacía y el resto del panel se muestra. */
fun PatientMonitoringPanelDto.toDomain(): PatientMonitoringPanel = PatientMonitoringPanel(
    date = date.toLocalDateOrNull() ?: throw SerializationException("Unreadable panel date"),
    week = week?.let { w ->
        val days = w.days.mapNotNull { day ->
            val d = day.date.toLocalDateOrNull() ?: return@mapNotNull null
            // Un resultado desconocido nunca se pinta como falla: se lee como «Sin registro».
            PanelDay(d, ComplianceOutcome.fromCode(day.outcome) ?: ComplianceOutcome.UNLOGGED)
        }.sortedBy { it.date }
        if (w.metDays in 0..w.totalDays) PanelWeek(days, w.metDays, w.totalDays) else null
    },
    loggedDays = loggedDays?.takeIf { it.logged in 0..it.totalDays }?.let { DaysRatio(it.logged, it.totalDays) },
    weightTrend = weightTrendSummary?.let { PanelWeightTrend(it.weeks, it.slopeKgPerWeek) },
    diary = diary.mapNotNull { entry ->
        val at = entry.localTimestamp.toInstantOrNull() ?: return@mapNotNull null
        val provenance = PanelEntryProvenance.fromCode(entry.provenance) ?: PanelEntryProvenance.MANUAL
        PanelDiaryEntry(
            id = entry.diaryEntryId,
            localTimestamp = at,
            foodName = (entry.confirmedFoodName ?: entry.foodName ?: entry.proposedFoodName)?.takeIf { it.isNotBlank() },
            grams = entry.confirmedPortionGrams ?: entry.proposedPortionGrams,
            provenance = provenance,
            isAwaitingConfirmation = provenance == PanelEntryProvenance.PHOTO && entry.confirmedAt == null &&
                entry.confirmedPortionGrams == null,
        )
    }.sortedBy { it.localTimestamp },
    lastClinicalMeasurement = lastClinicalMeasurement?.let { m ->
        m.takenAt.toInstantOrNull()?.let { at ->
            PanelClinicalMeasurement(at, m.weightKg, m.protocolChecks.mapNotNull(PanelProtocolCheck::fromCode))
        }
    },
)

fun MonitoringSummaryDto.toDomain(): MonitoringSummary = MonitoringSummary(text?.trim()?.takeIf { it.isNotEmpty() })
