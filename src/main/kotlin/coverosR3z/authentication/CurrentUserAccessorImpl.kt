package coverosR3z.authentication

import coverosR3z.domainobjects.User
import java.lang.AssertionError

class CurrentUserAccessorImpl() : CurrentUserAccessor {

    override fun getCurrentUser(): User {
        return CurrentUser.get()
                ?: throw AssertionError("Cannot record time when no user is logged in, you mangy cur")
    }
}