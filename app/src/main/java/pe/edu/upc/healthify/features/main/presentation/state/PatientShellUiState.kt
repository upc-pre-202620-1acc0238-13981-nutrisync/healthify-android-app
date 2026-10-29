package pe.edu.upc.healthify.features.main.presentation.state

/** Patient shell: the main menu header and whether the menu is open. */
data class PatientShellUiState(
    val fullName: String = "",
    val avatarInitial: String = "",
    val isMenuOpen: Boolean = false,
)
