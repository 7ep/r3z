package coverosR3z.authentication

import coverosR3z.domainobjects.DateTime
import coverosR3z.domainobjects.Hash
import coverosR3z.domainobjects.User
import coverosR3z.domainobjects.UserName

/**
 * Used as a mock object for testing
 */
class FakeAuthPersistence(
        var createUserBehavior : () -> Unit = {},
        var isUserRegisteredBehavior : () -> Boolean = {false},
        var getUserBehavior: () -> User? = {null},
        var getUserForSessionBehavior: () -> User? = {null},
        var addNewSessionBehavior : () -> Unit = {},
) : IAuthPersistence {

    override fun createUser(name: UserName, hash: Hash, salt: String, employeeId: Int?) {
        createUserBehavior()
    }

    override fun isUserRegistered(name: UserName) : Boolean {
        return isUserRegisteredBehavior()
    }

    override fun getUser(name: UserName): User? {
        return getUserBehavior()
    }

    override fun getUserForSession(sessionToken: String): User? {
        return getUserForSessionBehavior()
    }

    override fun addNewSession(sessionId: String, user: User, time: DateTime) {
        addNewSessionBehavior()
    }

}