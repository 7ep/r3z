package coverosR3z.timerecording.api

import coverosR3z.authentication.types.CurrentUser
import coverosR3z.authentication.types.NO_USER
import coverosR3z.authentication.types.Role
import coverosR3z.authentication.types.SYSTEM_USER
import coverosR3z.authentication.utility.AuthenticationUtilities
import coverosR3z.authentication.utility.FakeAuthenticationUtilities
import coverosR3z.persistence.utility.PureMemoryDatabase
import coverosR3z.server.APITestCategory
import coverosR3z.server.api.MessageAPI
import coverosR3z.server.types.AuthStatus
import coverosR3z.server.types.PostBodyData
import coverosR3z.server.types.ServerData
import coverosR3z.system.misc.*
import coverosR3z.system.misc.exceptions.InexactInputsException
import coverosR3z.timerecording.FakeTimeRecordingUtilities
import coverosR3z.timerecording.types.NO_EMPLOYEE
import coverosR3z.timerecording.utility.TimeRecordingUtilities
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.experimental.categories.Category

@Category(APITestCategory::class)
class RoleAPITests {

    lateinit var au : FakeAuthenticationUtilities
    lateinit var tru : FakeTimeRecordingUtilities

    @Before
    fun init() {
        au = FakeAuthenticationUtilities()
        tru = FakeTimeRecordingUtilities()
    }

    @Test
    fun `should be able to set an employee as a regular role`() {
        val expected = MessageAPI.createCustomMessageRedirect(
            "${DEFAULT_REGULAR_USER.employee.name.value} now has a role of: regular",
            true,
            CreateEmployeeAPI.path
        )
        tru.findEmployeeByIdBehavior = { DEFAULT_EMPLOYEE }
        au.getUserByEmployeeBehavior = { DEFAULT_REGULAR_USER }
        val data = PostBodyData(
            mapOf(
                RoleAPI.Elements.EMPLOYEE_ID.getElemName() to DEFAULT_REGULAR_USER.employee.id.value.toString(),
                RoleAPI.Elements.ROLE.getElemName() to "regular"
            )
        )
        val sd = makeSetApproverServerData(data)

        val result = RoleAPI.handlePost(sd)

        assertEquals(expected, result)
    }

    @Test
    fun `should be able to set an employee as an approver role`() {
        val expected = MessageAPI.createCustomMessageRedirect(
            "${DEFAULT_REGULAR_USER.employee.name.value} now has a role of: approver",
            true,
            CreateEmployeeAPI.path
        )
        tru.findEmployeeByIdBehavior = { DEFAULT_EMPLOYEE }
        au.getUserByEmployeeBehavior = { DEFAULT_REGULAR_USER }
        val data = PostBodyData(
            mapOf(
                RoleAPI.Elements.EMPLOYEE_ID.getElemName() to DEFAULT_REGULAR_USER.employee.id.value.toString(),
                RoleAPI.Elements.ROLE.getElemName() to "approver"
            )
        )
        val sd = makeSetApproverServerData(data)

        val result = RoleAPI.handlePost(sd)

        assertEquals(expected, result)
    }

    @Test
    fun `should be able to set an employee as an admin role`() {
        val expected = MessageAPI.createCustomMessageRedirect(
            "${DEFAULT_REGULAR_USER.employee.name.value} now has a role of: admin",
            true,
            CreateEmployeeAPI.path
        )
        tru.findEmployeeByIdBehavior = { DEFAULT_EMPLOYEE }
        au.getUserByEmployeeBehavior = { DEFAULT_REGULAR_USER }
        val data = PostBodyData(
            mapOf(
                RoleAPI.Elements.EMPLOYEE_ID.getElemName() to DEFAULT_REGULAR_USER.employee.id.value.toString(),
                RoleAPI.Elements.ROLE.getElemName() to "admin"
            )
        )
        val sd = makeSetApproverServerData(data)

        val result = RoleAPI.handlePost(sd)

        assertEquals(expected, result)
    }

    @Test
    fun `passing in a name that does not correspond to any employee should result in a complaint`(){
        val expected = MessageAPI.createCustomMessageRedirect(
            "No employee was found with an id of ${DEFAULT_REGULAR_USER.employee.id.value}",
            false,
            RoleAPI.path
        )
        tru.findEmployeeByIdBehavior = { NO_EMPLOYEE }
        val data = PostBodyData(
            mapOf(
                RoleAPI.Elements.EMPLOYEE_ID.getElemName() to DEFAULT_REGULAR_USER.employee.id.value.toString(),
                RoleAPI.Elements.ROLE.getElemName() to "approver"
            )
        )
        val sd = makeSetApproverServerData(data)

        val result = RoleAPI.handlePost(sd)

        assertEquals(expected, result)
    }

    @Test
    fun `passing in an employee who has no associated user should result in a complaint`() {
        val expected = MessageAPI.createCustomMessageRedirect(
            "No user associated with the employee named ${DEFAULT_REGULAR_USER.employee.name.value} and id ${DEFAULT_REGULAR_USER.employee.id.value}",
            false,
            RoleAPI.path
        )
        tru.findEmployeeByIdBehavior = { DEFAULT_EMPLOYEE }
        au.getUserByEmployeeBehavior = { NO_USER }
        val data = PostBodyData(
            mapOf(
                RoleAPI.Elements.EMPLOYEE_ID.getElemName() to DEFAULT_REGULAR_USER.employee.id.value.toString(),
                RoleAPI.Elements.ROLE.getElemName() to "approver"
            )
        )
        val sd = makeSetApproverServerData(data)

        val result = RoleAPI.handlePost(sd)

        assertEquals(expected, result)
    }

    @Test
    fun `should throw an exception if the client does not pass in the role`() {
        val data = PostBodyData(
            mapOf(
                RoleAPI.Elements.EMPLOYEE_ID.getElemName() to DEFAULT_REGULAR_USER.employee.id.value.toString(),
            )
        )
        val sd = makeSetApproverServerData(data)

        val result = assertThrows(InexactInputsException::class.java) { RoleAPI.handlePost(sd) }

        assertEquals("expected keys: [employee_id, role]. received keys: [employee_id]", result.message)
    }

    @Test
    fun `should throw an exception if the client does not pass in the employee id`() {
        val data = PostBodyData(
            mapOf(
                RoleAPI.Elements.ROLE.getElemName() to "approver"
            )
        )
        val sd = makeSetApproverServerData(data)

        val result = assertThrows(InexactInputsException::class.java) { RoleAPI.handlePost(sd) }

        assertEquals("expected keys: [employee_id, role]. received keys: [role]", result.message)
    }

    /**
     * The moment a user's role gets changed, the system should treat them
     * according to their new role immediately.
     *
     * This tests by using real state within the test.
     *
     * In here, we'll start with a user who has admin privileges,
     * and then switch them to regular and confirm they cannot see
     * a page only admins could see.
     */
    @Category(IntegrationTestCategory::class)
    @Test
    fun `should immediately change a user's role`() {
        val expected = MessageAPI.createCustomMessageRedirect(
            "${DEFAULT_REGULAR_USER.employee.name.value} now has a role of: regular",
            true,
            CreateEmployeeAPI.path
        )

        var cu = CurrentUser(SYSTEM_USER)
        val pmd = PureMemoryDatabase.createEmptyDatabase()
        var tru = TimeRecordingUtilities(pmd, cu, testLogger)
        var au = AuthenticationUtilities(pmd, testLogger, cu)
        // make a new employee (required for a user)
        val defaultEmployee = tru.createEmployee(DEFAULT_EMPLOYEE_NAME)
        // make a new user
        val (_, defaultUser) = au.registerWithEmployee(DEFAULT_USER.name, DEFAULT_PASSWORD, defaultEmployee)
        // make them an admin, so they can run this API command.
        val defaultUserAdmin = au.addRoleToUser(defaultUser, Role.ADMIN)
        cu = CurrentUser(defaultUser)
        tru = TimeRecordingUtilities(pmd, cu, testLogger)
        au = AuthenticationUtilities(pmd, testLogger, cu)

        assertEquals(Role.ADMIN, defaultUserAdmin.role)

        val data = PostBodyData(
            mapOf(
                RoleAPI.Elements.EMPLOYEE_ID.getElemName() to defaultUserAdmin.employee.id.value.toString(),
                RoleAPI.Elements.ROLE.getElemName() to "regular"
            )
        )

        val sd = makeServerData(data, tru, au, AuthStatus.AUTHENTICATED, user = defaultUserAdmin, path = RoleAPI.path)

        // this is the point where they change the role
        val successfulRoleChangeResult = RoleAPI.handlePost(sd)
        assertEquals(expected, successfulRoleChangeResult)

        assertEquals(Role.ADMIN, cu.role)
        val failedRoleChangeResult = RoleAPI.handlePost(sd) // at this point, it shouldn't work
        assertEquals("User lacked proper role for this action. Roles allowed: SYSTEM;ADMIN. Your role: REGULAR", failedRoleChangeResult.headers)
    }


    /**
     * A helper method for the ordinary [ServerData] present during login
     */
    private fun makeSetApproverServerData(data: PostBodyData): ServerData {
        return makeServerData(data, tru, au, AuthStatus.AUTHENTICATED, user = DEFAULT_ADMIN_USER, path = RoleAPI.path)
    }
}