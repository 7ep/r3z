package coverosR3z.authentication

import coverosR3z.domainobjects.*
import coverosR3z.persistence.PureMemoryDatabase

class AuthenticationPersistence(val pmd : PureMemoryDatabase) : IAuthPersistence {

    override fun createUser(name: UserName, hash: Hash, salt: String, employeeId: Int?) {
        pmd.addNewUser(name, hash, salt, employeeId)
    }

    override fun isUserRegistered(name: UserName): Boolean {
        return pmd.getUserByName(name) != NO_USER
    }

    override fun getUser(name: UserName) : User {
        return pmd.getUserByName(name)
    }

    override fun getUserForSession(sessionToken: String): User {
        return pmd.getUserBySessionToken(sessionToken)
    }

    override fun addNewSession(sessionId: String, user: User, time: DateTime) {
        pmd.addNewSession(sessionId, user, time)
    }

}