package coverosR3z.authentication

class FakeAuthPersistence(
        var createExecutorBehavior : () -> Unit = {},
        var isUserRegisteredBehavior : () -> Boolean = {false}
) : IAuthPersistence {

    override fun createExecutor(name: String) {
        createExecutorBehavior()
    }

    override fun isUserRegistered(name: String) : Boolean {
        return isUserRegisteredBehavior()
    }


}