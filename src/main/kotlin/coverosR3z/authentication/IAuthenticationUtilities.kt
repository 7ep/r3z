package coverosR3z.authentication

import coverosR3z.domainobjects.LoginResult
import coverosR3z.domainobjects.RegistrationResult
import coverosR3z.domainobjects.User

interface IAuthenticationUtilities {
    /**
     * Register a user through auth persistent, providing a username, password, and
     * optional employeeId (defaults to null)
     */
    fun register(username: String, password: String, employeeId: Int? = null) : RegistrationResult

    /**
     * Takes a user's username and password and returns a result, and a user
     * as well if the [LoginResult] was successful.
     */
    fun login(username: String, password: String): Pair<LoginResult, User>

    /**
     * Whether the token associates to a valid session
     */
    fun getUserForSession(sessionToken: String): User?

}
