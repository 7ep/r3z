package coverosR3z.config


/** The version of the database.  Update when we have
 * real users and we're changing live prod data.
 */
const val CURRENT_DATABASE_VERSION = 1

/**
 * This is the length of the number of bytes of
 * cryptographically random text that will be
 * used when generating a cookie value.
 * Per the OWASP cheat sheet, this should be more than 16
 * https://cheatsheetseries.owasp.org/cheatsheets/Session_Management_Cheat_Sheet.html
 */
const val LENGTH_OF_BYTES_OF_SESSION_STRING = 40

