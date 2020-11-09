package coverosR3z.authentication

import coverosR3z.domainobjects.*

/**
 * Used as a mock object for testing
 */
class FakeAuthenticationUtilities (
        var registerBehavior : () -> RegistrationResult = {RegistrationResult.SUCCESS},
        var loginBehavior : () -> Pair<LoginResult, User> = {Pair(LoginResult.SUCCESS, SYSTEM_USER)},
        var getUserForSessionBehavior: () -> User? = {null},
        var createNewSessionBehavior: () -> String? = {""}
    ) : IAuthenticationUtilities {

    override fun register(username: String, password: String, employeeId: Int?): RegistrationResult {
       return registerBehavior()
    }

    override fun login(username: String, password: String): Pair<LoginResult, User> {
        return loginBehavior()
    }

    override fun getUserForSession(sessionToken: String): User? {
        return getUserForSessionBehavior()
    }

    override fun createNewSession(user: User, time: DateTime, rand: () -> String): String? {
        return createNewSessionBehavior()
    }


}