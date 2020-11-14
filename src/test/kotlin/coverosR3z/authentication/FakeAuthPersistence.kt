package coverosR3z.authentication

import coverosR3z.domainobjects.*

/**
 * Used as a mock object for testing
 */
class FakeAuthPersistence(
    var createUserBehavior : () -> Unit = {},
    var isUserRegisteredBehavior : () -> Boolean = {false},
    var getUserBehavior: () -> User = { NO_USER },
    var getUserForSessionBehavior: () -> User = { NO_USER },
    var addNewSessionBehavior : () -> Unit = {},
    var deleteSessionBehavior : () -> Unit = {},
) : IAuthPersistence {

    override fun createUser(name: UserName, hash: Hash, salt: String, employeeId: Int?) {
        createUserBehavior()
    }

    override fun isUserRegistered(name: UserName) : Boolean {
        return isUserRegisteredBehavior()
    }

    override fun getUser(name: UserName): User {
        return getUserBehavior()
    }

    override fun getUserForSession(sessionToken: String): User {
        return getUserForSessionBehavior()
    }

    override fun addNewSession(sessionId: String, user: User, time: DateTime) {
        addNewSessionBehavior()
    }

    override fun deleteSession(sessionToken: String) {
        deleteSessionBehavior()
    }

}