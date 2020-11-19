package coverosR3z.authentication

import coverosR3z.domainobjects.*
import coverosR3z.logging.logDebug
import coverosR3z.logging.logInfo


class AuthenticationUtilities(private val ap : IAuthPersistence) : IAuthenticationUtilities {

    private val blacklistedPasswords = listOf("password")


    /**
     * Register a user through auth persistent, providing a username, password, and
     * optional employeeId (defaults to null)
     */
    override fun register(username: UserName, password: Password, employeeId: EmployeeId?) : RegistrationResult {
        val passwordResult = analyzePassword(password)
        val usernameResult = analyzeUsername(username)

        val registrationResult =
            if (passwordResult == RegistrationPasswordResult.SUCCESS && usernameResult == RegistrationUsernameResult.SUCCESS) {
                RegistrationResult.SUCCESS
            } else {
                RegistrationResult.FAILURE
            }

        if(registrationResult == RegistrationResult.FAILURE){
            logInfo("User registration failed for $username: passwordResult: $passwordResult usernameResult: $usernameResult")
        }else{
            //Registration success -> add the user to the database
            val salt = Hash.getSalt()
            ap.createUser(username, Hash.createHash(password.addSalt(salt)), salt, employeeId)
            logInfo("User registration successful for $username")
        }

        return registrationResult
    }

    fun analyzeUsername(username: UserName): RegistrationUsernameResult {
        return try {
            when {
                ap.isUserRegistered(username) -> RegistrationUsernameResult.USERNAME_ALREADY_REGISTERED
                else -> RegistrationUsernameResult.SUCCESS
            }
        } catch (ex: Exception) {
            logInfo("Analysis of username failed during registration.  Message: ${ex.message ?: "(NO MESSAGE)"}")
            when (ex.message){
                tooSmallUsernameMsg -> RegistrationUsernameResult.USERNAME_TOO_SHORT
                tooLargeUsernameMsg -> RegistrationUsernameResult.USERNAME_TOO_LONG
                usernameCannotBeEmptyMsg -> RegistrationUsernameResult.EMPTY_USERNAME
               else -> RegistrationUsernameResult.FAILED_UNKNOWN
            }
        }
    }

    /**
     * Examine the password the registrant wishes to use for
     * meeting our quality characteristics
     */
    fun analyzePassword(password: Password): RegistrationPasswordResult {
        return when {
            blacklistedPasswords.contains(password.value) -> RegistrationPasswordResult.BLACKLISTED_PASSWORD
            else -> RegistrationPasswordResult.SUCCESS
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
            logInfo("Login failed: user $user is not registered.")
            return Pair(LoginResult.NOT_REGISTERED, NO_USER)
        }

        val hashedSaltedPassword = Hash.createHash(password.addSalt(user.salt))
        if (user.hash != hashedSaltedPassword) {
            logInfo("Login failed for user $user: Incorrect password.")
            return Pair(LoginResult.FAILURE, NO_USER)
        }

        logInfo("Login successful for user $user.")
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
        logDebug("New session ID ($sessionId) generated for user ($user)")
        ap.addNewSession(sessionId, user, time)
        return sessionId
    }

    override fun logout(sessionToken: String) {
        ap.deleteSession(sessionToken)
    }

}