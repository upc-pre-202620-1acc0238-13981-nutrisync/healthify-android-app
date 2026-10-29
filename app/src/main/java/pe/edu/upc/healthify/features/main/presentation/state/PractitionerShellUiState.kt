package pe.edu.upc.healthify.features.main.presentation.state

/** Shell del nutricionista: el encabezado del menú principal (PR1.D) y si el menú está abierto. */
data class PractitionerShellUiState(
    val fullName: String = "",
    val avatarInitial: String = "",
    val isMenuOpen: Boolean = false,
)
