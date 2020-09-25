package coverosR3z.authentication

import coverosR3z.domainobjects.Hash
import coverosR3z.domainobjects.RegistrationResult
import coverosR3z.domainobjects.User
import coverosR3z.domainobjects.UserName
import coverosR3z.domainobjects.LoginResult
import coverosR3z.domainobjects.LoginStatuses.*
import coverosR3z.logging.logInfo


class AuthenticationUtilities(val ap : IAuthPersistence, private val cua : ICurrentUserAccessor = CurrentUserAccessor()){

    val blacklistedPasswords : List<String> = listOf<String>("password")

    /**
     * Register a user through auth persistent, providing a username, password, and
     * optional employeeId (defaults to null)
     */
    fun register(username: String, password: String, employeeId: Int? = null) : RegistrationResult {
        val result = when {
            password.isEmpty() -> RegistrationResult.EMPTY_PASSWORD
            password.length < 12 -> RegistrationResult.PASSWORD_TOO_SHORT
            password.length > 255 -> RegistrationResult.PASSWORD_TOO_LONG
            blacklistedPasswords.contains(password) -> RegistrationResult.BLACKLISTED_PASSWORD
            ap.isUserRegistered(UserName(username)) -> RegistrationResult.ALREADY_REGISTERED
            else -> RegistrationResult.SUCCESS
        }

        if(result != RegistrationResult.SUCCESS){
            logInfo("User registration failed for $username: $result")
        }else{
            //Registration success -> add the user to the database
            val salt = Hash.getSalt()
            ap.createUser(UserName(username), Hash.createHash(password + salt), salt, employeeId)
            logInfo("User registration successful for $username")
        }

        return result

    }

    fun isUserRegistered(username: String) : Boolean {
        require(username.isNotBlank()){"no username was provided to check"}
        return ap.isUserRegistered(UserName(username))
    }


    fun login(user: String, password: String): LoginResult {
        val u : User? = ap.getUser(UserName(user))
        if(u != null){
            val hashedSaltedPassword : Hash = Hash.createHash(password + u.salt)
            if(u.hash == hashedSaltedPassword){
                cua.set(u)
                logInfo("Login successful for user $user. Very good work")
                return LoginResult(SUCCESS, u)
            }
            logInfo("Login failed for user $user: Incorrect password. Please stop trying to hack me")
            return LoginResult(FAILURE, u)
        }
        logInfo("Login failed: user $user is not registered. Maybe try registering first?")
        return LoginResult(NOT_REGISTERED, User(1, user, Hash.createHash(password), "", null))
    }
}