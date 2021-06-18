package coverosR3z.authentication.utility

import coverosR3z.authentication.types.CurrentUser
import coverosR3z.authentication.types.Role

interface IRolesChecker {

    fun checkAllowed(cu: CurrentUser, vararg roles: Role)
}
