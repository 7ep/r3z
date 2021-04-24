package coverosR3z.timerecording.api

import coverosR3z.authentication.types.SYSTEM_USER
import coverosR3z.authentication.types.User
import coverosR3z.authentication.utility.FakeAuthenticationUtilities
import coverosR3z.system.misc.*
import coverosR3z.system.misc.exceptions.InexactInputsException
import coverosR3z.server.APITestCategory
import coverosR3z.server.types.PostBodyData
import coverosR3z.server.types.ServerData
import coverosR3z.server.types.StatusCode
import coverosR3z.timerecording.FakeTimeRecordingUtilities
import coverosR3z.timerecording.api.CreateEmployeeAPI.Elements
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.experimental.categories.Category

@Category(APITestCategory::class)
class CreateEmployeeAPITests {


    lateinit var au : FakeAuthenticationUtilities
    lateinit var tru : FakeTimeRecordingUtilities

    @Before
    fun init() {
        au = FakeAuthenticationUtilities()
        tru = FakeTimeRecordingUtilities()
    }

    /**
     * A basic happy path for POST
     */
    @Test
    fun testHandlePOSTNewEmployee() {
        val data = PostBodyData(mapOf(Elements.EMPLOYEE_INPUT.getElemName() to DEFAULT_EMPLOYEE_NAME.value))
        val sd = makeTypicalCEServerData(data)

        assertEquals(StatusCode.SEE_OTHER, CreateEmployeeAPI.handlePost(sd).statusCode)
    }

    /**
     * Basic happy path for GET
     */
    @Test
    fun testHandleGetEmployees() {
        tru.listAllEmployeesBehavior = {listOf(DEFAULT_EMPLOYEE, DEFAULT_EMPLOYEE_2)}
        au.listAllInvitationsBehavior = {setOf(DEFAULT_INVITATION)}
        val sd = makeTypicalCEServerData()

        val result = CreateEmployeeAPI.handleGet(sd).fileContentsString()

        assertTrue(result.contains("DefaultEmployee"))
        assertTrue(result.contains("register?code=abc123"))
    }

    /**
     * Huge name
     */
    @Test
    fun testHandlePOSTNewEmployee_HugeName() {
        val data = PostBodyData(mapOf(Elements.EMPLOYEE_INPUT.getElemName() to "a".repeat(31)))
        val sd = makeTypicalCEServerData(data)

        val ex = assertThrows(IllegalArgumentException::class.java){ CreateEmployeeAPI.handlePost(sd)}

        assertEquals("Max size of employee name is 30", ex.message)
    }

    /**
     * Big name, but acceptable
     */
    @Test
    fun testHandlePOSTNewEmployee_BigName() {
        val data = PostBodyData(mapOf(Elements.EMPLOYEE_INPUT.getElemName() to "a".repeat(30)))
        val sd = makeTypicalCEServerData(data)

        assertEquals(StatusCode.SEE_OTHER, CreateEmployeeAPI.handlePost(sd).statusCode)
    }

    /**
     * Missing data
     */
    @Test
    fun testHandlePOSTNewEmployee_noBody() {
        val data = PostBodyData()
        val sd = makeTypicalCEServerData(data)
        val ex = assertThrows(InexactInputsException::class.java){ CreateEmployeeAPI.handlePost(sd) }
        assertEquals("expected keys: [employee_name]. received keys: []", ex.message)
    }

    // region ROLE TESTS

    @Test
    fun testShouldAllowAdminForPost() {
        val data = PostBodyData(mapOf(Elements.EMPLOYEE_INPUT.getElemName() to DEFAULT_EMPLOYEE_NAME.value))
        val sd = makeTypicalCEServerData(data=data, user = DEFAULT_ADMIN_USER)

        assertEquals(StatusCode.SEE_OTHER, CreateEmployeeAPI.handlePost(sd).statusCode)
    }

    @Test
    fun testShouldAllowSystemForPost() {
        val data = PostBodyData(mapOf(Elements.EMPLOYEE_INPUT.getElemName() to DEFAULT_EMPLOYEE_NAME.value))
        val sd = makeTypicalCEServerData(data=data, user = SYSTEM_USER)

        assertEquals(StatusCode.SEE_OTHER, CreateEmployeeAPI.handlePost(sd).statusCode)
    }

    /**
     * if a user with an improper role tries something, the exception gets
     * bubbled up to [coverosR3z.server.utility.ServerUtilities.handleRequest]
     */
    @Test
    fun testShouldDisallowRegularUserForPost() {
        val sd = makeTypicalCEServerData(user = DEFAULT_REGULAR_USER)

        val result = CreateEmployeeAPI.handlePost(sd).statusCode

        assertEquals(StatusCode.FORBIDDEN, result)
    }

    /**
     * if a user with an improper role tries something, the exception gets
     * bubbled up to [coverosR3z.server.utility.ServerUtilities.handleRequest]
     */
    @Test
    fun testShouldDisallowApproverUserForPost() {
        val sd = makeTypicalCEServerData(user = DEFAULT_APPROVER)

        val result = CreateEmployeeAPI.handlePost(sd).statusCode

        assertEquals(StatusCode.FORBIDDEN, result)
    }

    @Test
    fun testShouldAllowAdminToGetPageForPost() {
        val data = PostBodyData(mapOf(Elements.EMPLOYEE_INPUT.getElemName() to DEFAULT_EMPLOYEE_NAME.value))
        val sd = makeTypicalCEServerData(data=data, user = DEFAULT_ADMIN_USER)

        assertEquals(StatusCode.OK, CreateEmployeeAPI.handleGet(sd).statusCode)
    }

    /**
     * Why would the system be GET'ing this page? disallow
     */
    @Test
    fun testShouldDisallowSystemToGetPage() {
        val sd = makeTypicalCEServerData(user = SYSTEM_USER)

        val result = CreateEmployeeAPI.handleGet(sd).statusCode

        assertEquals(StatusCode.FORBIDDEN, result)
    }

    /**
     * if a user with an improper role tries something, the exception gets
     * bubbled up to [coverosR3z.server.utility.ServerUtilities.handleRequest]
     */
    @Test
    fun testShouldDisallowRegularUserToGetPage() {
        val sd = makeTypicalCEServerData(user = DEFAULT_REGULAR_USER)

        val result = CreateEmployeeAPI.handleGet(sd).statusCode

        assertEquals(StatusCode.FORBIDDEN, result)
    }

    /**
     * if a user with an improper role tries something, the exception gets
     * bubbled up to [coverosR3z.server.utility.ServerUtilities.handleRequest]
     */
    @Test
    fun testShouldDisallowApproverUserToGetPage() {
        val sd = makeTypicalCEServerData(user = DEFAULT_APPROVER)

        val result = CreateEmployeeAPI.handleGet(sd).statusCode

        assertEquals(StatusCode.FORBIDDEN, result)
    }

    // endregion


/*
 _ _       _                  __ __        _    _           _
| | | ___ | | ___  ___  _ _  |  \  \ ___ _| |_ | |_  ___  _| | ___
|   |/ ._>| || . \/ ._>| '_> |     |/ ._> | |  | . |/ . \/ . |<_-<
|_|_|\___.|_||  _/\___.|_|   |_|_|_|\___. |_|  |_|_|\___/\___|/__/
             |_|
 alt-text: Helper Methods
 */

    /**
     * Simpler helper to make the server data commonly used for CreateEmployee
     */
    private fun makeTypicalCEServerData(data: PostBodyData = PostBodyData(), user: User = DEFAULT_ADMIN_USER): ServerData {
        return makeServerData(data, tru, au, user = user)
    }
}