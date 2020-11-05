package coverosR3z.domainobjects

enum class RegistrationUsernameResult {
    SUCCESS,
    EMPTY_USERNAME,
    USERNAME_TOO_SHORT,
    USERNAME_TOO_LONG,
    USERNAME_ALREADY_REGISTERED
}