package coverosR3z.authentication.types

enum class RegistrationResultStatus {
    SUCCESS,
    USERNAME_ALREADY_REGISTERED,

    /**
     * This represents the state when the invitation code
     * doesn't find any invitation
     */
    NO_INVITATION_FOUND,
}

data class RegistrationResult(val status: RegistrationResultStatus, val user : User)