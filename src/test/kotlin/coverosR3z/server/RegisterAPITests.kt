package coverosR3z.server

import coverosR3z.DEFAULT_EMPLOYEE
import coverosR3z.DEFAULT_PASSWORD
import coverosR3z.DEFAULT_USER
import coverosR3z.authentication.FakeAuthenticationUtilities
import coverosR3z.authentication.IAuthenticationUtilities
import coverosR3z.authentication.handlePOSTRegister
import coverosR3z.domainobjects.NO_USER
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
     */
    @Test
    fun testShouldHandleValidInputs() {
        val data = mapOf("username" to DEFAULT_USER.name,
                      "password" to DEFAULT_PASSWORD,
                      "employee" to DEFAULT_EMPLOYEE.id.toString())
        val responseData = handlePOSTRegister(au, NO_USER, data).fileContents
        assertTrue("The system should indicate success.  File was $responseData",
                responseData.contains("SUCCESS"))
    }

    /**
     * If already authenticated, redirect to the AUTHHOMEPAGE
     */
    @Test
    fun testShouldHandleInvalidInputs_alreadyAuthenticated() {
        val responseData = handlePOSTRegister(au, DEFAULT_USER, emptyMap())
        assertEquals(ServerUtilities.redirectTo(NamedPaths.AUTHHOMEPAGE.path), responseData)
    }

    /**
     * If our API code is missing a required value
     */
    @Test
    fun testShouldHandleInvalidInputs_blankName() {
        val data = mapOf("username" to "",
                      "password" to DEFAULT_PASSWORD,
                      "employee" to DEFAULT_EMPLOYEE.id.toString())
        val ex = assertThrows(IllegalStateException::class.java){ handlePOSTRegister(au, NO_USER, data) }
        assertEquals("The username must not be blank", ex.message)
    }

    @Test
    fun testShouldHandleInvalidInputs_blankPassword() {
        val data = mapOf("username" to DEFAULT_USER.name,
                      "password" to "",
                      "employee" to DEFAULT_EMPLOYEE.id.toString())
        val ex = assertThrows(IllegalStateException::class.java){ handlePOSTRegister(au, NO_USER, data) }
        assertEquals("The password must not be blank", ex.message)
    }

    @Test
    fun testShouldHandleInvalidInputs_blankEmployee() {
        val data = mapOf("username" to DEFAULT_USER.name,
                      "password" to DEFAULT_PASSWORD,
                      "employee" to "")
        val ex = assertThrows(IllegalStateException::class.java){ handlePOSTRegister(au, NO_USER, data) }
        assertEquals("The employee must not be blank", ex.message)
    }

    @Test
    fun testShouldHandleInvalidInputs_nonNumericEmployee() {
        val employee = "abc"
        val data = mapOf("username" to DEFAULT_USER.name,
                      "password" to DEFAULT_PASSWORD,
                      "employee" to employee)
        val ex = assertThrows(IllegalStateException::class.java){ handlePOSTRegister(au, NO_USER, data) }
        assertEquals("Must be able to parse $employee as integer", ex.message)
    }

    /**
     * If the employee id is zero
     */
    @Test
    fun testShouldHandleInvalidInputs_ZeroEmployee() {
        val data = mapOf("username" to DEFAULT_USER.name,
              "password" to DEFAULT_PASSWORD,
              "employee" to "0")
        val ex = assertThrows(IllegalStateException::class.java){ handlePOSTRegister(au, NO_USER, data) }
        assertEquals("The employee id must be greater than zero", ex.message)
    }

    /**
     * If the employee id is below zero
     */
    @Test
    fun testShouldHandleInvalidInputs_NegativeEmployee() {
        val data = mapOf("username" to DEFAULT_USER.name,
              "password" to DEFAULT_PASSWORD,
              "employee" to "-10")
        val ex = assertThrows(IllegalStateException::class.java){ handlePOSTRegister(au, NO_USER, data) }
        assertEquals("The employee id must be greater than zero", ex.message)
    }

    /**
     * If our API code is missing a required value
     */
    @Test
    fun testShouldHandleInvalidInputs_missingUsername() {
        val data = mapOf("password" to DEFAULT_PASSWORD,
                      "employee" to DEFAULT_EMPLOYEE.id.toString())
        val ex = assertThrows(IllegalStateException::class.java){ handlePOSTRegister(au, NO_USER, data) }
        assertEquals("username must not be missing", ex.message)
    }

    /**
     * If our API code is missing a required value
     */
    @Test
    fun testShouldHandleInvalidInputs_missingPassword() {
        val data = mapOf("username" to DEFAULT_USER.name,
                      "employee" to DEFAULT_EMPLOYEE.id.toString())
        val ex = assertThrows(IllegalStateException::class.java){ handlePOSTRegister(au, NO_USER, data) }
        assertEquals("password must not be missing", ex.message)
    }

    /**
     * If our API code is missing a required value
     */
    @Test
    fun testShouldHandleInvalidInputs_missingEmployee() {
        val data = mapOf("username" to DEFAULT_USER.name,
                      "password" to DEFAULT_PASSWORD)
        val ex = assertThrows(IllegalStateException::class.java){ handlePOSTRegister(au, NO_USER, data) }
        assertEquals("employee must not be missing", ex.message)
    }


}