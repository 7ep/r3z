package coverosR3z.authentication.utility

import coverosR3z.authentication.exceptions.UnpermittedOperationException
import coverosR3z.authentication.types.CurrentUser
import coverosR3z.authentication.types.Roles

class FakeRolesChecker(
    private val cu: CurrentUser,
) : IRolesChecker {

    var didAuthorize: Boolean = false

    override fun checkAllowed(vararg roles: Roles) {
        didAuthorize = try {
            RolesChecker(cu).checkAllowed(roles = roles)
            true
        } catch (ex: UnpermittedOperationException) {
            false
        }
    }


}