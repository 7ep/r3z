package coverosR3z.timerecording

import coverosR3z.misc.DEFAULT_EMPLOYEE_NAME
import coverosR3z.authentication.utility.FakeAuthenticationUtilities
import coverosR3z.authentication.types.Password
import coverosR3z.authentication.types.UserName
import coverosR3z.fakeServerObjects
import coverosR3z.misc.DEFAULT_USER_SYSTEM_EMPLOYEE
import coverosR3z.misc.exceptions.InexactInputsException
import coverosR3z.misc.makeServerData
import coverosR3z.misc.testLogger
import coverosR3z.server.APITestCategory
import coverosR3z.server.types.*
import coverosR3z.timerecording.api.CreateEmployeeAPI
import coverosR3z.timerecording.api.CreateEmployeeAPI.Elements
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.experimental.categories.Category

class CreateEmployeeAPITests {


    lateinit var au : FakeAuthenticationUtilities
    lateinit var tru : FakeTimeRecordingUtilities

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
        val sd = makeServerData(data, tru, au)

        assertEquals(StatusCode.OK, CreateEmployeeAPI.handlePost(sd).statusCode)
    }

    /**
     * Positive case
     * Angela, an admin should be able to make new employees
     */
    @Category(APITestCategory::class)
    @Test
    fun testHandlePOSTNewEmployeeAsAdmin() {
        au.login(UserName("Angela"), Password("password12345"))
        val data = PostBodyData(mapOf(Elements.EMPLOYEE_INPUT.getElemName() to DEFAULT_EMPLOYEE_NAME.value))
        val sd = makeServerData(data, tru, au)

        assertEquals(StatusCode.OK, CreateEmployeeAPI.handlePost(sd).statusCode)
    }

    /**
     * Negative test
     * Max, an employee, can't make employees
     */
    @Category(APITestCategory::class)
    @Test
    fun testHandlePOSTNewEmployeeAsEmployee() {
        au.login(UserName("Max"), Password("password12345"))
        tru.createEmployeeBehavior = { throw IllegalStateException("This is bad, you are bad") }
        val data = PostBodyData(mapOf(Elements.EMPLOYEE_INPUT.getElemName() to DEFAULT_EMPLOYEE_NAME.value))
        val sd = makeServerData(data, tru, au)

        assertThrows(java.lang.IllegalStateException::class.java) { CreateEmployeeAPI.handlePost(sd).statusCode }
    }

    /**
     * Huge name
     */
    @Category(APITestCategory::class)
    @Test
    fun testHandlePOSTNewEmployee_HugeName() {
        val data = PostBodyData(mapOf(Elements.EMPLOYEE_INPUT.getElemName() to "a".repeat(31)))
        val sd = makeServerData(data, tru, au)

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
        val sd = makeServerData(data, tru, au)

        assertEquals(StatusCode.OK, CreateEmployeeAPI.handlePost(sd).statusCode)
    }

    /**
     * Missing data
     */
    @Category(APITestCategory::class)
    @Test
    fun testHandlePOSTNewEmployee_noBody() {
        val data = PostBodyData()
        val sd = makeServerData(data, tru, au)
        val ex = assertThrows(InexactInputsException::class.java){ CreateEmployeeAPI.handlePost(sd) }
        assertEquals("expected keys: [employee_name]. received keys: []", ex.message)
    }

}