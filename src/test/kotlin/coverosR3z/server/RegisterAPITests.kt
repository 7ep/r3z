package coverosR3z.server

import coverosR3z.DEFAULT_EMPLOYEE
import coverosR3z.DEFAULT_PASSWORD
import coverosR3z.DEFAULT_USER
import coverosR3z.authentication.FakeAuthenticationUtilities
import coverosR3z.authentication.IAuthenticationUtilities
import coverosR3z.authentication.handlePOSTRegister
import coverosR3z.domainobjects.*
import coverosR3z.misc.toStr
import coverosR3z.timerecording.FakeTimeRecordingUtilities
import coverosR3z.timerecording.ITimeRecordingUtilities
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.lang.IllegalStateException

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
        val data = mapOf("username" to DEFAULT_USER.name.value,
                      "password" to DEFAULT_PASSWORD.value,
                      "employee" to DEFAULT_EMPLOYEE.id.value.toString())
        val responseData = handlePOSTRegister(au, NO_USER, data).fileContents
        assertTrue("The system should indicate success.  File was $responseData",
                toStr(responseData).contains("SUCCESS"))
    }

    /**
     * If already authenticated, redirect to the AUTHHOMEPAGE
     */
    @Test
    fun testShouldHandleInvalidInputs_alreadyAuthenticated() {
        val responseData = handlePOSTRegister(au, DEFAULT_USER, emptyMap())
        assertEquals(redirectTo(NamedPaths.AUTHHOMEPAGE.path), responseData)
    }

    /**
     * If our API code is missing a required value
     */
    @Test
    fun testShouldHandleInvalidInputs_blankName() {
        val data = mapOf("username" to "",
                      "password" to DEFAULT_PASSWORD.value,
                      "employee" to DEFAULT_EMPLOYEE.id.toString())
        val ex = assertThrows(IllegalArgumentException::class.java){ handlePOSTRegister(au, NO_USER, data) }
        assertEquals(usernameCannotBeEmptyMsg, ex.message)
    }

    @Test
    fun testShouldHandleInvalidInputs_blankPassword() {
        val data = mapOf("username" to DEFAULT_USER.name.value,
                      "password" to "",
                      "employee" to DEFAULT_EMPLOYEE.id.toString())
        val ex = assertThrows(IllegalArgumentException::class.java){ handlePOSTRegister(au, NO_USER, data) }
        assertEquals(passwordMustNotBeBlankMsg, ex.message)
    }

    @Test
    fun testShouldHandleInvalidInputs_blankEmployee() {
        val data = mapOf("username" to DEFAULT_USER.name.value,
                      "password" to DEFAULT_PASSWORD.value,
                      "employee" to "")
        val ex = assertThrows(IllegalArgumentException::class.java){ handlePOSTRegister(au, NO_USER, data) }
        assertEquals(employeeIdCannotBeBlank, ex.message)
    }

    @Test
    fun testShouldHandleInvalidInputs_nonNumericEmployee() {
        val employee = "abc"
        val data = mapOf("username" to DEFAULT_USER.name.value,
                      "password" to DEFAULT_PASSWORD.value,
                      "employee" to employee)
        val ex = assertThrows(IllegalStateException::class.java){ handlePOSTRegister(au, NO_USER, data) }
        assertEquals("Must be able to parse $employee as integer", ex.message)
    }

    /**
     * If the employee id is zero
     */
    @Test
    fun testShouldHandleInvalidInputs_ZeroEmployee() {
        val data = mapOf("username" to DEFAULT_USER.name.value,
              "password" to DEFAULT_PASSWORD.value,
              "employee" to "0")
        val ex = assertThrows(IllegalArgumentException::class.java){ handlePOSTRegister(au, NO_USER, data) }
        assertEquals(minEmployeeIdMsg, ex.message)
    }

    /**
     * If the employee id is below zero
     */
    @Test
    fun testShouldHandleInvalidInputs_NegativeEmployee() {
        val data = mapOf("username" to DEFAULT_USER.name.value,
              "password" to DEFAULT_PASSWORD.value,
              "employee" to "-10")
        val ex = assertThrows(IllegalArgumentException::class.java){ handlePOSTRegister(au, NO_USER, data) }
        assertEquals(minEmployeeIdMsg, ex.message)
    }

    /**
     * If our API code is missing a required value
     */
    @Test
    fun testShouldHandleInvalidInputs_missingUsername() {
        val data = mapOf("password" to DEFAULT_PASSWORD.value,
                      "employee" to DEFAULT_EMPLOYEE.id.toString())
        val ex = assertThrows(IllegalStateException::class.java){ handlePOSTRegister(au, NO_USER, data) }
        assertEquals(usernameNotNullMsg, ex.message)
    }

    /**
     * If our API code is missing a required value
     */
    @Test
    fun testShouldHandleInvalidInputs_missingPassword() {
        val data = mapOf("username" to DEFAULT_USER.name.value,
                      "employee" to DEFAULT_EMPLOYEE.id.toString())
        val ex = assertThrows(IllegalStateException::class.java){ handlePOSTRegister(au, NO_USER, data) }
        assertEquals(passwordMustNotBeNullMsg, ex.message)
    }

    /**
     * If our API code is missing a required value
     */
    @Test
    fun testShouldHandleInvalidInputs_missingEmployee() {
        val data = mapOf("username" to DEFAULT_USER.name.value,
                      "password" to DEFAULT_PASSWORD.value)
        val ex = assertThrows(IllegalArgumentException::class.java){ handlePOSTRegister(au, NO_USER, data) }
        assertEquals(employeeIdNotNullMsg, ex.message)
    }


}