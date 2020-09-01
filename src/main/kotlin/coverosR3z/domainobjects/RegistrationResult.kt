package coverosR3z.domainobjects

enum class RegistrationResult {
    SUCCESS,
    EMPTY_PASSWORD,
    PASSWORD_TOO_LONG,
    PASSWORD_TOO_SHORT,
    BLACKLISTED_PASSWORD,
    ALREADY_REGISTERED
}