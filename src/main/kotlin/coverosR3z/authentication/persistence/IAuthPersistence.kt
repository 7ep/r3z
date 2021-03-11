package coverosR3z.authentication.persistence

import coverosR3z.authentication.types.*
import coverosR3z.misc.types.DateTime
import coverosR3z.timerecording.types.Employee

interface IAuthPersistence {
    fun createUser(name: UserName, hash: Hash, salt: Salt, employee: Employee, role: Role) : User
    fun isUserRegistered(name : UserName) : Boolean
    fun getUser(name: UserName) : User
    fun getUserForSession(sessionToken: String): User
    fun addNewSession(sessionId: String, user: User, time: DateTime)
    fun deleteSession(user: User)
    fun getAllSessions(): Set<Session>
    fun getAllUsers(): Set<User>
    fun addRoleToUser(user: User, role: Role): User
    fun createInvitation(employee: Employee, datetime: DateTime, invitationCode: InvitationCode) : Invitation
    fun getEmployeeFromInvitationCode(invitationCode: InvitationCode): Employee
    fun removeInvitation(employee: Employee): Boolean
    fun listAllInvitations() : Set<Invitation>
}