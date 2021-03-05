package coverosR3z.authentication

import coverosR3z.misc.DEFAULT_USER
import coverosR3z.authentication.persistence.IAuthPersistence
import coverosR3z.authentication.types.*
import coverosR3z.misc.types.DateTime
import coverosR3z.timerecording.types.EmployeeId

/**
 * Used as a mock object for testing
 */
class FakeAuthPersistence(
    var createUserBehavior : () -> User = { DEFAULT_USER },
    var isUserRegisteredBehavior : () -> Boolean = {false},
    var getUserBehavior: () -> User = { NO_USER },
    var getUserForSessionBehavior: () -> User = { NO_USER },
    var addNewSessionBehavior : () -> Unit = {},
    var deleteSessionBehavior : () -> Unit = {},
    var getAllSessionBehavior : () -> Set<Session> = { setOf() },
    var getAllUsersBehavior : () -> Set<User> = { setOf() },
    var addRoleToUserBehavior: () -> User = { NO_USER },
) : IAuthPersistence {

    override fun createUser(name: UserName, hash: Hash, salt: Salt, employeeId: EmployeeId, role: Roles) : User {
        return createUserBehavior()
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

    override fun deleteSession(user: User) {
        deleteSessionBehavior()
    }

    override fun getAllSessions(): Set<Session> {
        return getAllSessionBehavior()
    }

    override fun getAllUsers(): Set<User> {
        return getAllUsersBehavior()
    }

    override fun addRoleToUser(user: User, role: Roles): User {
        return addRoleToUserBehavior()
    }

}