package coverosR3z.timerecording

import coverosR3z.authentication.types.CurrentUser
import coverosR3z.authentication.types.NO_USER
import coverosR3z.authentication.utility.FakeAuthenticationUtilities
import coverosR3z.authentication.utility.FakeRolesChecker
import coverosR3z.system.misc.*
import coverosR3z.timerecording.types.DeleteEmployeeResult
import coverosR3z.timerecording.types.NO_EMPLOYEE
import coverosR3z.timerecording.utility.DeleteEmployeeUtility
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.lang.IllegalArgumentException

/**
 * Tests for the utility that handles both authentication
 * and aspects from time recording
 */
class DeleteEmployeeUtilityTests {

    private lateinit var de: DeleteEmployeeUtility
    private lateinit var frc: FakeRolesChecker
    val tru = FakeTimeRecordingUtilities()
    val au = FakeAuthenticationUtilities()


    @Before
    fun init() {
        makeDeleteEmployeeUtility()
    }

    /**
     * Happy path test to delete a newly-created employee
     */
    @Test
    fun testDeleteEmployee() {
        au.getUserByEmployeeBehavior = { NO_USER }
        tru.deleteEmployeeBehavior = { true }

        val result = de.deleteEmployee(DEFAULT_EMPLOYEE)

        assertEquals(DeleteEmployeeResult.SUCCESS, result)
    }

    /**
     * What about when the Employee passed in is
     * specifically [NO_EMPLOYEE]?  That would mean
     * a developer has gotten the wires crossed
     * somewhere and deserves an exception to be thrown.
     */
    @Test
    fun testDeleteEmployee_DisallowNoEmployee() {
        assertThrows(IllegalArgumentException::class.java) { de.deleteEmployee(NO_EMPLOYEE) }
    }

    /**
     * What will happen if we try deleting an employee
     * that isn't actually found in the database?
     */
    @Test
    fun testDeleteEmployee_EmployeeDoesNotExist() {
        au.getUserByEmployeeBehavior = { NO_USER }
        tru.deleteEmployeeBehavior = { false }

        val result = de.deleteEmployee(DEFAULT_EMPLOYEE)

        assertEquals(DeleteEmployeeResult.DID_NOT_DELETE, result)
    }

    /**
     * What will happen if the employee has
     * registered a user?
     */
    @Test
    fun testDeleteEmployee_UserRegistered() {
        au.getUserByEmployeeBehavior = { DEFAULT_USER }

        val result = de.deleteEmployee(DEFAULT_EMPLOYEE)

        assertEquals(DeleteEmployeeResult.TOO_LATE_REGISTERED, result)
    }

    /**
     * Make sure that the roles are handled properly
     */
    @Test
    fun testDeleteEmployee_Roles() {
        au.getUserByEmployeeBehavior = { NO_USER }

        makeDeleteEmployeeUtility(cu = CurrentUser(DEFAULT_ADMIN_USER))
        de.deleteEmployee(DEFAULT_EMPLOYEE)
        assertTrue(frc.roleCanDoAction)

        makeDeleteEmployeeUtility(cu = CurrentUser(DEFAULT_APPROVER))
        de.deleteEmployee(DEFAULT_EMPLOYEE)
        assertFalse(frc.roleCanDoAction)

        makeDeleteEmployeeUtility(cu = CurrentUser(DEFAULT_REGULAR_USER))
        de.deleteEmployee(DEFAULT_EMPLOYEE)
        assertFalse(frc.roleCanDoAction)

    }


    private fun makeDeleteEmployeeUtility(
        cu: CurrentUser = CurrentUser(DEFAULT_ADMIN_USER)
    ) {
        frc = FakeRolesChecker(cu)
        de = DeleteEmployeeUtility(tru, au, cu, testLogger, frc)
    }
}