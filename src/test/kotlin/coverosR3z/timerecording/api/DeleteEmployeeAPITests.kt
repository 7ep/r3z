package coverosR3z.timerecording.api

import coverosR3z.authentication.types.NO_USER
import coverosR3z.authentication.types.SYSTEM_USER
import coverosR3z.authentication.types.User
import coverosR3z.authentication.utility.FakeAuthenticationUtilities
import coverosR3z.server.APITestCategory
import coverosR3z.server.types.PostBodyData
import coverosR3z.server.types.ServerData
import coverosR3z.server.types.StatusCode
import coverosR3z.system.misc.*
import coverosR3z.system.misc.exceptions.InexactInputsException
import coverosR3z.timerecording.FakeTimeRecordingUtilities
import coverosR3z.timerecording.types.NO_EMPLOYEE
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.experimental.categories.Category

@Category(APITestCategory::class)
class DeleteEmployeeAPITests {
    lateinit var au : FakeAuthenticationUtilities
    lateinit var tru : FakeTimeRecordingUtilities

    @Before
    fun init() {
        au = FakeAuthenticationUtilities()
        tru = FakeTimeRecordingUtilities()
    }

    /**
     * Basic happy path
     */
    @Test
    fun testDeleteEmployee() {
        au.getUserByEmployeeBehavior = { NO_USER }
        tru.findEmployeeByIdBehavior = { DEFAULT_EMPLOYEE }
        val data = PostBodyData(mapOf(
            DeleteEmployeeAPI.Elements.EMPLOYEE_ID.getElemName() to "1"
        ))
        val sd = makeDEServerData(data)

        val response = DeleteEmployeeAPI.handlePost(sd)

        assertEquals(StatusCode.SEE_OTHER, response.statusCode)
        assertEquals(1, response.headers.count())
        assertEquals("Location: result?msg=EMPLOYEE_DELETED", response.headers[0])
    }

    @Test
    fun testDeleteEmployee_NonNumericId() {
        au.getUserByEmployeeBehavior = { NO_USER }
        tru.findEmployeeByIdBehavior = { DEFAULT_EMPLOYEE }
        val data = PostBodyData(mapOf(
            DeleteEmployeeAPI.Elements.EMPLOYEE_ID.getElemName() to "abc"
        ))
        val sd = makeDEServerData(data)

        val response = DeleteEmployeeAPI.handlePost(sd)

        assertEquals(StatusCode.SEE_OTHER, response.statusCode)
        assertEquals(1, response.headers.count())
        assertEquals("Location: result?rtn=deleteemployee&suc=false&custommsg=The+employee+id+was+not+interpretable+as+an+integer.++You+sent+%22abc%22.", response.headers[0])
    }

    /**
     * If we send an ID for an employee that doesn't exist
     */
    @Test
    fun testDeleteEmployee_EmployeeNotFoundById() {
        au.getUserByEmployeeBehavior = { NO_USER }
        tru.findEmployeeByIdBehavior = { NO_EMPLOYEE }
        val data = PostBodyData(mapOf(
            DeleteEmployeeAPI.Elements.EMPLOYEE_ID.getElemName() to "1"
        ))
        val sd = makeDEServerData(data)

        val response = DeleteEmployeeAPI.handlePost(sd)

        assertEquals(StatusCode.SEE_OTHER, response.statusCode)
        assertEquals(1, response.headers.count())
        assertEquals("Location: result?rtn=deleteemployee&suc=false&custommsg=No+employee+found+by+that+id", response.headers[0])
    }

    /**
     * If we send an ID for an employee that a user is registered to,
     * then we cannot delete it
     */
    @Test
    fun testDeleteEmployee_UserRegisteredToEmployee() {
        au.getUserByEmployeeBehavior = { DEFAULT_USER }
        tru.findEmployeeByIdBehavior = { DEFAULT_EMPLOYEE }
        val data = PostBodyData(mapOf(
            DeleteEmployeeAPI.Elements.EMPLOYEE_ID.getElemName() to "1"
        ))
        val sd = makeDEServerData(data)

        val response = DeleteEmployeeAPI.handlePost(sd)

        assertEquals(StatusCode.SEE_OTHER, response.statusCode)
        assertEquals(1, response.headers.count())
        assertEquals("Location: result?msg=EMPLOYEE_USED", response.headers[0])
    }

    // if we are missing the id, get an exception
    @Test
    fun testDeleteEmployee_MissingId() {
        au.getUserByEmployeeBehavior = { DEFAULT_USER }
        tru.findEmployeeByIdBehavior = { DEFAULT_EMPLOYEE }
        val data = PostBodyData(emptyMap())
        val sd = makeDEServerData(data)

        val ex = assertThrows(InexactInputsException::class.java) { DeleteEmployeeAPI.handlePost(sd) }
        assertEquals("expected keys: [employeeid]. received keys: []", ex.message)
    }

    // region role tests


    @Test
    fun testDeleteProject_Roles_NotAllowed_Regular() {
        val data = PostBodyData(mapOf(
            DeleteEmployeeAPI.Elements.EMPLOYEE_ID.getElemName() to "1"
        ))
        val sd = makeDEServerData(data, DEFAULT_REGULAR_USER)

        val response = DeleteEmployeeAPI.handlePost(sd).statusCode

        assertEquals(StatusCode.FORBIDDEN, response)
    }

    @Test
    fun testDeleteProject_Roles_NotAllowed_Approver() {
        val data = PostBodyData(mapOf(
            DeleteEmployeeAPI.Elements.EMPLOYEE_ID.getElemName() to "1"
        ))
        val sd = makeDEServerData(data, DEFAULT_APPROVER)

        val response = DeleteEmployeeAPI.handlePost(sd).statusCode

        assertEquals(StatusCode.FORBIDDEN, response)
    }


    @Test
    fun testDeleteProject_Roles_NotAllowed_System() {
        val data = PostBodyData(mapOf(
            DeleteEmployeeAPI.Elements.EMPLOYEE_ID.getElemName() to "1"
        ))
        val sd = makeDEServerData(data, SYSTEM_USER)

        val response = DeleteEmployeeAPI.handlePost(sd).statusCode

        assertEquals(StatusCode.FORBIDDEN, response)
    }

    // endregion

    /**
     * Helper method to make a [ServerData] for the delete employee API tests
     */
    private fun makeDEServerData(data: PostBodyData, user: User = DEFAULT_ADMIN_USER): ServerData {
        return makeServerData(data, tru, au, user = user, path = DeleteProjectAPI.path)
    }
}