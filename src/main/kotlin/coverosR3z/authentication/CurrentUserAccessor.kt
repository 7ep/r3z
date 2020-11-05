package coverosR3z.authentication

import coverosR3z.domainobjects.User

// TODO - keep an eye on this class.  Starting 9/11/2020, if you haven't seen problems with this class running in parallel tests, maybe we were overly unnecessarily concerned.
class CurrentUserAccessor() : ICurrentUserAccessor {

    override fun get(): User? {
        return CurrentUser.get()
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
            require(user == null) { "CurrentUser.id is already set to $user.  " +
                    "Please be aware: if this occurred during a test, it is possible " +
                    "that it is due to parallel tests conflicting with each other.  In that " +
                    "case, now is the time to strongly consider having tests that use this run serially" }

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