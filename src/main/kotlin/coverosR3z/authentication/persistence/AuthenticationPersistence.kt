package coverosR3z.authentication.persistence

import coverosR3z.authentication.types.*
import coverosR3z.logging.ILogger
import coverosR3z.misc.types.DateTime
import coverosR3z.persistence.types.DataAccess
import coverosR3z.persistence.utility.PureMemoryDatabase
import coverosR3z.timerecording.types.Employee
import coverosR3z.timerecording.types.NO_EMPLOYEE

class AuthenticationPersistence(pmd : PureMemoryDatabase, private val logger: ILogger) : IAuthPersistence {

    private val userDataAccess: DataAccess<User> = pmd.dataAccess(User.directoryName)
    private val sessionDataAccess: DataAccess<Session> = pmd.dataAccess(Session.directoryName)
    private val invitationDataAccess: DataAccess<Invitation> = pmd.dataAccess(Invitation.directoryName)

    override fun createUser(name: UserName, hash: Hash, salt: Salt, employee: Employee, role: Role) : User {
        return userDataAccess.actOn { users ->
            logger.logTrace { "PMD: adding new user, \"${name.value}\"" }
            val newUser = User(UserId(users.nextIndex.getAndIncrement()), name, hash, salt, employee, role)
            users.add(newUser)
            newUser
        }
    }

    override fun isUserRegistered(name: UserName): Boolean {
        return userDataAccess.read { users -> users.singleOrNull { u -> u.name == name } ?: NO_USER } != NO_USER
    }

    override fun getUser(name: UserName) : User {
        return userDataAccess.read { users ->  users.singleOrNull { u -> u.name == name } ?: NO_USER }
    }

    override fun getUserForSession(sessionToken: String): User {
        return sessionDataAccess.read { sessions -> sessions.singleOrNull { it.sessionId == sessionToken }?.user ?: NO_USER }
    }

    override fun addNewSession(sessionId: String, user: User, time: DateTime) {
        sessionDataAccess.read { sessions -> require(sessions.none { it.sessionId == sessionId }) { "There must not already exist a session for (${user.name}) if we are to create one" } }
        sessionDataAccess.actOn { sessions -> sessions.add(Session(sessions.nextIndex.getAndIncrement(), sessionId, user, time)) }
    }

    override fun deleteSession(user: User) {
        sessionDataAccess.read { sessions ->  check(sessions.any{it.user == user}) {"There must exist a session in the database for (${user.name.value}) in order to delete it"} }
        sessionDataAccess.actOn { sessions -> sessions.filter { it.user == user }.forEach { sessions.remove(it) } }
    }

    override fun getAllSessions(): Set<Session> {
        return sessionDataAccess.read { sessions -> sessions.toSet() }
    }

    override fun getAllUsers(): Set<User> {
        return userDataAccess.read { users -> users.toSet() }
    }

    override fun addRoleToUser(user: User, role: Role): User {
        val changedUser = user.copy(role=role)
        userDataAccess.actOn { users -> users.update(changedUser) }
        return changedUser
    }

    override fun createInvitation(employee: Employee, datetime: DateTime, invitationCode: InvitationCode) : Invitation {
        return invitationDataAccess.actOn { invitations ->
            val newInvitation = Invitation(InvitationId(invitations.nextIndex.getAndIncrement()), invitationCode, employee, datetime)
            invitations.add(newInvitation)
            newInvitation
        }
    }

    override fun getEmployeeFromInvitationCode(invitationCode: InvitationCode): Employee {
        return invitationDataAccess.read { invitations ->
            invitations.singleOrNull { it.code == invitationCode }?.employee ?: NO_EMPLOYEE
        }
    }

    override fun removeInvitation(employee: Employee): Boolean {
        val invitation = invitationDataAccess.read { i -> i.single{ it.employee == employee} }
        return invitationDataAccess.actOn { i -> i.remove(invitation) }
    }

    override fun listAllInvitations() : Set<Invitation> {
        return invitationDataAccess.read { it }
    }

}