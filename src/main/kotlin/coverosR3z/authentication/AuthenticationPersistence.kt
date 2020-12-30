package coverosR3z.authentication

import coverosR3z.domainobjects.*
import coverosR3z.logging.logTrace
import coverosR3z.persistence.PureMemoryDatabase

class AuthenticationPersistence(private val pmd : PureMemoryDatabase) : IAuthPersistence {

    override fun createUser(name: UserName, hash: Hash, salt: Salt, employeeId: EmployeeId?) {
        pmd.actOnUsers (shouldSerialize = true) { users ->
            logTrace("PMD: adding new user, \"${name.value}\"")
            val newUser = User(UserId(users.nextIndex.getAndIncrement()), name, hash, salt, employeeId)
            users.add(newUser)
            newUser
        }
    }

    override fun isUserRegistered(name: UserName): Boolean {
        return pmd.actOnUsers { users -> users.singleOrNull { u -> u.name == name } ?: NO_USER } != NO_USER
    }

    override fun getUser(name: UserName) : User {
        return pmd.actOnUsers { users ->  users.singleOrNull { u -> u.name == name } ?: NO_USER }
    }

    override fun getUserForSession(sessionToken: String): User {
        return pmd.actOnSessions { sessions -> sessions.singleOrNull { it.sessionId == sessionToken }?.user ?: NO_USER }
    }

    override fun addNewSession(sessionId: String, user: User, time: DateTime) {
        pmd.actOnSessions (shouldSerialize = true) { sessions -> require(sessions.none { it.sessionId == sessionId }) { "There must not already exist a session for (${user.name}) if we are to create one" } }
        pmd.actOnSessions (shouldSerialize = true) { sessions -> sessions.add(Session(sessionId, user, time)) }
    }

    override fun deleteSession(user: User) {
        pmd.actOnSessions (shouldSerialize = true) { sessions ->  check(sessions.any{it.user == user}) {"There must exist a session in the database for (${user.name.value}) in order to delete it"} }
        pmd.actOnSessions (shouldSerialize = true) { sessions -> sessions.filter { it.user == user }.forEach { sessions.remove(it) } }
    }

    override fun getAllSessions(): Set<Session> {
        return pmd.actOnSessions { sessions -> sessions.toSet() }
    }

}