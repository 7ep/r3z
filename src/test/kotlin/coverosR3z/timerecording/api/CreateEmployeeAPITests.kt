package coverosR3z.timerecording.api

import coverosR3z.authentication.types.NO_USER
import coverosR3z.authentication.types.SYSTEM_USER
import coverosR3z.authentication.types.User
import coverosR3z.authentication.utility.FakeAuthenticationUtilities
import coverosR3z.system.misc.*
import coverosR3z.system.misc.exceptions.InexactInputsException
import coverosR3z.server.APITestCategory
import coverosR3z.server.api.MessageAPI
import coverosR3z.server.types.PostBodyData
import coverosR3z.server.types.ServerData
import coverosR3z.server.types.StatusCode
import coverosR3z.timerecording.FakeTimeRecordingUtilities
import coverosR3z.timerecording.api.CreateEmployeeAPI.Elements
import coverosR3z.timerecording.types.*
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
        assertTrue(result.contains("register?invitation=abc123"))
    }

    /**
     * Huge name
     */
    @Test
    fun testHandlePOSTNewEmployee_HugeName() {
        val expected = MessageAPI.createCustomMessageRedirect(
            "Max size of employee name is $maxEmployeeNameSize",
            false,
            CreateEmployeeAPI.path)

        val data = PostBodyData(mapOf(Elements.EMPLOYEE_INPUT.getElemName() to "a".repeat(31)))
        val sd = makeTypicalCEServerData(data)

        val result = CreateEmployeeAPI.handlePost(sd)

        assertEquals(expected, result)
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
        val sd = makeTypicalCEServerData(user = DEFAULT_APPROVER_USER)

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
        val sd = makeTypicalCEServerData(user = DEFAULT_APPROVER_USER)

        val result = CreateEmployeeAPI.handleGet(sd).statusCode

        assertEquals(StatusCode.FORBIDDEN, result)
    }

    // endregion

    @Test
    fun testShouldDisallowDuplicateEmployeeNames() {
        val expected = MessageAPI.createEnumMessageRedirect(MessageAPI.Message.FAILED_CREATE_EMPLOYEE_DUPLICATE)
        tru.findEmployeeByNameBehavior = { Employee(EmployeeId(1), EmployeeName("abc123")) }
        val data = PostBodyData(mapOf(Elements.EMPLOYEE_INPUT.getElemName() to "abc123"))
        val sd = makeTypicalCEServerData(data)

        val result = CreateEmployeeAPI.handlePost(sd)

        assertEquals(expected, result)
    }

    /**
     * We will disallow duplicates, and disregard surrounding whitespace.
     * That is, "abc" is equivalent to "   abc   "
     */
    @Test
    fun testShouldDisallowDuplicateEmployeeNamesWithSurroundingWhitespace() {
        val expected = MessageAPI.createEnumMessageRedirect(MessageAPI.Message.FAILED_CREATE_EMPLOYEE_DUPLICATE)
        tru.findEmployeeByNameBehavior = { Employee(EmployeeId(1), EmployeeName("abc123")) }
        val data = PostBodyData(mapOf(Elements.EMPLOYEE_INPUT.getElemName() to "   abc123   "))
        val sd = makeTypicalCEServerData(data)

        val result = CreateEmployeeAPI.handlePost(sd)

        assertEquals(expected, result)
    }

    /**
     * The role buttons for an employee let us change their role, but it's actually
     * tied to the user - and we cannot change the role unless they register a user
     * to the employee.  Until they register, the "delete" button will remain active
     * and the role-changing buttons will all be disabled
     */
    @Test
    fun `if an employee has no associated user, all of their role buttons should be disabled`() {
        // create a couple employees...
        tru.listAllEmployeesBehavior = { listOf(DEFAULT_EMPLOYEE, DEFAULT_EMPLOYEE_2) }
        // but none of them have an associated user
        au.getUserByEmployeeBehavior = { NO_USER }
        val sd = makeTypicalCEServerData()

        val result = CreateEmployeeAPI.handleGet(sd)

        assertTrue(result.fileContentsString().contains("""<button disabled class="${Elements.MAKE_REGULAR.getElemClass()}""""))
        assertTrue(result.fileContentsString().contains("""<button disabled class="${Elements.MAKE_APPROVER.getElemClass()}""""))
        assertTrue(result.fileContentsString().contains("""<button disabled class="${Elements.MAKE_ADMINISTRATOR.getElemClass()}""""))
    }

    @Test
    fun `if an employee has a user, and they are a regular role, then just the regular role button should be disabled`() {
        // create a couple employees...
        tru.listAllEmployeesBehavior = { listOf(DEFAULT_EMPLOYEE, DEFAULT_EMPLOYEE_2) }
        // and they all have a user - admittedly this is a bit artificial,
        // they can't *really* all be tied to this one user.  This user
        // has a role of Role.REGULAR
        au.getUserByEmployeeBehavior = { DEFAULT_REGULAR_USER }
        val sd = makeTypicalCEServerData()

        val result = CreateEmployeeAPI.handleGet(sd)

        assertTrue(result.fileContentsString().contains("""<button disabled class="${Elements.MAKE_REGULAR.getElemClass()}""""))
        assertTrue(result.fileContentsString().contains("""<button  class="${Elements.MAKE_APPROVER.getElemClass()}""""))
        assertTrue(result.fileContentsString().contains("""<button  class="${Elements.MAKE_ADMINISTRATOR.getElemClass()}""""))
    }

    @Test
    fun `if an employee has a user, and they are an approver role, then just the approver role button should be disabled`() {
        // create a couple employees...
        tru.listAllEmployeesBehavior = { listOf(DEFAULT_EMPLOYEE, DEFAULT_EMPLOYEE_2) }
        // and they all have a user - admittedly this is a bit artificial,
        // they can't *really* all be tied to this one user.  This user
        // has a role of Role.APPROVER
        au.getUserByEmployeeBehavior = { DEFAULT_APPROVER_USER }
        val sd = makeTypicalCEServerData()

        val result = CreateEmployeeAPI.handleGet(sd)

        assertTrue(result.fileContentsString().contains("""<button  class="${Elements.MAKE_REGULAR.getElemClass()}""""))
        assertTrue(result.fileContentsString().contains("""<button disabled class="${Elements.MAKE_APPROVER.getElemClass()}""""))
        assertTrue(result.fileContentsString().contains("""<button  class="${Elements.MAKE_ADMINISTRATOR.getElemClass()}""""))
    }

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
        return makeServerData(data, tru, au, user = user, path = CreateEmployeeAPI.path)
    }
}