package coverosR3z.authentication

import coverosR3z.domainobjects.User

interface CurrentUserAccessor {

    fun getCurrentUser(): User
}