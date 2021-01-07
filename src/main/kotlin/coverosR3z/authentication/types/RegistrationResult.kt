package coverosR3z.domainobjects

import coverosR3z.authentication.types.User

enum class RegistrationResultStatus {
    SUCCESS,
    USERNAME_ALREADY_REGISTERED
}

data class RegistrationResult(val status: RegistrationResultStatus, val user : User)