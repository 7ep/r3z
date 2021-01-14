package coverosR3z.timerecording

import coverosR3z.DEFAULT_EMPLOYEE_NAME
import coverosR3z.DEFAULT_USER_NO_EMPLOYEE
import coverosR3z.authentication.FakeAuthenticationUtilities
import coverosR3z.authentication.utility.IAuthenticationUtilities
import coverosR3z.misc.exceptions.InexactInputsException
import coverosR3z.server.types.*
import coverosR3z.timerecording.api.CreateEmployeeAPI
import coverosR3z.timerecording.api.CreateEmployeeAPI.Elements
import coverosR3z.timerecording.utility.ITimeRecordingUtilities
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test

class CreateEmployeeAPITests {


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
        val data = PostBodyData(mapOf(Elements.EMPLOYEE_INPUT.getElemName() to DEFAULT_EMPLOYEE_NAME.value))
        val sd = ServerData(au, tru, AnalyzedHttpData(data = data, user = DEFAULT_USER_NO_EMPLOYEE), authStatus = AuthStatus.AUTHENTICATED)

        assertEquals(StatusCode.OK, CreateEmployeeAPI.handlePost(sd).statusCode)
    }

    /**
     * Huge name
     */
    @Test
    fun testHandlePOSTNewEmployee_HugeName() {
        val data = PostBodyData(mapOf(Elements.EMPLOYEE_INPUT.getElemName() to "a".repeat(31)))
        val sd = ServerData(au, tru, AnalyzedHttpData(data = data, user = DEFAULT_USER_NO_EMPLOYEE), authStatus = AuthStatus.AUTHENTICATED)

        val ex = assertThrows(IllegalArgumentException::class.java){ CreateEmployeeAPI.handlePost(sd)}

        assertEquals("Max size of employee name is 30", ex.message)
    }

    /**
     * Big name, but acceptable
     */
    @Test
    fun testHandlePOSTNewEmployee_BigName() {
        val data = PostBodyData(mapOf(Elements.EMPLOYEE_INPUT.getElemName() to "a".repeat(30)))
        val sd = ServerData(au, tru, AnalyzedHttpData(data = data, user = DEFAULT_USER_NO_EMPLOYEE), authStatus = AuthStatus.AUTHENTICATED)

        assertEquals(StatusCode.OK, CreateEmployeeAPI.handlePost(sd).statusCode)
    }

    /**
     * Missing data
     */
    @Test
    fun testHandlePOSTNewEmployee_noBody() {
        val data = PostBodyData()
        val sd = ServerData(au, tru, AnalyzedHttpData(data = data, user = DEFAULT_USER_NO_EMPLOYEE), authStatus = AuthStatus.AUTHENTICATED)
        val ex = assertThrows(InexactInputsException::class.java){ CreateEmployeeAPI.handlePost(sd) }
        assertEquals("expected keys: [employee_name]. received keys: []", ex.message)
    }
}