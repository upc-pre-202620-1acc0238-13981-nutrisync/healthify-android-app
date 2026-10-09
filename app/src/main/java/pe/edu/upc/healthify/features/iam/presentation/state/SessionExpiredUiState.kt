package pe.edu.upc.healthify.features.iam.presentation.state

/**
 * S6 · Sesión expirada. Los frames Pa y Nu son iguales; cambia el visual con la conexión: oscuro (Normal) o claro
 * con banner y «En cuanto tengas conexión, vuelve a iniciar sesión.» (Sin conexión).
 */
data class SessionExpiredUiState(
    val isOffline: Boolean = false,
)
