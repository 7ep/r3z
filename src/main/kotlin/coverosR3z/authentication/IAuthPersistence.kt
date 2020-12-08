package coverosR3z.authentication

import coverosR3z.domainobjects.*

interface IAuthPersistence {
    fun createUser(name: UserName, hash: Hash, salt: Salt, employeeId: EmployeeId?)
    fun isUserRegistered(name : UserName) : Boolean
    fun getUser(name: UserName) : User
    fun getUserForSession(sessionToken: String): User
    fun addNewSession(sessionId: String, user: User, time: DateTime)
    fun deleteSession(user: User)
}