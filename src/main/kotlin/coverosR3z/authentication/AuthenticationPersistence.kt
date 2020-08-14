package coverosR3z.authentication

import coverosR3z.domainobjects.Hash
import coverosR3z.domainobjects.UserName
import coverosR3z.persistence.PureMemoryDatabase

class AuthenticationPersistence(val pmd : PureMemoryDatabase) : IAuthPersistence {

    override fun createUser(name: UserName, hash: Hash, salt: String) {
        pmd.addNewUser(name, hash, salt)
    }

    override fun isUserRegistered(name: UserName): Boolean {
        return pmd.getUserByName(name) != null
    }

}