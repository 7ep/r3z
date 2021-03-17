package coverosR3z.authentication.utility

import coverosR3z.authentication.exceptions.UnpermittedOperationException
import coverosR3z.authentication.types.CurrentUser
import coverosR3z.authentication.types.Role
import java.lang.IllegalStateException

class FakeRolesChecker(
    private val cu: CurrentUser,
) : IRolesChecker {

    /**
     * Part of a dead man's switch
     * See https://en.wikipedia.org/wiki/Dead_man%27s_switch
     *
     * If this is true when we get the value [roleCanDoAction],
     * then we will throw an exception.  This alerts us when
     * the code being checked didn't actually even check roles.
     */
    private var diediedie = true

    var roleCanDoAction: Boolean = false
        get() {
            if (diediedie) throw IllegalStateException("role checking never occurred")
            diediedie = true
            return field
        }
        set(value) {
            field = value
            diediedie = false
        }

    override fun checkAllowed(vararg roles: Role) {
        roleCanDoAction = try {
            RolesChecker(cu).checkAllowed(roles = roles)
            true
        } catch (ex: UnpermittedOperationException) {
            false
        }
    }


}