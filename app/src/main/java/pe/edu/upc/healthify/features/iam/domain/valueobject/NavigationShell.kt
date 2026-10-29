package pe.edu.upc.healthify.features.iam.domain.valueobject

/** Shell the app mounts for a session (App Shell read model, `NavigationShellResource`). */
enum class NavigationShell {
    PATIENT,
    PRACTITIONER,
    ;

    companion object {
        /** The shell for a role (the backend's `NavigationShell.ForRole`). */
        fun forRole(role: UserRole): NavigationShell = when (role) {
            UserRole.PATIENT -> PATIENT
            UserRole.PRACTITIONER -> PRACTITIONER
        }
    }
}
