package pe.edu.upc.healthify.features.main.presentation.state

/** Shell del paciente: el encabezado del menú principal y si el menú está abierto. */
data class PatientShellUiState(
    val fullName: String = "",
    val avatarInitial: String = "",
    val isMenuOpen: Boolean = false,
)
