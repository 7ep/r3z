package coverosR3z.authentication.persistence

import coverosR3z.authentication.types.*
import coverosR3z.misc.types.DateTime
import coverosR3z.timerecording.types.EmployeeId

interface IAuthPersistence {
    fun createUser(name: UserName, hash: Hash, salt: Salt, employeeId: EmployeeId) : User
    fun isUserRegistered(name : UserName) : Boolean
    fun getUser(name: UserName) : User
    fun getUserForSession(sessionToken: String): User
    fun addNewSession(sessionId: String, user: User, time: DateTime)
    fun deleteSession(user: User)
    fun getAllSessions(): Set<Session>
    fun getAllUsers(): Set<User>
}