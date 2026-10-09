package pe.edu.upc.healthify.features.intake.presentation.state

import pe.edu.upc.healthify.features.intake.domain.entity.ReminderSettings

/** PT22 · Recordatorios (+ PT22.M permiso de notificaciones denegado). Local: funciona igual sin conexión. */
data class RemindersUiState(
    val isLoading: Boolean = true,
    val settings: ReminderSettings = ReminderSettings(),
    /** PT22.M «AVISOS DESACTIVADOS»: el permiso se negó al encender un recordatorio. */
    val showPermissionDialog: Boolean = false,
)
