package coverosR3z.authentication

class FakeAuthPersistence(
        var createUserBehavior : () -> Unit = {},
        var isUserRegisteredBehavior : () -> Boolean = {false}
) : IAuthPersistence {

    override fun createUser(name: String) {
        createUserBehavior()
    }

    override fun isUserRegistered(name: String) : Boolean {
        return isUserRegisteredBehavior()
    }


}