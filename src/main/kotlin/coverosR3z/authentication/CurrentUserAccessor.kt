package coverosR3z.authentication

import coverosR3z.domainobjects.User
import java.lang.AssertionError

class CurrentUserAccessor() : ICurrentUserAccessor {

    override fun get(): User {
        return CurrentUser.get()
                ?: throw AssertionError("Cannot record time when no user is logged in, you mangy cur")
    }

    override fun set(value : User) {
        CurrentUser.set(value)
    }

    override fun clearCurrentUserTestOnly(){
        CurrentUser.clearCurrentUserTestOnly()
    }

    private object CurrentUser {

        private var user : User? = null

        fun set(value : User) {
            assert(user == null) { "CurrentUser.id is already set to $user." }

            user = value
        }

        fun get() : User? {
            return user
        }

        fun clearCurrentUserTestOnly(){
            user = null
        }
    }
}