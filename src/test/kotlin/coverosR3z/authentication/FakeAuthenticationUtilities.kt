package coverosR3z.authentication

import coverosR3z.domainobjects.*

/**
 * Used as a mock object for testing
 */
class FakeAuthenticationUtilities (
        var registerBehavior : () -> RegistrationResult = {RegistrationResult.SUCCESS},
        var loginBehavior : () -> Pair<LoginResult, User> = {Pair(LoginResult.SUCCESS, SYSTEM_USER)}) : IAuthenticationUtilities {

    override fun register(username: String, password: String, employeeId: Int?): RegistrationResult {
       return registerBehavior()
    }

    override fun login(username: String, password: String): Pair<LoginResult, User> {
        return loginBehavior()
    }


}