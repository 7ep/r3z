package coverosR3z.timerecording.api

import coverosR3z.authentication.types.SYSTEM_USER
import coverosR3z.authentication.utility.FakeAuthenticationUtilities
import coverosR3z.authentication.utility.IAuthenticationUtilities
import coverosR3z.system.misc.*
import coverosR3z.system.misc.exceptions.InexactInputsException
import coverosR3z.server.APITestCategory
import coverosR3z.server.types.PostBodyData
import coverosR3z.server.types.ServerData
import coverosR3z.server.types.StatusCode
import coverosR3z.timerecording.FakeTimeRecordingUtilities
import coverosR3z.timerecording.types.maxProjectNameSize
import coverosR3z.timerecording.types.maxProjectNameSizeMsg
import coverosR3z.timerecording.utility.ITimeRecordingUtilities
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.experimental.categories.Category

class ProjectAPITests {


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
    fun testHandlePOSTNewProject() {
        val data = PostBodyData(mapOf(ProjectAPI.Elements.PROJECT_INPUT.getElemName() to DEFAULT_PROJECT_NAME.value))
        val sd = makeTypicalServerDataForProjectAPI(data)

        assertEquals(StatusCode.SEE_OTHER, ProjectAPI.handlePost(sd).statusCode)
    }

    /**
     * Huge name
     */
    @Category(APITestCategory::class)
    @Test
    fun testHandlePOSTNewProject_HugeName() {
        val data = PostBodyData(mapOf(ProjectAPI.Elements.PROJECT_INPUT.getElemName() to "a".repeat(maxProjectNameSize + 1)))
        val sd = makeTypicalServerDataForProjectAPI(data)

        val ex = assertThrows(IllegalArgumentException::class.java){ ProjectAPI.handlePost(sd)}

        assertEquals(maxProjectNameSizeMsg, ex.message)
    }

    /**
     * Big name, but acceptable
     */
    @Category(APITestCategory::class)
    @Test
    fun testHandlePOSTNewProject_BigName() {
        val data = PostBodyData(mapOf(ProjectAPI.Elements.PROJECT_INPUT.getElemName() to "a".repeat(maxProjectNameSize)))
        val sd = makeTypicalServerDataForProjectAPI(data)

        assertEquals(StatusCode.SEE_OTHER, ProjectAPI.handlePost(sd).statusCode)
    }

    /**
     * Missing data
     */
    @Category(APITestCategory::class)
    @Test
    fun testHandlePOSTNewProject_noBody() {
        val data = PostBodyData()
        val sd = makeTypicalServerDataForProjectAPI(data)
        val ex = assertThrows(InexactInputsException::class.java){  ProjectAPI.handlePost(sd) }
        assertEquals("expected keys: [project_name]. received keys: []", ex.message)
    }

    // region ROLES TESTS

    /**
     * Should only allow the admin to post
     */
    @Category(APITestCategory::class)
    @Test
    fun testShouldAllowAdminForPost() {
        val data = PostBodyData(mapOf(ProjectAPI.Elements.PROJECT_INPUT.getElemName() to DEFAULT_PROJECT_NAME.value))
        val sd = makeTypicalServerDataForProjectAPI(data)

        assertEquals(StatusCode.SEE_OTHER, ProjectAPI.handlePost(sd).statusCode)
    }

    /**
     * There's no need for the system role to create projects
     */
    @Category(APITestCategory::class)
    @Test
    fun testShouldDisallowSystemForPost() {
        val sd = makeServerData(PostBodyData(), tru, au, user = SYSTEM_USER)

        val result = ProjectAPI.handlePost(sd).statusCode

        assertEquals(StatusCode.FORBIDDEN, result)
    }

    /**
     * Disallow approvers to create projects
     */
    @Category(APITestCategory::class)
    @Test
    fun testShouldDisallowApproverForPost() {
        val sd = makeServerData(PostBodyData(), tru, au, user = DEFAULT_APPROVER)

        val result = ProjectAPI.handlePost(sd).statusCode

        assertEquals(StatusCode.FORBIDDEN, result)
    }

    /**
     * Disallow regular roles to create projects
     */
    @Category(APITestCategory::class)
    @Test
    fun testShouldDisallowRegularRoleForPost() {
        val sd = makeServerData(PostBodyData(), tru, au, user = DEFAULT_REGULAR_USER)

        val result = ProjectAPI.handlePost(sd).statusCode

        assertEquals(StatusCode.FORBIDDEN, result)
    }

    /**
     * Only the admin can view this page
     */
    @Category(APITestCategory::class)
    @Test
    fun testShouldAllowAdminForGet() {
        val data = PostBodyData(mapOf(ProjectAPI.Elements.PROJECT_INPUT.getElemName() to DEFAULT_PROJECT_NAME.value))
        val sd = makeTypicalServerDataForProjectAPI(data)

        assertEquals(StatusCode.OK, ProjectAPI.handleGet(sd).statusCode)
    }

    /**
     * Disallow the system role from viewing this page
     */
    @Category(APITestCategory::class)
    @Test
    fun testShouldDisallowSystemForGet() {
        val sd = makeServerData(PostBodyData(), tru, au, user = SYSTEM_USER)

        val result = ProjectAPI.handleGet(sd).statusCode

        assertEquals(StatusCode.FORBIDDEN, result)
    }

    /**
     * Disallow approvers seeing this page
     */
    @Category(APITestCategory::class)
    @Test
    fun testShouldDisallowApproverForGet() {
        val sd = makeServerData(PostBodyData(), tru, au, user = DEFAULT_APPROVER)

        val result = ProjectAPI.handleGet(sd).statusCode

        assertEquals(StatusCode.FORBIDDEN, result)
    }

    /**
     * Disallow regular users viewing this page
     */
    @Category(APITestCategory::class)
    @Test
    fun testShouldDisallowRegularRoleForGet() {
        val sd = makeServerData(PostBodyData(), tru, au, user = DEFAULT_REGULAR_USER)

        val result = ProjectAPI.handleGet(sd).statusCode

        assertEquals(StatusCode.FORBIDDEN, result)
    }

    // endregion


    private fun makeTypicalServerDataForProjectAPI(data: PostBodyData): ServerData {
        return makeServerData(data, tru, au, user = DEFAULT_ADMIN_USER)
    }
}