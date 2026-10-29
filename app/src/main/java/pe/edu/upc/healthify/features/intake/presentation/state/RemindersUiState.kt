package pe.edu.upc.healthify.features.intake.presentation.state

import pe.edu.upc.healthify.features.intake.domain.entity.ReminderSettings

/** Reminders (and the denied notification permission dialog). Local: works the same offline. */
data class RemindersUiState(
    val isLoading: Boolean = true,
    val settings: ReminderSettings = ReminderSettings(),
    /** The "NOTIFICATIONS OFF" dialog: the permission was denied when turning a reminder on. */
    val showPermissionDialog: Boolean = false,
)
