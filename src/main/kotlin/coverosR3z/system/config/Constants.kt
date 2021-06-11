package coverosR3z.system.config

/**
 * The name of this glorious application.
 */
const val APPLICATION_NAME = "r3z"

/**
 * The text at the beginning of the title on HTML pages
 */
const val TITLE_PREFIX = "$APPLICATION_NAME |"

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
const val LENGTH_OF_BYTES_OF_SESSION_STRING = 20

/**
 * The length of the invitation code we will create for when
 * we are inviting a new employee to register a user.
 * See [coverosR3z.authentication.types.Invitation] and
 * [coverosR3z.authentication.types.InvitationCode]
 */
const val LENGTH_OF_BYTES_OF_INVITATION_CODE = 10

/**
 * in the resources, where we store our static files
 */
const val STATIC_FILES_DIRECTORY = "static/"

const val SIZE_OF_DECENT_PASSWORD = 20


