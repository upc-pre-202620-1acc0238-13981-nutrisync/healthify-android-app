package pe.edu.upc.healthify.features.iam.domain.valueobject

/** Shell que la app monta para una sesión (read model App Shell, `NavigationShellResource`). */
enum class NavigationShell {
    PATIENT,
    PRACTITIONER,
    ;

    companion object {
        /** El shell que corresponde a un rol (`NavigationShell.ForRole` del backend). */
        fun forRole(role: UserRole): NavigationShell = when (role) {
            UserRole.PATIENT -> PATIENT
            UserRole.PRACTITIONER -> PRACTITIONER
        }
    }
}
