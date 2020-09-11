package coverosR3z.authentication

import coverosR3z.domainobjects.User

interface ICurrentUserAccessor {

    fun get(): User
    fun set(value : User)
    fun clearCurrentUserTestOnly()
}