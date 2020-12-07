package coverosR3z.server

import coverosR3z.DEFAULT_EMPLOYEE
import coverosR3z.DEFAULT_PASSWORD
import coverosR3z.DEFAULT_USER
import coverosR3z.authentication.FakeAuthenticationUtilities
import coverosR3z.authentication.IAuthenticationUtilities
import coverosR3z.authentication.RegisterAPI
import coverosR3z.domainobjects.employeeIdCannotBeBlank
import coverosR3z.domainobjects.minEmployeeIdMsg
import coverosR3z.domainobjects.passwordMustNotBeBlankMsg
import coverosR3z.domainobjects.usernameCannotBeEmptyMsg
import coverosR3z.exceptions.InexactInputsException
import coverosR3z.misc.toStr
import coverosR3z.timerecording.FakeTimeRecordingUtilities
import coverosR3z.timerecording.ITimeRecordingUtilities
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Tests related to registering a user through the API
 */
class RegisterAPITests {

    lateinit var au : IAuthenticationUtilities
    lateinit var tru : ITimeRecordingUtilities

    @Before
    fun init() {
        au = FakeAuthenticationUtilities()
        tru = FakeTimeRecordingUtilities()
    }

    /**
     * If our code received all the expected
     * values properly, it shouldn't complain
     * Basically a happy path
     */
    @Test
    fun testShouldHandleValidInputs() {
        val data = mapOf(
            RegisterAPI.Elements.USERNAME_INPUT.elemName to DEFAULT_USER.name.value,
            RegisterAPI.Elements.PASSWORD_INPUT.elemName to DEFAULT_PASSWORD.value,
            RegisterAPI.Elements.EMPLOYEE_INPUT.elemName to DEFAULT_EMPLOYEE.id.value.toString())
        val responseData = doPOSTRequireUnauthenticated(AuthStatus.UNAUTHENTICATED, RegisterAPI.requiredInputs, data) { RegisterAPI.handlePOST(au, data) }.fileContents
        assertTrue("The system should indicate success.  File was $responseData",
                toStr(responseData).contains("SUCCESS"))
    }

    /**
     * If our API code is missing a required value
     */
    @Test
    fun testShouldHandleInvalidInputs_blankName() {
        val data = mapOf(RegisterAPI.Elements.USERNAME_INPUT.elemName to "",
            RegisterAPI.Elements.PASSWORD_INPUT.elemName to DEFAULT_PASSWORD.value,
            RegisterAPI.Elements.EMPLOYEE_INPUT.elemName to DEFAULT_EMPLOYEE.id.toString())
        val ex = assertThrows(IllegalArgumentException::class.java){ doPOSTRequireUnauthenticated(AuthStatus.UNAUTHENTICATED, RegisterAPI.requiredInputs, data) { RegisterAPI.handlePOST(au, data) } }
        assertEquals(usernameCannotBeEmptyMsg, ex.message)
    }

    @Test
    fun testShouldHandleInvalidInputs_blankPassword() {
        val data = mapOf(RegisterAPI.Elements.USERNAME_INPUT.elemName to DEFAULT_USER.name.value,
            RegisterAPI.Elements.PASSWORD_INPUT.elemName to "",
            RegisterAPI.Elements.EMPLOYEE_INPUT.elemName to DEFAULT_EMPLOYEE.id.toString())
        val ex = assertThrows(IllegalArgumentException::class.java){ doPOSTRequireUnauthenticated(AuthStatus.UNAUTHENTICATED, RegisterAPI.requiredInputs, data) { RegisterAPI.handlePOST(au, data) } }
        assertEquals(passwordMustNotBeBlankMsg, ex.message)
    }

    @Test
    fun testShouldHandleInvalidInputs_blankEmployee() {
        val data = mapOf(RegisterAPI.Elements.USERNAME_INPUT.elemName to DEFAULT_USER.name.value,
            RegisterAPI.Elements.PASSWORD_INPUT.elemName to DEFAULT_PASSWORD.value,
            RegisterAPI.Elements.EMPLOYEE_INPUT.elemName to "")
        val ex = assertThrows(IllegalArgumentException::class.java){ doPOSTRequireUnauthenticated(AuthStatus.UNAUTHENTICATED, RegisterAPI.requiredInputs, data) { RegisterAPI.handlePOST(au, data) } }
        assertEquals(employeeIdCannotBeBlank, ex.message)
    }

    @Test
    fun testShouldHandleInvalidInputs_nonNumericEmployee() {
        val employee = "abc"
        val data = mapOf(RegisterAPI.Elements.USERNAME_INPUT.elemName to DEFAULT_USER.name.value,
            RegisterAPI.Elements.PASSWORD_INPUT.elemName to DEFAULT_PASSWORD.value,
            RegisterAPI.Elements.EMPLOYEE_INPUT.elemName to employee)
        val ex = assertThrows(java.lang.IllegalArgumentException::class.java){ doPOSTRequireUnauthenticated(AuthStatus.UNAUTHENTICATED, RegisterAPI.requiredInputs, data) { RegisterAPI.handlePOST(au, data) } }
        assertEquals("Must be able to parse $employee as integer", ex.message)
    }

    /**
     * If the employee id is zero
     */
    @Test
    fun testShouldHandleInvalidInputs_ZeroEmployee() {
        val data = mapOf(RegisterAPI.Elements.USERNAME_INPUT.elemName to DEFAULT_USER.name.value,
            RegisterAPI.Elements.PASSWORD_INPUT.elemName to DEFAULT_PASSWORD.value,
            RegisterAPI.Elements.EMPLOYEE_INPUT.elemName to "0")
        val ex = assertThrows(IllegalArgumentException::class.java){ doPOSTRequireUnauthenticated(AuthStatus.UNAUTHENTICATED, RegisterAPI.requiredInputs, data) { RegisterAPI.handlePOST(au, data) } }
        assertEquals(minEmployeeIdMsg, ex.message)
    }

    /**
     * If the employee id is below zero
     */
    @Test
    fun testShouldHandleInvalidInputs_NegativeEmployee() {
        val data = mapOf(RegisterAPI.Elements.USERNAME_INPUT.elemName to DEFAULT_USER.name.value,
            RegisterAPI.Elements.PASSWORD_INPUT.elemName to DEFAULT_PASSWORD.value,
            RegisterAPI.Elements.EMPLOYEE_INPUT.elemName to "-10")
        val ex = assertThrows(IllegalArgumentException::class.java){ doPOSTRequireUnauthenticated(AuthStatus.UNAUTHENTICATED, RegisterAPI.requiredInputs, data) { RegisterAPI.handlePOST(au, data) } }
        assertEquals(minEmployeeIdMsg, ex.message)
    }

    /**
     * If our API code is missing a required value
     */
    @Test
    fun testShouldHandleInvalidInputs_missingUsername() {
        val data = mapOf(RegisterAPI.Elements.PASSWORD_INPUT.elemName to DEFAULT_PASSWORD.value,
            RegisterAPI.Elements.EMPLOYEE_INPUT.elemName to DEFAULT_EMPLOYEE.id.toString())
        val ex = assertThrows(InexactInputsException::class.java){ doPOSTRequireUnauthenticated(AuthStatus.UNAUTHENTICATED, RegisterAPI.requiredInputs, data) { RegisterAPI.handlePOST(au, data) }}
        assertEquals("expected keys: [username, password, employee]. received keys: [password, employee]", ex.message)
    }

    /**
     * If our API code is missing a required value
     */
    @Test
    fun testShouldHandleInvalidInputs_missingPassword() {
        val data = mapOf(RegisterAPI.Elements.USERNAME_INPUT.elemName to DEFAULT_USER.name.value,
            RegisterAPI.Elements.EMPLOYEE_INPUT.elemName to DEFAULT_EMPLOYEE.id.toString())
        val ex = assertThrows(InexactInputsException::class.java){ doPOSTRequireUnauthenticated(AuthStatus.UNAUTHENTICATED, RegisterAPI.requiredInputs, data) { RegisterAPI.handlePOST(au, data) } }
        assertEquals("expected keys: [username, password, employee]. received keys: [username, employee]", ex.message)
    }

    /**
     * If our API code is missing a required value
     */
    @Test
    fun testShouldHandleInvalidInputs_missingEmployee() {
        val data = mapOf(RegisterAPI.Elements.USERNAME_INPUT.elemName to DEFAULT_USER.name.value,
            RegisterAPI.Elements.PASSWORD_INPUT.elemName to DEFAULT_PASSWORD.value)
        val ex = assertThrows(InexactInputsException::class.java){ doPOSTRequireUnauthenticated(AuthStatus.UNAUTHENTICATED, RegisterAPI.requiredInputs, data) { RegisterAPI.handlePOST(au, data) } }
        assertEquals("expected keys: [username, password, employee]. received keys: [username, password]", ex.message)
    }


}