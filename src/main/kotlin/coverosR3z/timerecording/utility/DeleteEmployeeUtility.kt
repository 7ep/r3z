package coverosR3z.timerecording.utility

import coverosR3z.authentication.types.CurrentUser
import coverosR3z.authentication.types.NO_USER
import coverosR3z.authentication.types.Role
import coverosR3z.authentication.utility.IAuthenticationUtilities
import coverosR3z.authentication.utility.IRolesChecker
import coverosR3z.authentication.utility.RolesChecker
import coverosR3z.system.logging.ILogger
import coverosR3z.timerecording.types.DeleteEmployeeResult
import coverosR3z.timerecording.types.Employee
import coverosR3z.timerecording.types.NO_EMPLOYEE

class DeleteEmployeeUtility(
    private val tru: ITimeRecordingUtilities,
    private val au: IAuthenticationUtilities,
    val cu: CurrentUser,
    private val logger: ILogger,
    private val rc: IRolesChecker = RolesChecker(cu)
) {

    fun deleteEmployee(employee: Employee): DeleteEmployeeResult {
        rc.checkAllowed(Role.ADMIN)

        require(employee != NO_EMPLOYEE)

        /*
        check to make sure they haven't already registered (if so,
        we cannot delete this employee)
        */
        if (au.getUserByEmployee(employee) != NO_USER) {
            return DeleteEmployeeResult.TOO_LATE_REGISTERED
        }

        val deleteEmployeeResult = tru.deleteEmployee(employee)

        return if (deleteEmployeeResult) {
            au.removeInvitation(employee)
            DeleteEmployeeResult.SUCCESS
        } else {
            DeleteEmployeeResult.DID_NOT_DELETE
        }
    }

}