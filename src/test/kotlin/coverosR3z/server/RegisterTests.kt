package coverosR3z.server

import coverosR3z.DEFAULT_EMPLOYEE
import coverosR3z.DEFAULT_PASSWORD
import coverosR3z.DEFAULT_USER
import coverosR3z.authentication.FakeAuthenticationUtilities
import coverosR3z.authentication.IAuthenticationUtilities
import coverosR3z.timerecording.FakeTimeRecordingUtilities
import coverosR3z.timerecording.ITimeRecordingUtilities
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.lang.IllegalStateException

/**
 * Tests related to registering a user
 */
class RegisterTests {

    lateinit var au : IAuthenticationUtilities
    lateinit var tru : ITimeRecordingUtilities
    lateinit var su : ServerUtilities

    @Before
    fun init() {
        au = FakeAuthenticationUtilities()
        tru = FakeTimeRecordingUtilities()
        su = ServerUtilities(au, tru)
    }

    /**
     * If our code received all the expected
     * values properly, it shouldn't complain
     */
    @Test
    fun testShouldHandleValidInputs() {
        val rd = RequestData(Verb.POST,
                NamedPaths.REGISTER.path,
                mapOf("username" to DEFAULT_USER.name,
                      "password" to DEFAULT_PASSWORD,
                      "employee" to DEFAULT_EMPLOYEE.id.toString()))
        val responseData = su.handleRequestAndRespond(rd)
        assertTrue("The system should indicate success.  File was ${responseData.fileContents}",
                responseData.fileContents.contains("SUCCESS"))
    }

    /**
     * If our API code is missing a required value
     */
    @Test
    fun testShouldHandleInvalidInputs_blankName() {
        val rd = RequestData(Verb.POST,
                NamedPaths.REGISTER.path,
                mapOf("username" to "",
                      "password" to DEFAULT_PASSWORD,
                      "employee" to DEFAULT_EMPLOYEE.id.toString()))
        val ex = assertThrows(IllegalStateException::class.java){su.handleRequestAndRespond(rd)}
        assertEquals("The username must not be blank", ex.message)
    }

    @Test
    fun testShouldHandleInvalidInputs_blankPassword() {
        val rd = RequestData(Verb.POST,
                NamedPaths.REGISTER.path,
                mapOf("username" to DEFAULT_USER.name,
                      "password" to "",
                      "employee" to DEFAULT_EMPLOYEE.id.toString()))
        val ex = assertThrows(IllegalStateException::class.java){su.handleRequestAndRespond(rd)}
        assertEquals("The password must not be blank", ex.message)
    }

    @Test
    fun testShouldHandleInvalidInputs_blankEmployee() {
        val rd = RequestData(Verb.POST,
                NamedPaths.REGISTER.path,
                mapOf("username" to DEFAULT_USER.name,
                      "password" to DEFAULT_PASSWORD,
                      "employee" to ""))
        val ex = assertThrows(IllegalStateException::class.java){su.handleRequestAndRespond(rd)}
        assertEquals("The employee must not be blank", ex.message)
    }

    @Test
    fun testShouldHandleInvalidInputs_nonNumericEmployee() {
        val employee = "abc"
        val rd = RequestData(Verb.POST,
                NamedPaths.REGISTER.path,
                mapOf("username" to DEFAULT_USER.name,
                      "password" to DEFAULT_PASSWORD,
                      "employee" to employee))
        val ex = assertThrows(IllegalStateException::class.java){su.handleRequestAndRespond(rd)}
        assertEquals("Must be able to convert $employee to an int", ex.message)
    }

    /**
     * If the employee id is zero
     */
    @Test
    fun testShouldHandleInvalidInputs_ZeroEmployee() {
        val rd = RequestData(Verb.POST,
                NamedPaths.REGISTER.path,
                mapOf("username" to DEFAULT_USER.name,
                      "password" to DEFAULT_PASSWORD,
                      "employee" to "0"))
        val ex = assertThrows(IllegalStateException::class.java){su.handleRequestAndRespond(rd)}
        assertEquals("The employee id must be greater than zero", ex.message)
    }

    /**
     * If the employee id is below zero
     */
    @Test
    fun testShouldHandleInvalidInputs_NegativeEmployee() {
        val rd = RequestData(Verb.POST,
                NamedPaths.REGISTER.path,
                mapOf("username" to DEFAULT_USER.name,
                      "password" to DEFAULT_PASSWORD,
                      "employee" to "-10"))
        val ex = assertThrows(IllegalStateException::class.java){su.handleRequestAndRespond(rd)}
        assertEquals("The employee id must be greater than zero", ex.message)
    }

    /**
     * If our API code is missing a required value
     */
    @Test
    fun testShouldHandleInvalidInputs_missingUsername() {
        val rd = RequestData(Verb.POST,
                NamedPaths.REGISTER.path,
                mapOf("password" to DEFAULT_PASSWORD,
                      "employee" to DEFAULT_EMPLOYEE.id.toString()))
        val ex = assertThrows(IllegalStateException::class.java){su.handleRequestAndRespond(rd)}
        assertEquals("username must not be missing", ex.message)
    }

    /**
     * If our API code is missing a required value
     */
    @Test
    fun testShouldHandleInvalidInputs_missingPassword() {
        val rd = RequestData(Verb.POST,
                NamedPaths.REGISTER.path,
                mapOf("username" to DEFAULT_USER.name,
                      "employee" to DEFAULT_EMPLOYEE.id.toString()))
        val ex = assertThrows(IllegalStateException::class.java){su.handleRequestAndRespond(rd)}
        assertEquals("password must not be missing", ex.message)
    }

    /**
     * If our API code is missing a required value
     */
    @Test
    fun testShouldHandleInvalidInputs_missingEmployee() {
        val rd = RequestData(Verb.POST,
                NamedPaths.REGISTER.path,
                mapOf("username" to DEFAULT_USER.name,
                      "password" to DEFAULT_PASSWORD))
        val ex = assertThrows(IllegalStateException::class.java){su.handleRequestAndRespond(rd)}
        assertEquals("employee must not be missing", ex.message)
    }


}