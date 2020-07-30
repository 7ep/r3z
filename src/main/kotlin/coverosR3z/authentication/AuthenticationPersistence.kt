package coverosR3z.authentication

import coverosR3z.domainobjects.User
import coverosR3z.persistence.PureMemoryDatabase

class AuthenticationPersistence(val pmd : PureMemoryDatabase) : IAuthPersistence {

    override fun createExecutor(name: String) {
        TODO("Not yet implemented")
    }

    override fun isUserRegistered(name: String): Boolean {
        val users : List<User> = pmd.getAllUsers()

        return users.any { u -> u.name == name }
    }

}