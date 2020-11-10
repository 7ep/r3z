package coverosR3z.authentication

import coverosR3z.domainobjects.DateTime
import coverosR3z.domainobjects.LoginResult
import coverosR3z.domainobjects.RegistrationResult
import coverosR3z.domainobjects.User
import coverosR3z.misc.generateRandomString
import java.time.LocalDateTime
import java.time.ZoneOffset

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
     * Returns the user if there is a valid session,
     * otherwise returns null
     */
    fun getUserForSession(sessionToken: String): User

    /**
     * Adds a new session to the sessions data structure, with
     * the user and a generated session value
     */
    fun createNewSession(user: User, time : DateTime = DateTime(LocalDateTime.now().toEpochSecond(ZoneOffset.UTC)), rand : () -> String = {generateRandomString(16)}) : String?


}
