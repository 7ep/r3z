package coverosR3z.timerecording.api

import coverosR3z.authentication.types.SYSTEM_USER
import coverosR3z.authentication.types.User
import coverosR3z.authentication.utility.FakeAuthenticationUtilities
import coverosR3z.misc.*
import coverosR3z.server.APITestCategory
import coverosR3z.server.types.PostBodyData
import coverosR3z.server.types.ServerData
import coverosR3z.server.types.StatusCode
import coverosR3z.timerecording.FakeTimeRecordingUtilities
import coverosR3z.timerecording.types.NO_EMPLOYEE
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.experimental.categories.Category
import java.lang.IllegalStateException

@Category(APITestCategory::class)
class ApproveApiTests {

    lateinit var au : FakeAuthenticationUtilities
    lateinit var tru : FakeTimeRecordingUtilities
    private val defaultStartDate = "2021-01-01"

    @Before
    fun init() {
        au = FakeAuthenticationUtilities()
        tru = FakeTimeRecordingUtilities()
    }

    /**
     * A simple valid approval
     */
    @Test
    fun testApproveHappyPath() {
        tru.findEmployeeByIdBehavior = { DEFAULT_EMPLOYEE_2 }
        val sd = makeSdForApprove()

        val result = ApproveApi.handlePost(sd).statusCode

        assertEquals(StatusCode.SEE_OTHER, result)
    }

    // region Role tests

    /**
     * A user with a role of [coverosR3z.authentication.types.Role.REGULAR] cannot approve timesheets at all
     */
    @Test
    fun testApproval_badRole_Regular() {
        val sd = makeServerData(PostBodyData(), tru, au, user = DEFAULT_REGULAR_USER)

        val result = ApproveApi.handlePost(sd).statusCode

        assertEquals(StatusCode.FORBIDDEN, result)
    }

    /**
     * A user with a role of [coverosR3z.authentication.types.Role.SYSTEM] cannot approve timesheets at all
     */
    @Test
    fun testApproval_badRole_System() {
        val sd = makeServerData(PostBodyData(), tru, au, user = SYSTEM_USER)

        val result = ApproveApi.handlePost(sd).statusCode

        assertEquals(StatusCode.FORBIDDEN, result)
    }

    // endregion

    /**
     * If it's a valid id but it isn't found to connect to any employee
     */
    @Test
    fun testApprove_InvalidEmployeeId_NoEmployeeFound() {
        tru.findEmployeeByIdBehavior = { NO_EMPLOYEE }
        val sd = makeSdForApprove()

        val ex = assertThrows(IllegalStateException::class.java) { ApproveApi.handlePost(sd).statusCode }

        assertEquals("No employee was found with an id of ${DEFAULT_EMPLOYEE_2.id.value}", ex.message)
    }

    /**
     * If the employee id isn't even a valid integer
     */
    @Test
    fun testApprove_InvalidEmployeeId_EmployeeIdInvalid() {
        val sd = makeSdForApprove(employeeId = "a")

        val ex = assertThrows(IllegalStateException::class.java) { ApproveApi.handlePost(sd).statusCode }

        assertEquals("""The employee id was not interpretable as an integer.  You sent "a".""", ex.message)
    }

    /**
     * If the start date isn't a valid string
     */
    @Test
    fun testApprove_InvalidEmployeeId_StartDateInvalid() {
        tru.findEmployeeByIdBehavior = { DEFAULT_EMPLOYEE_2 }
        val sd = makeSdForApprove(startDate = "a")

        val ex = assertThrows(IllegalStateException::class.java) { ApproveApi.handlePost(sd).statusCode }

        assertEquals("""The date for approving time was not interpreted as a date. You sent "a".  Format is YYYY-MM-DD""", ex.message)
    }
    /**
     * If the employee id is blank...
     */
    @Test
    fun testApprove_InvalidEmployeeId_EmployeeIdBlank() {
        val sd = makeSdForApprove(employeeId = "")

        val ex = assertThrows(IllegalStateException::class.java) { ApproveApi.handlePost(sd).statusCode }

        assertEquals("""The employee id must not be blank""", ex.message)
    }

    /**
     * If the start date is blank...
     */
    @Test
    fun testApprove_InvalidEmployeeId_StartDateBlank() {
        tru.findEmployeeByIdBehavior = { DEFAULT_EMPLOYEE_2 }
        val sd = makeSdForApprove(startDate = "")

        val ex = assertThrows(IllegalStateException::class.java) { ApproveApi.handlePost(sd).statusCode }

        assertEquals("""The date for approving time was not interpreted as a date. You sent "".  Format is YYYY-MM-DD""", ex.message)
    }

    /**
     * What if we try to approve a timesheet that is not submitted?
     */
    @Test
    fun testApprove_TimesheetIsNotSubmitted() {
        tru.findEmployeeByIdBehavior = { DEFAULT_EMPLOYEE_2 }
        tru.approveTimesheetBehavior = { throw IllegalStateException("Cannot approve a non-submitted timesheet.  EmployeeId: ${DEFAULT_EMPLOYEE_2.id.value}, Timeperiod start: $defaultStartDate") }
        val sd = makeSdForApprove()

        val ex = assertThrows(IllegalStateException::class.java) { ApproveApi.handlePost(sd).statusCode }

        assertEquals("Cannot approve a non-submitted timesheet.  EmployeeId: ${DEFAULT_EMPLOYEE_2.id.value}, Timeperiod start: $defaultStartDate", ex.message)
    }

    /**
     * What if we try to approve our own timesheet?
     */
    @Test
    fun testApprove_TimesheetIsOurOwn() {
        tru.findEmployeeByIdBehavior = { DEFAULT_EMPLOYEE }
        tru.approveTimesheetBehavior = { throw IllegalStateException("Cannot approve your own timesheet") }
        val sd = makeSdForApprove()

        val ex = assertThrows(IllegalStateException::class.java) { ApproveApi.handlePost(sd).statusCode }

        assertEquals("Cannot approve your own timesheet", ex.message)
    }

    /**
     * The approve API allows us to unapprove if we pass in the right key-value.
     * [ApproveApi.Elements.IS_UNAPPROVAL] = "true"
     */
    @Test
    fun testApprove_Unapprove() {
        tru.findEmployeeByIdBehavior = { DEFAULT_EMPLOYEE_2 }
        val sd = makeSdForApprove(unapprove = true)

        val result = ApproveApi.handlePost(sd).statusCode

        assertEquals(StatusCode.SEE_OTHER, result)
    }

    /**
     * helper method for creating [ServerData] for this set of tests
     */
    private fun makeSdForApprove(employeeId: String = DEFAULT_EMPLOYEE_2.id.value.toString(),
                                 startDate: String = defaultStartDate,
                                 user: User = DEFAULT_ADMIN_USER,
                                 unapprove: Boolean = false,
                                 ): ServerData {
        val dataMap = mapOf(
            ViewTimeAPI.Elements.EMPLOYEE_TO_APPROVE_INPUT.getElemName() to employeeId,
            ViewTimeAPI.Elements.TIME_PERIOD.getElemName() to startDate,
            ApproveApi.Elements.IS_UNAPPROVAL.getElemName() to unapprove.toString()
        )
        val data = PostBodyData(
            dataMap
        )
        return makeServerData(data, tru, au, user = user)
    }

}