package coverosR3z.logging

enum class LogTypes {
    /**
     * Generally useful information, particularly for recording business actions by users.  That is, reading these
     * logs should read like a description of the user carrying out business chores
     */
    AUDIT,
    WARN,
    DEBUG,
    TRACE
}