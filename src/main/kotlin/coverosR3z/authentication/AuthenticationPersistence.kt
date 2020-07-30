package coverosR3z.authentication

import coverosR3z.domainobjects.Employee
import coverosR3z.persistence.PureMemoryDatabase

class AuthenticationPersistence(val pmd : PureMemoryDatabase) : IAuthPersistence {

    override fun createUser(name: String) {
        TODO("Not yet implemented")
    }

    override fun isUserRegistered(name: String): Boolean {
        return pmd.getUserByName(name) != null
    }

}