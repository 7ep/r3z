package coverosR3z.authentication

import coverosR3z.DEFAULT_USER
import coverosR3z.domainobjects.User

class FakeCurrentUserAccessorImpl(var getCurrentUseBehavior: () -> User = { DEFAULT_USER }) : CurrentUserAccessor {

    override fun getCurrentUser(): User {
        return getCurrentUseBehavior()
    }
}