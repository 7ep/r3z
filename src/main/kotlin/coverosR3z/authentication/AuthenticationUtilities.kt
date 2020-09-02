package coverosR3z.authentication

import coverosR3z.domainobjects.Hash
import coverosR3z.domainobjects.RegistrationResult
import coverosR3z.domainobjects.User
import coverosR3z.domainobjects.UserName
import coverosR3z.domainobjects.LoginResult
import coverosR3z.domainobjects.LoginStatuses.*


class AuthenticationUtilities(val ap : IAuthPersistence){

    val blacklistedPasswords : List<String> = listOf<String>("password")

    fun register(username: String, password: String) : RegistrationResult {
        if (password.isEmpty()) {
            return RegistrationResult.EMPTY_PASSWORD
        }
        if(password.length < 12) {
            return RegistrationResult.PASSWORD_TOO_SHORT
        }
        if (password.length > 255) {
            return RegistrationResult.PASSWORD_TOO_LONG
        }
        if(blacklistedPasswords.contains(password)){
            return RegistrationResult.BLACKLISTED_PASSWORD
        }
        if(ap.isUserRegistered(UserName(username))){
            return RegistrationResult.ALREADY_REGISTERED
        }

        //past here we're assuming we've passed all of the registration checks, and we want to add the user to the database
        val salt = Hash.getSalt()
        ap.createUser(UserName(username), Hash.createHash(password + salt), salt)
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
                return LoginResult(SUCCESS, u)
            }
            return LoginResult(FAILURE, u)
        }
        return LoginResult(NOT_REGISTERED, User(1, user, Hash.createHash(password), "a"))
    }
}