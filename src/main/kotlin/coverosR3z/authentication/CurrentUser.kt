package coverosR3z.authentication

import coverosR3z.domainobjects.User

object CurrentUser {

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
