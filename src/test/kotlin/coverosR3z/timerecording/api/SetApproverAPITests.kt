package coverosR3z.timerecording.api

import coverosR3z.authentication.types.NO_USER
import coverosR3z.authentication.utility.FakeAuthenticationUtilities
import coverosR3z.server.APITestCategory
import coverosR3z.server.api.MessageAPI
import coverosR3z.server.types.AuthStatus
import coverosR3z.server.types.PostBodyData
import coverosR3z.server.types.ServerData
import coverosR3z.system.misc.*
import coverosR3z.timerecording.FakeTimeRecordingUtilities
import coverosR3z.timerecording.types.NO_EMPLOYEE
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.experimental.categories.Category

@Category(APITestCategory::class)
class SetApproverAPITests {

    lateinit var au : FakeAuthenticationUtilities
    lateinit var tru : FakeTimeRecordingUtilities

    @Before
    fun init() {
        au = FakeAuthenticationUtilities()
        tru = FakeTimeRecordingUtilities()
    }

    @Test
    fun `should be able to set an employee as an approver`() {
        val expected = MessageAPI.createCustomMessageRedirect(
            "${DEFAULT_REGULAR_USER.employee.name.value} is now an approver",
            true,
            SetApproverAPI.path
        )
        tru.findEmployeeByNameBehavior = { DEFAULT_EMPLOYEE }
        au.getUserByEmployeeBehavior = { DEFAULT_REGULAR_USER }
        val data = PostBodyData(
            mapOf(
                SetApproverAPI.Elements.EMPLOYEE_INPUT.getElemName() to DEFAULT_REGULAR_USER.employee.id.value.toString()
            )
        )
        val sd = makeSetApproverServerData(data)

        val result = SetApproverAPI.handlePost(sd)

        assertEquals(expected, result)
    }

    @Test
    fun `passing in a name that does not correspond to any employee should result in a complaint`(){
        val expected = MessageAPI.createCustomMessageRedirect(
            "No employee was found with a name of ${DEFAULT_REGULAR_USER.employee.name.value}",
            false,
            SetApproverAPI.path
        )
        tru.findEmployeeByNameBehavior = { NO_EMPLOYEE }
        val data = PostBodyData(
            mapOf(
                SetApproverAPI.Elements.EMPLOYEE_INPUT.getElemName() to DEFAULT_REGULAR_USER.employee.name.value
            )
        )
        val sd = makeSetApproverServerData(data)

        val result = SetApproverAPI.handlePost(sd)

        assertEquals(expected, result)
    }

    @Test
    fun `passing in an employee who has no associated user should result in a complaint`() {
        val expected = MessageAPI.createCustomMessageRedirect(
            "No user associated with the employee named ${DEFAULT_REGULAR_USER.employee.name.value}",
            false,
            SetApproverAPI.path
        )
        tru.findEmployeeByNameBehavior = { DEFAULT_EMPLOYEE }
        au.getUserByEmployeeBehavior = { NO_USER }
        val data = PostBodyData(
            mapOf(
                SetApproverAPI.Elements.EMPLOYEE_INPUT.getElemName() to DEFAULT_REGULAR_USER.employee.name.value
            )
        )
        val sd = makeSetApproverServerData(data)

        val result = SetApproverAPI.handlePost(sd)

        assertEquals(expected, result)
    }


    /**
     * A helper method for the ordinary [ServerData] present during login
     */
    private fun makeSetApproverServerData(data: PostBodyData): ServerData {
        return makeServerData(data, tru, au, AuthStatus.AUTHENTICATED, user = DEFAULT_ADMIN_USER, path = SetApproverAPI.path)
    }
}