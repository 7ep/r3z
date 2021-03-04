package coverosR3z.authentication.utility

import coverosR3z.authentication.exceptions.UnpermittedOperationException
import coverosR3z.authentication.types.CurrentUser
import coverosR3z.authentication.types.Roles

/**
 * Code for authorization - a regular user can't do what an admin can do, etc.
 */
class RolesChecker(val cu: CurrentUser) : IRolesChecker {

    /**
     * This is handled by the server, search for usages of [UnpermittedOperationException]
     */
    override fun checkAllowed(vararg roles: Roles) {
        if (cu.role !in roles) {
            throw UnpermittedOperationException("User lacked proper role for this action. Roles allowed: ${roles.joinToString(";")}. Your role: ${cu.role}")
        }
    }

}