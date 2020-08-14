package coverosR3z.authentication

import coverosR3z.domainobjects.Hash
import coverosR3z.domainobjects.RegistrationResult
import coverosR3z.domainobjects.UserName


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


        //past here I'm (Mitch) assuming we've passed all of the registration checks, and we want to add the user to the database
        ap.createUser(UserName(username), Hash(password))
        return RegistrationResult.SUCCESS

    }

    fun isUserRegistered(username: String) : Boolean {
        require(username.isNotBlank()){"no username was provided to check"}
        return ap.isUserRegistered(UserName(username))
    }

}