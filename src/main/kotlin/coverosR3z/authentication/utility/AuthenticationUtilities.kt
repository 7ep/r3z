package coverosR3z.authentication.utility

import coverosR3z.authentication.persistence.IAuthPersistence
import coverosR3z.authentication.types.*
import coverosR3z.logging.logDebug
import coverosR3z.misc.types.DateTime
import coverosR3z.timerecording.types.EmployeeId


class AuthenticationUtilities(private val ap : IAuthPersistence) : IAuthenticationUtilities {

    /**
     * Register a user through auth persistent, providing a username, password, and
     * optional employeeId (defaults to null)
     */
    override fun register(username: UserName, password: Password, employeeId: EmployeeId?) : RegistrationResult {
        return if (! ap.isUserRegistered(username)) {
            //Registration success -> add the user to the database
            val salt = Hash.getSalt()
            val newUser = ap.createUser(username, Hash.createHash(password, salt), salt, employeeId)
            logDebug { "User registration successful for \"${username.value}\"" }
            RegistrationResult(RegistrationResultStatus.SUCCESS, newUser)
        } else {
            logDebug { "User ${username.value} could not be registered: already registered" }
            RegistrationResult(RegistrationResultStatus.USERNAME_ALREADY_REGISTERED, NO_USER)
        }
    }

    fun isUserRegistered(username: UserName) : Boolean {
        return ap.isUserRegistered(username)
    }

    /**
     * Takes a user's username and password and returns a result, and a user
     * as well if the [LoginResult] was successful.
     */
    override fun login(username: UserName, password: Password): Pair<LoginResult, User> {
        val user = ap.getUser(username)

        if (user == NO_USER) {
            logDebug { "Login failed: user ${user.name.value} is not registered." }
            return Pair(LoginResult.NOT_REGISTERED, NO_USER)
        }

        val hashedSaltedPassword = Hash.createHash(password,user.salt)
        if (user.hash != hashedSaltedPassword) {
            logDebug { "Login failed for user \"${user.name.value}\": Incorrect password." }
            return Pair(LoginResult.FAILURE, NO_USER)
        }

        logDebug { "Login successful for user ${user.name.value}." }
        return Pair(LoginResult.SUCCESS, user)
    }

    override fun getUserForSession(sessionToken: String): User {
        return ap.getUserForSession(sessionToken)
    }

    /**
     * Generates a new session entry for a user, which indicates they are
     * authenticated.  In so doing, we set the date and time when this is
     * happening, and create a secure random session value (a string)
     *
     * @param user the user for whom this session is being created
     * @param time the exact time and date this is being created (optional, has default)
     * @param rand the generator for a random string (optional, has default)
     */
    override fun createNewSession(user: User, time : DateTime, rand : () -> String): String {
        val sessionId = rand()
        logDebug { "New session ID ($sessionId) generated for user (${user.name.value})" }
        ap.addNewSession(sessionId, user, time)
        return sessionId
    }

    override fun logout(user: User) {
        ap.deleteSession(user)
    }

}