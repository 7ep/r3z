package coverosR3z.domainobjects

/**
 * This stores the information about when a user successfully logged
 * into the system.
 * @param sessionId the text identifier given to the user as a cookie, like "abc123",
 *        usually in a form like this: cookie: sessionId=abc123
 * @param user the user who is logged in
 * @param dt the date and time the user successfully logged in
 */
data class Session(val sessionId: String, val user: User, val dt: DateTime)
