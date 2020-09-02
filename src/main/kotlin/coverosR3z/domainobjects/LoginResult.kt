package coverosR3z.domainobjects

enum class LoginStatuses{
    SUCCESS,
    FAILURE,
    NOT_REGISTERED
}

data class LoginResult(val status: LoginStatuses, val user: User)