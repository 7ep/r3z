package coverosR3z.authentication

import coverosR3z.domainobjects.User

/**
 * The user currently logged in and executing commands
 */
data class CurrentUser(val user : User)

