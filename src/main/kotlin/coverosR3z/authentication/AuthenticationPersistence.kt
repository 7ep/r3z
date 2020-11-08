package coverosR3z.authentication

import coverosR3z.domainobjects.Hash
import coverosR3z.domainobjects.UserName
import coverosR3z.domainobjects.User
import coverosR3z.persistence.PureMemoryDatabase

class AuthenticationPersistence(val pmd : PureMemoryDatabase) : IAuthPersistence {

    override fun createUser(name: UserName, hash: Hash, salt: String, employeeId: Int?) {
        pmd.addNewUser(name, hash, salt, employeeId)
    }

    override fun isUserRegistered(name: UserName): Boolean {
        return pmd.getUserByName(name) != null
    }

    override fun getUser(name: UserName) : User? {
        return pmd.getUserByName(name)
    }

    override fun getUserForSession(sessionToken: String): User? {
        return pmd.getUserForSession(sessionToken)
    }

}