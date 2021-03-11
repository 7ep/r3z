package coverosR3z.timerecording

import coverosR3z.authentication.exceptions.UnpermittedOperationException
import coverosR3z.authentication.utility.FakeAuthenticationUtilities
import coverosR3z.authentication.types.SYSTEM_USER
import coverosR3z.misc.*
import coverosR3z.misc.exceptions.InexactInputsException
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
        val sd = makeTypicalCEServerData(data)

        assertEquals(StatusCode.SEE_OTHER, CreateEmployeeAPI.handlePost(sd).statusCode)
    }

    /**
     * Huge name
     */
    @Category(APITestCategory::class)
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
    @Category(APITestCategory::class)
    @Test
    fun testHandlePOSTNewEmployee_BigName() {
        val data = PostBodyData(mapOf(Elements.EMPLOYEE_INPUT.getElemName() to "a".repeat(30)))
        val sd = makeTypicalCEServerData(data)

        assertEquals(StatusCode.SEE_OTHER, CreateEmployeeAPI.handlePost(sd).statusCode)
    }

    /**
     * Missing data
     */
    @Category(APITestCategory::class)
    @Test
    fun testHandlePOSTNewEmployee_noBody() {
        val data = PostBodyData()
        val sd = makeTypicalCEServerData(data)
        val ex = assertThrows(InexactInputsException::class.java){ CreateEmployeeAPI.handlePost(sd) }
        assertEquals("expected keys: [employee_name]. received keys: []", ex.message)
    }

    // region ROLE TESTS

    @Category(APITestCategory::class)
    @Test
    fun testShouldAllowAdminForPost() {
        val data = PostBodyData(mapOf(Elements.EMPLOYEE_INPUT.getElemName() to DEFAULT_EMPLOYEE_NAME.value))
        val sd = makeServerData(data, tru, au, user = DEFAULT_ADMIN_USER)

        assertEquals(StatusCode.SEE_OTHER, CreateEmployeeAPI.handlePost(sd).statusCode)
    }

    @Category(APITestCategory::class)
    @Test
    fun testShouldAllowSystemForPost() {
        val data = PostBodyData(mapOf(Elements.EMPLOYEE_INPUT.getElemName() to DEFAULT_EMPLOYEE_NAME.value))
        val sd = makeServerData(data, tru, au, user = SYSTEM_USER)

        assertEquals(StatusCode.SEE_OTHER, CreateEmployeeAPI.handlePost(sd).statusCode)
    }

    /**
     * if a user with an improper role tries something, the exception gets
     * bubbled up to [coverosR3z.server.utility.ServerUtilities.handleRequest]
     */
    @Category(APITestCategory::class)
    @Test
    fun testShouldDisallowRegularUserForPost() {
        val sd = makeServerData(PostBodyData(), tru, au, user = DEFAULT_REGULAR_USER)

        val result = CreateEmployeeAPI.handlePost(sd).statusCode

        assertEquals(StatusCode.FORBIDDEN, result)
    }

    /**
     * if a user with an improper role tries something, the exception gets
     * bubbled up to [coverosR3z.server.utility.ServerUtilities.handleRequest]
     */
    @Category(APITestCategory::class)
    @Test
    fun testShouldDisallowApproverUserForPost() {
        val sd = makeServerData(PostBodyData(), tru, au, user = DEFAULT_APPROVER)

        val result = CreateEmployeeAPI.handlePost(sd).statusCode

        assertEquals(StatusCode.FORBIDDEN, result)
    }

    @Category(APITestCategory::class)
    @Test
    fun testShouldAllowAdminToGetPageForPost() {
        val data = PostBodyData(mapOf(Elements.EMPLOYEE_INPUT.getElemName() to DEFAULT_EMPLOYEE_NAME.value))
        val sd = makeServerData(data, tru, au, user = DEFAULT_ADMIN_USER)

        assertEquals(StatusCode.OK, CreateEmployeeAPI.handleGet(sd).statusCode)
    }

    /**
     * Why would the system be GET'ing this page? disallow
     */
    @Category(APITestCategory::class)
    @Test
    fun testShouldDisallowSystemToGetPage() {
        val sd = makeServerData(PostBodyData(), tru, au, user = SYSTEM_USER)

        val result = CreateEmployeeAPI.handleGet(sd).statusCode

        assertEquals(StatusCode.FORBIDDEN, result)
    }

    /**
     * if a user with an improper role tries something, the exception gets
     * bubbled up to [coverosR3z.server.utility.ServerUtilities.handleRequest]
     */
    @Category(APITestCategory::class)
    @Test
    fun testShouldDisallowRegularUserToGetPage() {
        val sd = makeServerData(PostBodyData(), tru, au, user = DEFAULT_REGULAR_USER)

        val result = CreateEmployeeAPI.handleGet(sd).statusCode

        assertEquals(StatusCode.FORBIDDEN, result)
    }

    /**
     * if a user with an improper role tries something, the exception gets
     * bubbled up to [coverosR3z.server.utility.ServerUtilities.handleRequest]
     */
    @Category(APITestCategory::class)
    @Test
    fun testShouldDisallowApproverUserToGetPage() {
        val sd = makeServerData(PostBodyData(), tru, au, user = DEFAULT_APPROVER)

        val result = CreateEmployeeAPI.handleGet(sd).statusCode

        assertEquals(StatusCode.FORBIDDEN, result)
    }

    // endregion


    /**
     * Simpler helper to make the server data commonly used for CreateEmployee
     */
    private fun makeTypicalCEServerData(data: PostBodyData): ServerData {
        return makeServerData(data, tru, au)
    }
}