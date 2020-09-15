package coverosR3z.authentication

import coverosR3z.domainobjects.Hash
import coverosR3z.domainobjects.User
import coverosR3z.domainobjects.UserName

class FakeAuthPersistence(
        var createUserBehavior : () -> Unit = {},
        var isUserRegisteredBehavior : () -> Boolean = {false},
        var getUserBehavior: () -> User? = {null}
) : IAuthPersistence {

    override fun createUser(name: UserName, hash: Hash, salt: String, employeeId: Int?) {
        createUserBehavior()
    }

    override fun isUserRegistered(name: UserName) : Boolean {
        return isUserRegisteredBehavior()
    }

    override fun getUser(name: UserName): User? {
        return getUserBehavior()
    }

}