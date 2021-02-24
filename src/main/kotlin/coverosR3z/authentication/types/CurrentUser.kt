package coverosR3z.authentication.types

/**
 * The user currently logged in and executing commands
 */
class CurrentUser(user : User) : User(user.id, user.name, user.hash, user.salt, user.employeeId, user.role)

