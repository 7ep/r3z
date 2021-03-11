package coverosR3z.authentication.utility

import coverosR3z.authentication.types.Role

interface IRolesChecker {
    fun checkAllowed(vararg roles: Role)
}