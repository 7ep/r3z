package coverosR3z.timerecording

import coverosR3z.misc.DEFAULT_EMPLOYEE_NAME
import coverosR3z.authentication.FakeAuthenticationUtilities
import coverosR3z.authentication.utility.IAuthenticationUtilities
import coverosR3z.fakeServerObjects
import coverosR3z.fakeTechempower
import coverosR3z.misc.DEFAULT_USER_SYSTEM_EMPLOYEE
import coverosR3z.misc.exceptions.InexactInputsException
import coverosR3z.misc.testLogger
import coverosR3z.server.APITestCategory
import coverosR3z.server.types.*
import coverosR3z.timerecording.api.CreateEmployeeAPI
import coverosR3z.timerecording.api.CreateEmployeeAPI.Elements
import coverosR3z.timerecording.utility.ITimeRecordingUtilities
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.experimental.categories.Category

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
    @Category(APITestCategory::class)
    @Test
    fun testHandlePOSTNewEmployee() {
        val data = PostBodyData(mapOf(Elements.EMPLOYEE_INPUT.getElemName() to DEFAULT_EMPLOYEE_NAME.value))
        val sd = makeServerData(data)

        assertEquals(StatusCode.OK, CreateEmployeeAPI.handlePost(sd).statusCode)
    }

    /**
     * Huge name
     */
    @Category(APITestCategory::class)
    @Test
    fun testHandlePOSTNewEmployee_HugeName() {
        val data = PostBodyData(mapOf(Elements.EMPLOYEE_INPUT.getElemName() to "a".repeat(31)))
        val sd = makeServerData(data)

        val ex = assertThrows(IllegalArgumentException::class.java){ CreateEmployeeAPI.handlePost(sd)}

        assertEquals("Max size of employee name is 30", ex.message)
    }

    /**
     * Big name, but acceptable
     */
    @Category(APITestCategory::class)
    @Test
    fun testHandlePOSTNewEmployee_BigName() {
        val data = PostBodyData(mapOf(Elements.EMPLOYEE_INPUT.getElemName() to "a".repeat(30)))
        val sd = makeServerData(data)

        assertEquals(StatusCode.OK, CreateEmployeeAPI.handlePost(sd).statusCode)
    }

    /**
     * Missing data
     */
    @Category(APITestCategory::class)
    @Test
    fun testHandlePOSTNewEmployee_noBody() {
        val data = PostBodyData()
        val sd = makeServerData(data)
        val ex = assertThrows(InexactInputsException::class.java){ CreateEmployeeAPI.handlePost(sd) }
        assertEquals("expected keys: [employee_name]. received keys: []", ex.message)
    }

    private fun makeServerData(data: PostBodyData): ServerData {
        val sd = ServerData(
            BusinessCode(tru, au, fakeTechempower),
            fakeServerObjects,
            AnalyzedHttpData(data = data, user = DEFAULT_USER_SYSTEM_EMPLOYEE),
            authStatus = AuthStatus.AUTHENTICATED,
            testLogger
        )
        return sd
    }
}