package coverosR3z.domainobjects

enum class RegistrationResultStatus {
    SUCCESS,
    USERNAME_ALREADY_REGISTERED
}

data class RegistrationResult(val status: RegistrationResultStatus, val user : User)