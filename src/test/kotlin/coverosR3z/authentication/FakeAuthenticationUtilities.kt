package coverosR3z.authentication

import coverosR3z.DEFAULT_USER
import coverosR3z.authentication.types.*
import coverosR3z.authentication.utility.IAuthenticationUtilities
import coverosR3z.domainobjects.*
import coverosR3z.misc.types.DateTime

/**
 * Used as a mock object for testing
 */
class FakeAuthenticationUtilities (
    var registerBehavior : () -> RegistrationResult = { RegistrationResult(RegistrationResultStatus.SUCCESS, DEFAULT_USER) },
    var loginBehavior : () -> Pair<LoginResult, User> = {Pair(LoginResult.SUCCESS, SYSTEM_USER)},
    var getUserForSessionBehavior: () -> User = { NO_USER },
    var createNewSessionBehavior: () -> String = {""},
    var logoutBehavior: () -> Unit = {},
    ) : IAuthenticationUtilities {

    override fun register(username: UserName, password: Password, employeeId: EmployeeId?): RegistrationResult {
       return registerBehavior()
    }

    override fun login(username: UserName, password: Password): Pair<LoginResult, User> {
        return loginBehavior()
    }

    override fun getUserForSession(sessionToken: String): User {
        return getUserForSessionBehavior()
    }

    override fun createNewSession(user: User, time: DateTime, rand: () -> String): String {
        return createNewSessionBehavior()
    }

    override fun logout(user: User) {
        logoutBehavior()
    }


}