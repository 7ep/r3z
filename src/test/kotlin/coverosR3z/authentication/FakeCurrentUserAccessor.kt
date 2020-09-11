package coverosR3z.authentication

import coverosR3z.DEFAULT_USER
import coverosR3z.domainobjects.User

class FakeCurrentUserAccessor(var getCurrentUseBehavior: () -> User = { DEFAULT_USER }) : ICurrentUserAccessor {

    override fun get(): User {
        return getCurrentUseBehavior()
    }

    override fun set(value: User) {
        // does nothing, this holds no state
    }

    override fun clearCurrentUserTestOnly() {
        // does nothing, this holds no state
    }
}