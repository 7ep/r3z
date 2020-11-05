package coverosR3z.authentication

import coverosR3z.domainobjects.*
import coverosR3z.logging.logInfo


class AuthenticationUtilities(private val ap : IAuthPersistence, private val cua : ICurrentUserAccessor){

    private val blacklistedPasswords = listOf("password")

    /**
     * Register a user through auth persistent, providing a username, password, and
     * optional employeeId (defaults to null)
     */
    fun register(username: String, password: String, employeeId: Int? = null) : RegistrationResult {
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
            ap.createUser(UserName(username), Hash.createHash(password + salt), salt, employeeId)
            logInfo("User registration successful for $username")
        }

        return registrationResult
    }

    fun analyzeUsername(username: String): RegistrationUsernameResult {
        return when {
            username.isEmpty() -> RegistrationUsernameResult.EMPTY_USERNAME
            username.length < 3 -> RegistrationUsernameResult.USERNAME_TOO_SHORT
            username.length > 50 -> RegistrationUsernameResult.USERNAME_TOO_LONG
            ap.isUserRegistered(UserName(username)) -> RegistrationUsernameResult.USERNAME_ALREADY_REGISTERED
            else -> RegistrationUsernameResult.SUCCESS
        }
    }

    /**
     * Examine the password the registrant wishes to use for
     * meeting our quality characteristics
     */
    fun analyzePassword(password: String): RegistrationPasswordResult {
        return when {
            password.isEmpty() -> RegistrationPasswordResult.EMPTY_PASSWORD
            password.length < 12 -> RegistrationPasswordResult.PASSWORD_TOO_SHORT
            password.length > 255 -> RegistrationPasswordResult.PASSWORD_TOO_LONG
            blacklistedPasswords.contains(password) -> RegistrationPasswordResult.BLACKLISTED_PASSWORD
            else -> RegistrationPasswordResult.SUCCESS
        }
    }

    fun isUserRegistered(username: String) : Boolean {
        require(username.isNotBlank()){"no username was provided to check"}
        return ap.isUserRegistered(UserName(username))
    }


    fun login(username: String, password: String): LoginResult {
        val user = ap.getUser(UserName(username))

        if (user == null) {
            logInfo("Login failed: user $user is not registered.")
            return LoginResult.NOT_REGISTERED
        }

        val hashedSaltedPassword = Hash.createHash(password + user.salt)
        if (user.hash != hashedSaltedPassword) {
            logInfo("Login failed for user $user: Incorrect password.")
            return LoginResult.FAILURE
        }

        cua.set(user)
        logInfo("Login successful for user $user.")
        return LoginResult.SUCCESS
    }

}