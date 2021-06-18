package coverosR3z.authentication.utility

import coverosR3z.authentication.exceptions.UnpermittedOperationException
import coverosR3z.authentication.types.CurrentUser
import coverosR3z.authentication.types.Role

/**
 * Code for authorization - a regular user can't do what an admin can do, etc.
 */
class RolesChecker {
    companion object : IRolesChecker {
        /**
         * This is handled by the server, search for usages of [UnpermittedOperationException]
         */
        override fun checkAllowed(cu: CurrentUser, vararg roles: Role) {
            if (cu.role !in roles) {
                throw UnpermittedOperationException("User lacked proper role for this action. Roles allowed: ${roles.joinToString(";")}. Your role: ${cu.role}")
            }
        }
    }


}