package coverosR3z.authentication

import coverosR3z.domainobjects.Hash
import coverosR3z.domainobjects.UserName

class FakeAuthPersistence(
        var createUserBehavior : () -> Unit = {},
        var isUserRegisteredBehavior : () -> Boolean = {false}
) : IAuthPersistence {

    override fun createUser(name: UserName, hash: Hash, salt: String) {
        createUserBehavior()
    }

    override fun isUserRegistered(name: UserName) : Boolean {
        return isUserRegisteredBehavior()
    }


}