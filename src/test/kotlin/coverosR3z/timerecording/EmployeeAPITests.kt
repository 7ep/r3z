package coverosR3z.timerecording

import coverosR3z.DEFAULT_EMPLOYEE_NAME
import coverosR3z.authentication.FakeAuthenticationUtilities
import coverosR3z.authentication.utility.IAuthenticationUtilities
import coverosR3z.misc.exceptions.InexactInputsException
import coverosR3z.server.utility.AuthStatus
import coverosR3z.server.utility.doPOSTAuthenticated
import coverosR3z.timerecording.api.EmployeeAPI
import coverosR3z.timerecording.utility.ITimeRecordingUtilities
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test

class EmployeeAPITests {


    lateinit var au : IAuthenticationUtilities
    lateinit var tru : ITimeRecordingUtilities

    @Before
    fun init() {
        au = FakeAuthenticationUtilities()
        tru = FakeTimeRecordingUtilities()
    }

    /**
     * A basic happy path
     */
    @Test
    fun testHandlePOSTNewEmployee() {
        val data = mapOf(EmployeeAPI.Elements.EMPLOYEE_INPUT.elemName to DEFAULT_EMPLOYEE_NAME.value)
        EmployeeAPI.handlePOST(tru, data)
    }

    /**
     * Huge name
     */
    @Test
    fun testHandlePOSTNewEmployee_HugeName() {
        val data = mapOf(EmployeeAPI.Elements.EMPLOYEE_INPUT.elemName to "a".repeat(31))
        val ex = assertThrows(IllegalArgumentException::class.java){ EmployeeAPI.handlePOST(tru, data)}
        assertEquals("Max size of employee name is 30", ex.message)
    }

    /**
     * Big name, but acceptable
     */
    @Test
    fun testHandlePOSTNewEmployee_BigName() {
        val data = mapOf(EmployeeAPI.Elements.EMPLOYEE_INPUT.elemName to "a".repeat(30))
        EmployeeAPI.handlePOST(tru, data)
    }

    /**
     * Missing data
     */
    @Test
    fun testHandlePOSTNewEmployee_noBody() {
        val data = emptyMap<String,String>()
        val ex = assertThrows(InexactInputsException::class.java){ doPOSTAuthenticated(AuthStatus.AUTHENTICATED, EmployeeAPI.requiredInputs, data) { EmployeeAPI.handlePOST(tru, data) } }
        assertEquals("expected keys: [employee_name]. received keys: []", ex.message)
    }
}