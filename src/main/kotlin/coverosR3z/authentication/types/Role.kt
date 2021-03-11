package coverosR3z.authentication.types

enum class Role {
    REGULAR,
    APPROVER,
    ADMIN,

    /**
     * No human can be the system.  This is a role that only exists
     * for the machine.
     */
    SYSTEM,

    /**
     * This is solely used by the [NO_USER]
     */
    NONE
}
