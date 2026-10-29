package pe.edu.upc.healthify.features.monitoring.domain.repository

import pe.edu.upc.healthify.features.monitoring.domain.entity.ConsultationReminder

/** Avisos locales de las próximas consultas en este teléfono (no hay push: el teléfono los programa). */
interface ConsultationReminderScheduler {

    /** Reemplaza todos los avisos programados por los de [reminders], del usuario [userId]. */
    fun replaceAll(userId: Long, reminders: List<ConsultationReminder>)

    /** Quita todos los avisos (sin sesión). */
    fun cancelAll()

    /** Pide releer las consultas próximas en segundo plano, con red, y reprogramar los avisos. */
    fun requestRefresh()
}
