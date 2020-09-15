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
     * Register a user through auth persistent, providing a username, password, and employeeId
     */
    fun register(username: String, password: String, employeeId: Int) : RegistrationResult {
        if (password.isEmpty()) {
            val result = RegistrationResult.EMPTY_PASSWORD
            logInfo("User registration failed, $result")
            return result
        }
        if(password.length < 12) {
            val result = RegistrationResult.PASSWORD_TOO_SHORT
            logInfo("User registration failed, $result")
            return result
        }
        if (password.length > 255) {
            val result = RegistrationResult.PASSWORD_TOO_LONG
            logInfo("User registration failed, $result")
            return result
        }
        if(blacklistedPasswords.contains(password)){
            val result = RegistrationResult.BLACKLISTED_PASSWORD
            logInfo("User registration failed, $result")
            return result
        }
        if(ap.isUserRegistered(UserName(username))) {
            val result = RegistrationResult.ALREADY_REGISTERED
            logInfo("User registration failed, $result")
            return result
        }

        //past here we're assuming we've passed all of the registration checks, and we want to add the user to the database
        val salt = Hash.getSalt()
        ap.createUser(UserName(username), Hash.createHash(password + salt), salt, employeeId)
        return RegistrationResult.SUCCESS

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
                return LoginResult(SUCCESS, u)
            }
            return LoginResult(FAILURE, u)
        }
        return LoginResult(NOT_REGISTERED, User(1, user, Hash.createHash(password), "", null))
    }
}