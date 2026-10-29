package pe.edu.upc.healthify.features.main.presentation.state

/** Practitioner shell: the main menu header and whether the menu is open. */
data class PractitionerShellUiState(
    val fullName: String = "",
    val avatarInitial: String = "",
    val isMenuOpen: Boolean = false,
)
