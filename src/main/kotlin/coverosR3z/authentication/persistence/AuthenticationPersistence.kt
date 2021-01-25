package coverosR3z.authentication.persistence

import coverosR3z.authentication.types.*
import coverosR3z.logging.logTrace
import coverosR3z.misc.types.DateTime
import coverosR3z.persistence.utility.PureMemoryDatabase
import coverosR3z.timerecording.types.EmployeeId

class AuthenticationPersistence(private val pmd : PureMemoryDatabase) : IAuthPersistence {

    override fun createUser(name: UserName, hash: Hash, salt: Salt, employeeId: EmployeeId?) : User {
        return pmd.UserDataAccess().actOn { users ->
            logTrace { "PMD: adding new user, \"${name.value}\"" }
            val newUser = User(UserId(users.nextIndex.getAndIncrement()), name, hash, salt, employeeId)
            users.add(newUser)
            newUser
        }
    }

    override fun isUserRegistered(name: UserName): Boolean {
        return pmd.UserDataAccess().read { users -> users.singleOrNull { u -> u.name == name } ?: NO_USER } != NO_USER
    }

    override fun getUser(name: UserName) : User {
        return pmd.UserDataAccess().read { users ->  users.singleOrNull { u -> u.name == name } ?: NO_USER }
    }

    override fun getUserForSession(sessionToken: String): User {
        return pmd.SessionDataAccess().read { sessions -> sessions.singleOrNull { it.sessionId == sessionToken }?.user ?: NO_USER }
    }

    override fun addNewSession(sessionId: String, user: User, time: DateTime) {
        pmd.SessionDataAccess().read { sessions -> require(sessions.none { it.sessionId == sessionId }) { "There must not already exist a session for (${user.name}) if we are to create one" } }
        pmd.SessionDataAccess().actOn { sessions -> sessions.add(Session(sessions.nextIndex.getAndIncrement(), sessionId, user, time)) }
    }

    override fun deleteSession(user: User) {
        pmd.SessionDataAccess().read { sessions ->  check(sessions.any{it.user == user}) {"There must exist a session in the database for (${user.name.value}) in order to delete it"} }
        pmd.SessionDataAccess().actOn { sessions -> sessions.filter { it.user == user }.forEach { sessions.remove(it) } }
    }

    override fun getAllSessions(): Set<Session> {
        return pmd.SessionDataAccess().read { sessions -> sessions.toSet() }
    }

    override fun getAllUsers(): Set<User> {
        return pmd.UserDataAccess().read { users -> users.toSet() }
    }

}