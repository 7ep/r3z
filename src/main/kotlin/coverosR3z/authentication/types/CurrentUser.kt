package coverosR3z.authentication.types

import coverosR3z.authentication.types.User

/**
 * The user currently logged in and executing commands
 */
data class CurrentUser(val user : User)

