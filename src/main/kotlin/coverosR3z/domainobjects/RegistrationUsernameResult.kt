package coverosR3z.domainobjects

enum class RegistrationUsernameResult {
    SUCCESS,
    EMPTY_USERNAME,
    USERNAME_TOO_SHORT,
    USERNAME_TOO_LONG,
    USERNAME_ALREADY_REGISTERED,

    /**
     * If the analysis of the username fails but
     * it's not clear why.  Hint: Check the logs
     * looking for "Analysis of username failed during registration"
     */
    FAILED_UNKNOWN
}