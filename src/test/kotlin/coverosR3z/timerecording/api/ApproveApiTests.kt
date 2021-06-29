package coverosR3z.timerecording.api

import coverosR3z.authentication.types.SYSTEM_USER
import coverosR3z.authentication.types.User
import coverosR3z.authentication.utility.FakeAuthenticationUtilities
import coverosR3z.system.misc.*
import coverosR3z.server.APITestCategory
import coverosR3z.server.api.MessageAPI
import coverosR3z.server.types.PostBodyData
import coverosR3z.server.types.ServerData
import coverosR3z.server.types.StatusCode
import coverosR3z.system.misc.exceptions.InexactInputsException
import coverosR3z.timerecording.FakeTimeRecordingUtilities
import coverosR3z.timerecording.types.NO_EMPLOYEE
import coverosR3z.timerecording.types.maxEmployeeNameSize
import org.junit.Assert.*
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
        val sd = makeSdForApprove(user = DEFAULT_REGULAR_USER)

        val result = ApproveApi.handlePost(sd).statusCode

        assertEquals(StatusCode.FORBIDDEN, result)
    }

    /**
     * A user with a role of [coverosR3z.authentication.types.Role.SYSTEM] cannot approve timesheets at all
     */
    @Test
    fun testApproval_badRole_System() {
        val sd = makeSdForApprove(user = SYSTEM_USER)

        val result = ApproveApi.handlePost(sd).statusCode

        assertEquals(StatusCode.FORBIDDEN, result)
    }

    // endregion

    /**
     * If it's a valid id but it isn't found to connect to any employee
     */
    @Test
    fun testApprove_InvalidEmployeeId_NoEmployeeFound() {
        val expected = MessageAPI.createCustomMessageRedirect(
            "No employee was found with an id of 2",
            false,
            ViewTimeAPI.path)

        tru.findEmployeeByIdBehavior = { NO_EMPLOYEE }
        val sd = makeSdForApprove()

        val result = ApproveApi.handlePost(sd)

        assertEquals(expected, result)
    }

    /**
     * If the employee id isn't even a valid integer
     */
    @Test
    fun testApprove_InvalidEmployeeId_EmployeeIdInvalid() {
        val expected = MessageAPI.createCustomMessageRedirect(
            "The employee id was not interpretable as an integer.  You sent \"a\".",
            false,
            ViewTimeAPI.path)

        val sd = makeSdForApprove(employeeId = "a")

        val result = ApproveApi.handlePost(sd)

        assertEquals(expected, result)
    }

    /**
     * If the start date isn't a valid string
     */
    @Test
    fun testApprove_InvalidEmployeeId_StartDateInvalid() {
        val expected = MessageAPI.createCustomMessageRedirect(
            "The date for approving time was not interpreted as a date. You sent \"a\".  Format is YYYY-MM-DD",
            false,
            ViewTimeAPI.path)

        tru.findEmployeeByIdBehavior = { DEFAULT_EMPLOYEE_2 }
        val sd = makeSdForApprove(startDate = "a")

        val result = ApproveApi.handlePost(sd)

        assertEquals(expected, result)
    }
    /**
     * If the employee id is blank...
     */
    @Test
    fun testApprove_InvalidEmployeeId_EmployeeIdBlank() {
        val expected = MessageAPI.createCustomMessageRedirect(
            "The employee id must not be blank",
            false,
            ViewTimeAPI.path)

        val sd = makeSdForApprove(employeeId = "")

        val result = ApproveApi.handlePost(sd)

        assertEquals(expected, result)
    }

    /**
     * If the start date is blank...
     */
    @Test
    fun testApprove_InvalidEmployeeId_StartDateBlank() {
        val expected = MessageAPI.createCustomMessageRedirect(
            "The date for approving time was not interpreted as a date. You sent \"\".  Format is YYYY-MM-DD",
            false,
            ViewTimeAPI.path)

        tru.findEmployeeByIdBehavior = { DEFAULT_EMPLOYEE_2 }
        val sd = makeSdForApprove(startDate = "")

        val result = ApproveApi.handlePost(sd)

        assertEquals(expected, result)
    }

    /**
     * What if we try to approve a timesheet that is not submitted?
     */
    @Test
    fun testApprove_TimesheetIsNotSubmitted() {
        val expected = MessageAPI.createCustomMessageRedirect(
            "Cannot approve a non-submitted timesheet.  EmployeeId: ${DEFAULT_EMPLOYEE_2.id.value}, Timeperiod start: $defaultStartDate",
            false,
            ViewTimeAPI.path)

        tru.findEmployeeByIdBehavior = { DEFAULT_EMPLOYEE_2 }
        tru.approveTimesheetBehavior = { throw IllegalStateException("Cannot approve a non-submitted timesheet.  EmployeeId: ${DEFAULT_EMPLOYEE_2.id.value}, Timeperiod start: $defaultStartDate") }
        val sd = makeSdForApprove()

        val result = ApproveApi.handlePost(sd)

        assertEquals(expected, result)
    }

    /**
     * What if we try to approve our own timesheet?
     */
    @Test
    fun testApprove_TimesheetIsOurOwn() {
        val expected = MessageAPI.createCustomMessageRedirect(
            "Cannot approve your own timesheet",
            false,
            ViewTimeAPI.path)

        tru.findEmployeeByIdBehavior = { DEFAULT_EMPLOYEE }
        tru.approveTimesheetBehavior = { throw IllegalStateException("Cannot approve your own timesheet") }
        val sd = makeSdForApprove()

        val result = ApproveApi.handlePost(sd)

        assertEquals(expected, result)
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

    @Test
    fun testApprove_MissingInput_Employee() {
        val data = PostBodyData(
            mapOf(
                ViewTimeAPI.Elements.TIME_PERIOD.getElemName() to DEFAULT_DATE.stringValue,
                ApproveApi.Elements.IS_UNAPPROVAL.getElemName() to "true"
            )
        )
        val sd = makeSdForApprove(data = data, user = DEFAULT_ADMIN_USER)

        val ex = assertThrows(InexactInputsException::class.java) { ApproveApi.handlePost(sd) }
        assertEquals("expected keys: [approval-employee, date, unappr]. received keys: [date, unappr]", ex.message)
    }

    @Test
    fun testApprove_MissingInput_TimePeriod() {
        val data = PostBodyData(
            mapOf(
                ViewTimeAPI.Elements.EMPLOYEE_TO_APPROVE_INPUT.getElemName() to DEFAULT_EMPLOYEE.id.value.toString(),
                ApproveApi.Elements.IS_UNAPPROVAL.getElemName() to "true"
            )
        )
        val sd = makeSdForApprove(data = data, user = DEFAULT_ADMIN_USER)

        val ex = assertThrows(InexactInputsException::class.java) { ApproveApi.handlePost(sd) }
        assertEquals("expected keys: [approval-employee, date, unappr]. received keys: [approval-employee, unappr]", ex.message)
    }

    @Test
    fun testApprove_MissingInput_IsApproval() {
        val data = PostBodyData(
            mapOf(
                ViewTimeAPI.Elements.EMPLOYEE_TO_APPROVE_INPUT.getElemName() to DEFAULT_EMPLOYEE.id.value.toString(),
                ViewTimeAPI.Elements.TIME_PERIOD.getElemName() to DEFAULT_DATE.stringValue,
            )
        )
        val sd = makeSdForApprove(data = data, user = DEFAULT_ADMIN_USER)

        val ex = assertThrows(InexactInputsException::class.java) { ApproveApi.handlePost(sd) }
        assertEquals("expected keys: [approval-employee, date, unappr]. received keys: [approval-employee, date]", ex.message)
    }


    /**
     * helper method for creating [ServerData] for this set of tests
     */
    private fun makeSdForApprove(
        employeeId: String = DEFAULT_EMPLOYEE_2.id.value.toString(),
        startDate: String = defaultStartDate,
        user: User = DEFAULT_ADMIN_USER,
        unapprove: Boolean = false,
        data: PostBodyData = PostBodyData(
            mapOf(
                ViewTimeAPI.Elements.EMPLOYEE_TO_APPROVE_INPUT.getElemName() to employeeId,
                ViewTimeAPI.Elements.TIME_PERIOD.getElemName() to startDate,
                ApproveApi.Elements.IS_UNAPPROVAL.getElemName() to unapprove.toString()
            )
        )): ServerData {

        return makeServerData(data, tru, au, user = user, path = ApproveApi.path)
    }

}