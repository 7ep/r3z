package coverosR3z.authentication.utility

import coverosR3z.authentication.types.Roles

interface IRolesChecker {
    fun checkAllowed(vararg roles: Roles)
}