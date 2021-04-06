package coverosR3z.timerecording.api

import coverosR3z.authentication.types.NO_USER
import coverosR3z.authentication.types.SYSTEM_USER
import coverosR3z.authentication.types.User
import coverosR3z.authentication.utility.FakeAuthenticationUtilities
import coverosR3z.authentication.utility.IAuthenticationUtilities
import coverosR3z.fakeServerObjects
import coverosR3z.misc.*
import coverosR3z.server.APITestCategory
import coverosR3z.server.types.*
import coverosR3z.timerecording.FakeTimeRecordingUtilities
import coverosR3z.timerecording.utility.ITimeRecordingUtilities
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.experimental.categories.Category

@Category(APITestCategory::class)
class ViewTimeAPITests {

    lateinit var au : FakeAuthenticationUtilities
    lateinit var tru : FakeTimeRecordingUtilities

    @Before
    fun init() {
        au = FakeAuthenticationUtilities()
        tru = FakeTimeRecordingUtilities()
    }

    // region role tests


    @Test
    fun testViewingTimeEntries_RegularUser() {
        val sd = makeServerData(PostBodyData(), tru, au, user = DEFAULT_REGULAR_USER)

        val result = ViewTimeAPI.handleGet(sd)

        assertEquals(StatusCode.OK, result.statusCode)
    }

    @Test
    fun testViewingTimeEntries_System() {
        val sd = makeServerData(PostBodyData(), tru, au, user = SYSTEM_USER)

        val result = ViewTimeAPI.handleGet(sd)

        assertEquals(StatusCode.FORBIDDEN, result.statusCode)
    }

    @Test
    fun testViewingTimeEntries_None() {
        val sd = makeServerData(PostBodyData(), tru, au, user = NO_USER)

        val result = ViewTimeAPI.handleGet(sd)

        assertEquals(StatusCode.SEE_OTHER, result.statusCode)
    }

    @Test
    fun testViewingTimeEntries_Admin() {
        val sd = makeServerData(PostBodyData(), tru, au, user = DEFAULT_ADMIN_USER)

        val result = ViewTimeAPI.handleGet(sd)

        assertEquals(StatusCode.OK, result.statusCode)
    }

    @Test
    fun testViewingTimeEntries_Approver() {
        val sd = makeServerData(PostBodyData(), tru, au, user = DEFAULT_APPROVER)

        val result = ViewTimeAPI.handleGet(sd)

        assertEquals(StatusCode.OK, result.statusCode)
    }

    // endregion

    /**
     * If a date is sent in the querystring, it will be used to
     * find the timeperiod we're in and show only those time entries.
     * The date must be in a format of year-month-day, like this: 2021-03-16
     * a typical usage might be like this: https://renomad.com:12443/timeentries?date=2021-03-16
     *
     * See [coverosR3z.timerecording.types.TimePeriodTests.testShouldGetTodayTimePeriod]
     * for the detailed tests for calculating time period from a date
     */
    @Test
    fun testGettingDate() {
        val sd = makeServerDataForGetWithQueryString(queryStringMap = mapOf(ViewTimeAPI.Elements.TIME_PERIOD.getElemName() to "2021-03-16"))

        val result = ViewTimeAPI.handleGet(sd)

        assertTrue(result.fileContentsString()
            .contains("""<div id="timeperiod_display">
                    <div>2021-03-16</div>
                    <div>2021-03-31</div>
                </div>"""))
    }

    /**
     * Similar to [testGettingDate] but one extra day in.. not 2021-03-16, but instead, 2021-03-17
     * I should get the same result - I'm still in the same timeperiod
     */
    @Test
    fun testGettingDateNextDay() {
        val sd = makeServerDataForGetWithQueryString(queryStringMap = mapOf(ViewTimeAPI.Elements.TIME_PERIOD.getElemName() to "2021-03-17"))

        val result = ViewTimeAPI.handleGet(sd)

        assertTrue(result.fileContentsString()
            .contains("""<div id="timeperiod_display">
                    <div>2021-03-16</div>
                    <div>2021-03-31</div>
                </div>"""))
    }

    /**
     * What if the date value is invalid? there is no day "1a"
     */
    @Test
    fun testDateInvalid_letters() {
        val sd = makeServerDataForGetWithQueryString(queryStringMap = mapOf(ViewTimeAPI.Elements.TIME_PERIOD.getElemName() to "2021-03-1a"))

        val ex = assertThrows(IllegalArgumentException::class.java) {ViewTimeAPI.handleGet(sd)}

        assertEquals("Must be able to parse 1a as integer", ex.message)
    }

    /**
     * What if the date value is invalid? like the year 1979
     */
    @Test
    fun testDateInvalid_tooAncient() {
        val sd = makeServerDataForGetWithQueryString(queryStringMap = mapOf(ViewTimeAPI.Elements.TIME_PERIOD.getElemName() to "1979-12-31"))

        val ex = assertThrows(IllegalArgumentException::class.java) {ViewTimeAPI.handleGet(sd)}

        assertEquals("no way on earth people are using this before 1980-01-01 or past 2200-01-01, you had a date of 1979-12-31", ex.message)
    }

    /**
     * What if the date value is invalid? like the year 2200
     */
    @Test
    fun testDateInvalid_tooFuture() {
        val sd = makeServerDataForGetWithQueryString(queryStringMap = mapOf(ViewTimeAPI.Elements.TIME_PERIOD.getElemName() to "2200-01-02"))

        val ex = assertThrows(IllegalArgumentException::class.java) {ViewTimeAPI.handleGet(sd)}

        assertEquals("no way on earth people are using this before 1980-01-01 or past 2200-01-01, you had a date of 2200-01-02", ex.message)
    }

    /**
     * What if the date value is invalid? like the month 13
     */
    @Test
    fun testDateInvalid_invalidMonth() {
        val sd = makeServerDataForGetWithQueryString(queryStringMap = mapOf(ViewTimeAPI.Elements.TIME_PERIOD.getElemName() to "2021-13-01"))

        val ex = assertThrows(IllegalStateException::class.java) {ViewTimeAPI.handleGet(sd)}

        assertEquals("Month must comply with arbitrary human divisions of time into 12 similarly sized chunks a year. You tried 13.", ex.message)
    }

    /**
     * What if the date value is invalid? like the month 0
     */
    @Test
    fun testDateInvalid_invalidMonthZero() {
        val sd = makeServerDataForGetWithQueryString(queryStringMap = mapOf(ViewTimeAPI.Elements.TIME_PERIOD.getElemName() to "2021-00-01"))

        val ex = assertThrows(IllegalStateException::class.java) {ViewTimeAPI.handleGet(sd)}

        assertEquals("Month must comply with arbitrary human divisions of time into 12 similarly sized chunks a year. You tried 0.", ex.message)
    }

    /**
     * What if the date value is invalid? like a blank date
     */
    @Test
    fun testDateInvalid_blank() {
        val sd = makeServerDataForGetWithQueryString(queryStringMap = mapOf(ViewTimeAPI.Elements.TIME_PERIOD.getElemName() to ""))

        val ex = assertThrows(IllegalArgumentException::class.java) {ViewTimeAPI.handleGet(sd)}

        assertEquals("date must not be blank", ex.message)
    }

    /**
     * On the time entry page, it is possible to request a particular employee's time
     * entries by including a query string similar to this: emp=2
     *
     * We then simply show that employee's time entries.  Note that this is contingent
     * on things like: the current user has a role that allows seeing other people's timesheets,
     * like an admin or approver.  Further, if an approver, it has to be one of their reports.
     */
    @Test
    fun testEmployeeRequested() {
        tru.findEmployeeByIdBehavior = { DEFAULT_EMPLOYEE }
        tru.getTimeEntriesForTimePeriodBehavior = { setOf(DEFAULT_TIME_ENTRY) }
        val sd = makeServerDataForGetWithQueryString(
            queryStringMap = mapOf(
                ViewTimeAPI.Elements.REQUESTED_EMPLOYEE.getElemName() to "2"))

        val result = ViewTimeAPI.handleGet(sd).fileContentsString()

        assertTrue("The page should announce we are viewing another's timesheet",
            result.contains("<h2>Viewing DefaultEmployee's timesheet</h2>"))
        assertTrue(result.contains("<title>Viewing DefaultEmployee's timesheet</title>"))
        assertFalse(result.contains(ViewTimeAPI.Elements.SUBMIT_BUTTON.getId()))
        assertFalse(result.contains(ViewTimeAPI.Elements.CREATE_BUTTON.getId()))
        assertFalse(result.contains(ViewTimeAPI.Elements.CREATE_BUTTON_MOBILE.getId()))
        assertFalse(result.contains(ViewTimeAPI.Elements.EDIT_BUTTON.getElemClass()))
    }

    /**
     * If you are viewing someone else's timesheet, you aren't allowed to edit anything,
     * so if you were to send the edit id in the query string, it would just complain
     */
    @Test
    fun testEmployeeRequested_NoEditAllowed() {
        tru.findEmployeeByIdBehavior = { DEFAULT_EMPLOYEE }
        val sd = makeServerDataForGetWithQueryString(
            queryStringMap = mapOf(
                ViewTimeAPI.Elements.REQUESTED_EMPLOYEE.getElemName() to "2",
                ViewTimeAPI.Elements.EDIT_ID.getElemName() to "1"))

        val ex = assertThrows(IllegalStateException::class.java) {ViewTimeAPI.handleGet(sd)}

        assertEquals("If you are viewing someone else's timesheet, " +
                "you aren't allowed to edit any fields.  " +
                "The ${ViewTimeAPI.Elements.EDIT_ID.getElemName()} key in the query string is not allowed.", ex.message)
    }

    /**
     * What if we request to see a different employee's time
     * sheet, on a different date than the current period?
     */
    @Test
    fun testEmployeeRequested_DifferentDate() {
        tru.findEmployeeByIdBehavior = { DEFAULT_EMPLOYEE }
        val sd = makeServerDataForGetWithQueryString(
            queryStringMap = mapOf(
                ViewTimeAPI.Elements.REQUESTED_EMPLOYEE.getElemName() to "2",
                ViewTimeAPI.Elements.TIME_PERIOD.getElemName() to "2021-01-11"))

        val result = ViewTimeAPI.handleGet(sd)

        assertTrue(result.fileContentsString()
            .contains("""<div id="timeperiod_display">
                    <div>2021-01-01</div>
                    <div>2021-01-15</div>
                </div>"""))
        assertTrue("The page should announce we are viewing another's timesheet",
            result.fileContentsString().contains("Viewing DefaultEmployee's timesheet"))
    }

    /**
     * What happens if we enter our own employee id?  It should complain. It's silly, so we won't allow it.
     * if we want to see our own, the key to see another person's timesheet shouldn't
     * be in the query string.
     */
    @Test
    fun testEmployeeRequested_Invalid_OurOwnId() {
        // we assume the current user's own employee id is 1
        val currentEmpId = 1
        val sd = makeServerDataForGetWithQueryString(
            queryStringMap = mapOf(
                ViewTimeAPI.Elements.REQUESTED_EMPLOYEE.getElemName() to "$currentEmpId"))

        val ex = assertThrows(IllegalStateException::class.java) {ViewTimeAPI.handleGet(sd)}

        assertEquals("Error: makes no sense to request your own timesheet (employee id in query string was your own)", ex.message)
    }

    /**
     * If we use an id that doesn't tie to any employee, throw an exception
     */
    @Test
    fun testEmployeeRequested_Invalid_NoEmployeeWithThisId() {
        val sd = makeServerDataForGetWithQueryString(
            queryStringMap = mapOf(
                ViewTimeAPI.Elements.REQUESTED_EMPLOYEE.getElemName() to "123"))

        val ex = assertThrows(IllegalStateException::class.java) {ViewTimeAPI.handleGet(sd)}

        assertEquals("Error: employee id in query string (123) does not find any employee", ex.message)
    }

    /**
     * If we use an id that doesn't parse as an integer, throw an exception
     */
    @Test
    fun testEmployeeRequested_Invalid_NonInteger() {
        val sd = makeServerDataForGetWithQueryString(
            queryStringMap = mapOf(
                ViewTimeAPI.Elements.REQUESTED_EMPLOYEE.getElemName() to "abc"))

        val ex = assertThrows(IllegalArgumentException::class.java) {ViewTimeAPI.handleGet(sd)}

        assertEquals("Must be able to parse abc as integer", ex.message)
    }

    /**
     * If we use an employee id that is blank, throw exception
     */
    @Test
    fun testEmployeeRequested_Invalid_Blank() {
        val sd = makeServerDataForGetWithQueryString(
            queryStringMap = mapOf(
                ViewTimeAPI.Elements.REQUESTED_EMPLOYEE.getElemName() to ""))

        val ex = assertThrows(IllegalArgumentException::class.java) {ViewTimeAPI.handleGet(sd)}

        assertEquals("The employee id must not be blank", ex.message)
    }

    /**
     * If we pass in an employee id but we are a Role.REGULAR, throw exception
     */
    @Test
    fun testEmployeeRequested_Invalid_Role() {
        val sd = makeServerDataForGetWithQueryString(
            queryStringMap = mapOf(ViewTimeAPI.Elements.REQUESTED_EMPLOYEE.getElemName() to "1"),
            user = DEFAULT_REGULAR_USER)

        val result = ViewTimeAPI.handleGet(sd).fileContentsString()

        assertTrue(result.contains("Your role does not allow viewing other employee's timesheets.  Your " +
                "URL had a query string requesting to see a particular employee, using the " +
                "key ${ViewTimeAPI.Elements.REQUESTED_EMPLOYEE.getElemName()}"))
    }

    /**
     * If the user provides an id to edit through a key of "editid", we will
     * render HTML to allow editing that time entry
     */
    @Test
    fun testEditId() {
        tru.getTimeEntriesForTimePeriodBehavior = { setOf(DEFAULT_TIME_ENTRY) }
        val sd = makeServerDataForGetWithQueryString(queryStringMap = mapOf(ViewTimeAPI.Elements.EDIT_ID.getElemName() to "1"))

        val result = ViewTimeAPI.handleGet(sd)

        assertTrue("The page should render HTML for editing a time entry",
            result.fileContentsString().contains("""<div class="editable-time-entry-row" id="time-entry-1">"""))
    }


    /**
     * Builds a prefabricated object for a GET query,
     * where the user must set the value of the query string
     */
    private fun makeServerDataForGetWithQueryString(
        queryStringMap: Map<String, String> = mapOf(),
        data: PostBodyData = PostBodyData(),
        tru: ITimeRecordingUtilities = this.tru,
        au: IAuthenticationUtilities = this.au,
        authStatus: AuthStatus = AuthStatus.AUTHENTICATED,
        user: User = DEFAULT_ADMIN_USER): ServerData {
        return ServerData(
            BusinessCode(tru, au),
            fakeServerObjects,
            AnalyzedHttpData(data = data, user = user, queryString = queryStringMap),
            authStatus = authStatus,
            testLogger
        )
    }
}