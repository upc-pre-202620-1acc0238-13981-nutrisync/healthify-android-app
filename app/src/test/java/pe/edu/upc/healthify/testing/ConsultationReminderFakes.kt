package pe.edu.upc.healthify.testing

import pe.edu.upc.healthify.features.monitoring.domain.entity.ConsultationReminder
import pe.edu.upc.healthify.features.monitoring.domain.repository.ConsultationReminderScheduler

class FakeConsultationReminderScheduler : ConsultationReminderScheduler {
    val replaced = mutableListOf<Pair<Long, List<ConsultationReminder>>>()
    var cancelled = 0
    var refreshRequests = 0

    override fun replaceAll(userId: Long, reminders: List<ConsultationReminder>) {
        replaced += userId to reminders
    }

    override fun cancelAll() {
        cancelled++
    }

    override fun requestRefresh() {
        refreshRequests++
    }
}
