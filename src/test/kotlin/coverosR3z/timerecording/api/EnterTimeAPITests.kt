package coverosR3z.timerecording.api

import coverosR3z.authentication.persistence.AuthenticationPersistence
import coverosR3z.authentication.types.CurrentUser
import coverosR3z.authentication.types.NO_USER
import coverosR3z.authentication.types.SYSTEM_USER
import coverosR3z.authentication.types.User
import coverosR3z.authentication.utility.AuthenticationUtilities
import coverosR3z.authentication.utility.FakeAuthenticationUtilities
import coverosR3z.fakeServerObjects
import coverosR3z.persistence.utility.PureMemoryDatabase.Companion.createEmptyDatabase
import coverosR3z.server.APITestCategory
import coverosR3z.server.ServerPerformanceTests
import coverosR3z.server.types.*
import coverosR3z.system.misc.*
import coverosR3z.system.misc.exceptions.InexactInputsException
import coverosR3z.system.misc.types.Date
import coverosR3z.system.misc.utility.getTime
import coverosR3z.timerecording.FakeTimeRecordingUtilities
import coverosR3z.timerecording.persistence.TimeEntryPersistence
import coverosR3z.timerecording.types.Employee
import coverosR3z.timerecording.types.NO_PROJECT
import coverosR3z.timerecording.types.Project
import coverosR3z.timerecording.utility.TimeRecordingUtilities
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.experimental.categories.Category
import java.io.File

class EnterTimeAPITests {

    lateinit var au : FakeAuthenticationUtilities
    lateinit var tru : FakeTimeRecordingUtilities

    @Before
    fun init() {
        au = FakeAuthenticationUtilities()
        tru = FakeTimeRecordingUtilities()
        tru.findProjectByNameBehavior = { DEFAULT_PROJECT }
    }

    /**
     * If we pass in valid information, it should indicate success
     */
    @Category(APITestCategory::class)
    @Test
    fun testEnterTimeAPI() {
        val data = PostBodyData(mapOf(
                ViewTimeAPI.Elements.PROJECT_INPUT.getElemName() to "default project",
                ViewTimeAPI.Elements.TIME_INPUT.getElemName() to "1",
                ViewTimeAPI.Elements.DETAIL_INPUT.getElemName() to "not much to say",
                ViewTimeAPI.Elements.DATE_INPUT.getElemName() to DEFAULT_DATE_STRING
        ))
        val sd = makeETServerData(data)
        val result = EnterTimeAPI.handlePost(sd)

        assertSuccessfulTimeEntry(result)
    }

    /**
     * If we are missing required data
     */
    @Category(APITestCategory::class)
    @Test
    fun testEnterTimeAPI_missingProject() {
        val data = PostBodyData(mapOf(
                ViewTimeAPI.Elements.TIME_INPUT.getElemName() to "60",
                ViewTimeAPI.Elements.DETAIL_INPUT.getElemName() to "not much to say",
                ViewTimeAPI.Elements.DATE_INPUT.getElemName() to DEFAULT_DATE_STRING
        ))
        val sd = makeETServerData(data)
        val ex = assertThrows(InexactInputsException::class.java){ EnterTimeAPI.handlePost(sd) }
        assertEquals("expected keys: [project_entry, time_entry, detail_entry, date_entry]. received keys: [time_entry, detail_entry, date_entry]", ex.message)
    }

    /**
     * If we are missing required data
     */
    @Category(APITestCategory::class)
    @Test
    fun testEnterTimeAPI_missingTimeEntry() {
        val data = PostBodyData(mapOf(
                ViewTimeAPI.Elements.PROJECT_INPUT.getElemName() to "default project",
                ViewTimeAPI.Elements.DETAIL_INPUT.getElemName() to "not much to say",
                ViewTimeAPI.Elements.DATE_INPUT.getElemName() to DEFAULT_DATE_STRING
        ))
        val sd = makeETServerData(data)
        val ex = assertThrows(InexactInputsException::class.java){ EnterTimeAPI.handlePost(sd) }
        assertEquals("expected keys: [project_entry, time_entry, detail_entry, date_entry]. received keys: [project_entry, detail_entry, date_entry]", ex.message)
    }

    /**
     * If we are missing required data
     */
    @Category(APITestCategory::class)
    @Test
    fun testEnterTimeAPI_missingDetailEntry() {
        val data = PostBodyData(mapOf(
                ViewTimeAPI.Elements.PROJECT_INPUT.getElemName() to "default project",
                ViewTimeAPI.Elements.TIME_INPUT.getElemName() to "60",
                ViewTimeAPI.Elements.DATE_INPUT.getElemName() to DEFAULT_DATE_STRING,
        ))
        val sd = makeETServerData(data)
        val ex = assertThrows(InexactInputsException::class.java){ EnterTimeAPI.handlePost(sd) }
        assertEquals("expected keys: [project_entry, time_entry, detail_entry, date_entry]. received keys: [project_entry, time_entry, date_entry]", ex.message)
    }

    /**
     * If we are missing required data
     */
    @Category(APITestCategory::class)
    @Test
    fun testEnterTimeAPI_missingDateEntry() {
        val data = PostBodyData(mapOf(
            ViewTimeAPI.Elements.PROJECT_INPUT.getElemName() to "default project",
            ViewTimeAPI.Elements.TIME_INPUT.getElemName() to "1",
            ViewTimeAPI.Elements.DETAIL_INPUT.getElemName() to "not much to say",
        ))
        val sd = makeETServerData(data)
        val ex = assertThrows(InexactInputsException::class.java){ EnterTimeAPI.handlePost(sd) }
        assertEquals("expected keys: [project_entry, time_entry, detail_entry, date_entry]. received keys: [project_entry, time_entry, detail_entry]", ex.message)
    }

    /**
     * If we pass in an empty string for project
     */
    @Category(APITestCategory::class)
    @Test
    fun testEnterTimeAPI_emptyStringProject() {
        val data = PostBodyData(mapOf(
            ViewTimeAPI.Elements.PROJECT_INPUT.getElemName() to "",
            ViewTimeAPI.Elements.TIME_INPUT.getElemName() to "60",
            ViewTimeAPI.Elements.DETAIL_INPUT.getElemName() to "not much to say",
            ViewTimeAPI.Elements.DATE_INPUT.getElemName() to A_RANDOM_DAY_IN_JUNE_2020.epochDay.toString()))
        val sd = makeETServerData(data)

        val result = EnterTimeAPI.handlePost(sd)

        assertTrue(
            result.headers.joinToString(";"),
            result.headers.contains("Location: result?rtn=timeentries&suc=false&custommsg=Makes+no+sense+to+have+an+empty+project+name")
        )
    }

    /**
     * If we pass in all spaces as the project
     */
    @Category(APITestCategory::class)
    @Test
    fun testEnterTimeAPI_allSpacesProject() {
        val data = PostBodyData(mapOf(
            ViewTimeAPI.Elements.PROJECT_INPUT.getElemName() to "   ",
            ViewTimeAPI.Elements.TIME_INPUT.getElemName() to "60",
            ViewTimeAPI.Elements.DETAIL_INPUT.getElemName() to "not much to say",
            ViewTimeAPI.Elements.DATE_INPUT.getElemName() to A_RANDOM_DAY_IN_JUNE_2020.stringValue))
        val sd = makeETServerData(data)

        val result = EnterTimeAPI.handlePost(sd)

        assertTrue(
            result.headers.joinToString(";"),
            result.headers.contains("Location: result?rtn=timeentries&suc=false&custommsg=Makes+no+sense+to+have+an+empty+project+name")
        )
    }

    /**
     * If the project passed in isn't recognized
     */
    @Category(APITestCategory::class)
    @Test
    fun testEnterTimeAPI_unrecognizedProject() {
        tru.findProjectByNameBehavior = { NO_PROJECT }
        val data = PostBodyData(mapOf(
            ViewTimeAPI.Elements.PROJECT_INPUT.getElemName() to "UNRECOGNIZED",
            ViewTimeAPI.Elements.TIME_INPUT.getElemName() to "60",
            ViewTimeAPI.Elements.DETAIL_INPUT.getElemName() to "not much to say",
            ViewTimeAPI.Elements.DATE_INPUT.getElemName() to A_RANDOM_DAY_IN_JUNE_2020.stringValue))
        val sd = makeETServerData(data)
        val result = EnterTimeAPI.handlePost(sd)
        assertEquals(StatusCode.SEE_OTHER, result.statusCode)
        assertTrue(result.headers.joinToString(";"), result.headers.contains("Location: result?msg=INVALID_PROJECT_DURING_ENTERING_TIME"))
    }

    /**
     * If the time entered is negative
     */
    @Category(APITestCategory::class)
    @Test
    fun testEnterTimeAPI_negativeTime() {
        val data = PostBodyData(mapOf(
            ViewTimeAPI.Elements.PROJECT_INPUT.getElemName() to "default project",
            ViewTimeAPI.Elements.TIME_INPUT.getElemName() to "-1",
            ViewTimeAPI.Elements.DETAIL_INPUT.getElemName() to "not much to say",
            ViewTimeAPI.Elements.DATE_INPUT.getElemName() to A_RANDOM_DAY_IN_JUNE_2020.stringValue))
        val sd = makeETServerData(data)

        val result = EnterTimeAPI.handlePost(sd)

        assertTrue(
            result.headers.joinToString(";"),
            result.headers.contains("Location: result?rtn=timeentries&suc=false&custommsg=Doesn%27t+make+sense+to+have+negative+time.+time+in+minutes%3A+-60")
        )
    }

    /**
     * You can only enter 24 hours in a day
     */
    @Category(APITestCategory::class)
    @Test
    fun testEnterTimeAPI_greaterThanTwentyFour() {
        val data = PostBodyData(mapOf(
            ViewTimeAPI.Elements.PROJECT_INPUT.getElemName() to "default project",
            ViewTimeAPI.Elements.TIME_INPUT.getElemName() to "24.25",
            ViewTimeAPI.Elements.DETAIL_INPUT.getElemName() to "not much to say",
            ViewTimeAPI.Elements.DATE_INPUT.getElemName() to A_RANDOM_DAY_IN_JUNE_2020.stringValue))
        val sd = makeETServerData(data)

        val result = EnterTimeAPI.handlePost(sd)

        assertEquals(StatusCode.SEE_OTHER, result.statusCode)
        assertTrue(result.headers.joinToString(";"), result.headers.contains("Location: result?msg=TIME_MUST_BE_LESS_OR_EQUAL_TO_24"))
    }

    /**
     * If the time entered is zero, it's fine.
     */
    @Category(APITestCategory::class)
    @Test
    fun testEnterTimeAPI_zeroTime() {
        val data = PostBodyData(mapOf(
                ViewTimeAPI.Elements.PROJECT_INPUT.getElemName() to "default project",
                ViewTimeAPI.Elements.TIME_INPUT.getElemName() to "0",
                ViewTimeAPI.Elements.DETAIL_INPUT.getElemName() to "not much to say",
                ViewTimeAPI.Elements.DATE_INPUT.getElemName() to DEFAULT_DATE_STRING
        ))
        val sd = makeETServerData(data)
        val result = EnterTimeAPI.handlePost(sd)

        assertSuccessfulTimeEntry(result)
    }

    /**
     * If the time entered is non-numeric, like "a"
     */
    @Category(APITestCategory::class)
    @Test
    fun testEnterTimeAPI_nonNumericTime() {
        val data = PostBodyData(mapOf(
                ViewTimeAPI.Elements.PROJECT_INPUT.getElemName() to "default project",
                ViewTimeAPI.Elements.TIME_INPUT.getElemName() to "aaa",
                ViewTimeAPI.Elements.DETAIL_INPUT.getElemName() to "not much to say",
                ViewTimeAPI.Elements.DATE_INPUT.getElemName() to DEFAULT_DATE_STRING
        ))
        val sd = makeETServerData(data)

        val result = EnterTimeAPI.handlePost(sd)

        assertTrue(
            result.headers.joinToString(";"),
            result.headers.contains("Location: result?rtn=timeentries&suc=false&custommsg=Must+be+able+to+parse+%22aaa%22+as+a+double")
        )
    }

    /**
     * The time must be a valid multiple of 0.25
     */
    @Category(APITestCategory::class)
    @Test
    fun testEnterTimeAPI_timeNotOnValidMultiple() {
        val data = PostBodyData(mapOf(
                ViewTimeAPI.Elements.PROJECT_INPUT.getElemName() to "default project",
                ViewTimeAPI.Elements.TIME_INPUT.getElemName() to "1.23",
                ViewTimeAPI.Elements.DETAIL_INPUT.getElemName() to "not much to say",
                ViewTimeAPI.Elements.DATE_INPUT.getElemName() to DEFAULT_DATE_STRING
        ))
        val sd = makeETServerData(data)

        val result = EnterTimeAPI.handlePost(sd)

        assertEquals(StatusCode.SEE_OTHER, result.statusCode)
        assertTrue(result.headers.joinToString(";"), result.headers.contains("Location: result?msg=MINUTES_MUST_BE_MULTIPLE_OF_15"))
    }

    /**
     * If a time period (future or past, doesn't matter) has been submitted, it isn't possible to
     * create a new time entry for it.
     */
    @Category(APITestCategory::class)
    @Test
    fun testEnterTime_DateInvalid_DateEntryDisallowedForSubmittedTime() {
        tru.isInASubmittedPeriodBehavior = { true }
        val data = PostBodyData(mapOf(
            ViewTimeAPI.Elements.PROJECT_INPUT.getElemName() to "default project",
            ViewTimeAPI.Elements.TIME_INPUT.getElemName() to "1",
            ViewTimeAPI.Elements.DETAIL_INPUT.getElemName() to "not much to say",
            ViewTimeAPI.Elements.DATE_INPUT.getElemName() to DEFAULT_DATE_STRING
        ))
        val sd = makeETServerData(data)

        val result = EnterTimeAPI.handlePost(sd)

        assertEquals(StatusCode.SEE_OTHER, result.statusCode)
        assertTrue(result.headers.joinToString(";"), result.headers.contains("Location: result?msg=NO_TIME_ENTRY_ALLOWED_IN_SUBMITTED_PERIOD"))
    }

    @Category(APITestCategory::class)
    @Test
    fun testEnterTimeAPI_EmptyDateString() {
        val data = PostBodyData(mapOf(
            ViewTimeAPI.Elements.PROJECT_INPUT.getElemName() to "default project",
            ViewTimeAPI.Elements.TIME_INPUT.getElemName() to "1",
            ViewTimeAPI.Elements.DETAIL_INPUT.getElemName() to "not much to say",
            ViewTimeAPI.Elements.DATE_INPUT.getElemName() to ""
        ))
        val sd = makeETServerData(data)

        val result = EnterTimeAPI.handlePost(sd)

        assertTrue(
            result.headers.joinToString(";"),
            result.headers.contains("Location: result?rtn=timeentries&suc=false&custommsg=date+must+not+be+blank")
        )
    }

    /**
     * If the date is before [coverosR3z.system.misc.types.earliestAllowableDate]
     */
    @Category(APITestCategory::class)
    @Test
    fun testEnterTimeAPI_TooEarlyDateString() {
        val data = PostBodyData(mapOf(
            ViewTimeAPI.Elements.PROJECT_INPUT.getElemName() to "default project",
            ViewTimeAPI.Elements.TIME_INPUT.getElemName() to "1",
            ViewTimeAPI.Elements.DETAIL_INPUT.getElemName() to "not much to say",
            ViewTimeAPI.Elements.DATE_INPUT.getElemName() to "1979-12-31"
        ))
        val sd = makeETServerData(data)
        val result = EnterTimeAPI.handlePost(sd)

        assertTrue(
            result.headers.joinToString(";"),
            result.headers.contains("Location: result?rtn=timeentries&suc=false&custommsg=no+way+on+earth+people+are+using+this+before+1980-01-01+or+past+2200-01-01%2C+you+had+a+date+of+1979-12-31")
        )
    }

    /**
     * If the date is after [coverosR3z.system.misc.types.latestAllowableDate]
     */
    @Category(APITestCategory::class)
    @Test
    fun testEnterTimeAPI_TooLateDateString() {
        val data = PostBodyData(mapOf(
            ViewTimeAPI.Elements.PROJECT_INPUT.getElemName() to "default project",
            ViewTimeAPI.Elements.TIME_INPUT.getElemName() to "1",
            ViewTimeAPI.Elements.DETAIL_INPUT.getElemName() to "not much to say",
            ViewTimeAPI.Elements.DATE_INPUT.getElemName() to "2200-01-02"
        ))
        val sd = makeETServerData(data)

        val result = EnterTimeAPI.handlePost(sd)

        assertTrue(
            result.headers.joinToString(";"),
            result.headers.contains("Location: result?rtn=timeentries&suc=false&custommsg=no+way+on+earth+people+are+using+this+before+1980-01-01+or+past+2200-01-01%2C+you+had+a+date+of+2200-01-02")
        )
    }

    /**
     * Just how quickly does it go, from this level?
     *
     * With 1000 requests, it takes .180 seconds = 5,555 requests per second.
     *
     * See [ServerPerformanceTests.testEnterTimeReal_PERFORMANCE]
     */
    @Category(PerformanceTestCategory::class)
    @Test
    fun testEnterTimeAPI_PERFORMANCE() {
        val numberOfRequests = 200

        testLogger.turnOffAllLogging()
        // set up real database
        val pmd = createEmptyDatabase()
        val tep  = TimeEntryPersistence(pmd, logger = testLogger)
        val au = AuthenticationUtilities(
            AuthenticationPersistence(pmd, logger = testLogger),
            testLogger,
        )
        val employee : Employee = tep.persistNewEmployee(DEFAULT_EMPLOYEE_NAME)
        val user = au.registerWithEmployee(DEFAULT_USER.name, DEFAULT_PASSWORD,employee).user
        val tru = TimeRecordingUtilities(tep, CurrentUser(user), testLogger)
        val project : Project = tep.persistNewProject(DEFAULT_PROJECT_NAME)

        val (time, _) = getTime {
            for (i in 1..numberOfRequests) {

                val data = PostBodyData(mapOf(
                    ViewTimeAPI.Elements.PROJECT_INPUT.getElemName() to project.name.value,
                    ViewTimeAPI.Elements.TIME_INPUT.getElemName() to "1",
                    ViewTimeAPI.Elements.DETAIL_INPUT.getElemName() to "not much to say",
                    ViewTimeAPI.Elements.DATE_INPUT.getElemName() to Date(A_RANDOM_DAY_IN_JUNE_2020.epochDay + (i / 20)).stringValue
                ))

                val sd = ServerData(
                    BusinessCode(tru, au),
                    fakeServerObjects,
                    AnalyzedHttpData(data = data, user = user), authStatus = AuthStatus.AUTHENTICATED, testLogger)
                val result = EnterTimeAPI.handlePost(sd)

                assertSuccessfulTimeEntry(result)

            }
        }
        testLogger.resetLogSettingsToDefault()
        println(time)
        File("${granularPerfArchiveDirectory}testEnterTimeAPI_PERFORMANCE")
            .appendText("${Date.now().stringValue}\trequests: $numberOfRequests\ttime: $time milliseconds\n")
    }

    // region ROLE TESTS

    // POST tests


    @Category(APITestCategory::class)
    @Test
    fun testShouldAllowAdminToEnterTimePOST() {
        val sd = makeETServerData(happyPathData, user = DEFAULT_ADMIN_USER)
        val result = EnterTimeAPI.handlePost(sd).statusCode
        assertEquals(StatusCode.SEE_OTHER, result)
    }

    @Category(APITestCategory::class)
    @Test
    fun testShouldAllowApproverToEnterTimePOST() {
        val sd = makeETServerData(happyPathData, user = DEFAULT_APPROVER)
        val result = EnterTimeAPI.handlePost(sd).statusCode
        assertEquals(StatusCode.SEE_OTHER, result)
    }

    @Category(APITestCategory::class)
    @Test
    fun testShouldAllowRegularToEnterTimePOST() {
        val sd = makeETServerData(happyPathData, user = DEFAULT_REGULAR_USER)
        val result = EnterTimeAPI.handlePost(sd).statusCode
        assertEquals(StatusCode.SEE_OTHER, result)
    }

    @Category(APITestCategory::class)
    @Test
    fun testShouldDisallowSystemToEnterTimePOST() {
        val sd = makeETServerData(happyPathData, user = SYSTEM_USER)

        val result = EnterTimeAPI.handlePost(sd).statusCode

        assertEquals(StatusCode.FORBIDDEN, result)
    }

    @Category(APITestCategory::class)
    @Test
    fun testShouldDisallowNoUserToEnterTimePOST() {
        val sd = makeETServerData(PostBodyData(), user = NO_USER)
        assertEquals(StatusCode.UNAUTHORIZED, EnterTimeAPI.handlePost(sd).statusCode)
    }

    @Test
    fun testEdit() {
        tru.findEmployeeByIdBehavior = { DEFAULT_REGULAR_USER.employee }
        val data = PostBodyData(mapOf(
            ViewTimeAPI.Elements.PROJECT_INPUT.getElemName() to DEFAULT_PROJECT.name.value,
            ViewTimeAPI.Elements.TIME_INPUT.getElemName() to "1",
            ViewTimeAPI.Elements.DETAIL_INPUT.getElemName() to "not much to say",
            ViewTimeAPI.Elements.DATE_INPUT.getElemName() to DEFAULT_DATE_STRING,
            ViewTimeAPI.Elements.ID_INPUT.getElemName() to "1",
            ViewTimeAPI.Elements.BEING_EDITED.getElemName() to "true",
        ))
        val sd = makeETServerData(data)
        val response = EnterTimeAPI.handlePost(sd).statusCode
        assertEquals(
            "We should have gotten redirected to the viewTime page",
            StatusCode.SEE_OTHER, response
        )
    }

    /**
     * If we are editing time, and missing an id, should throw inexact inputs
     */
    @Category(APITestCategory::class)
    @Test
    fun testEdit_Negative_MissingId() {
        val data = PostBodyData(mapOf(
            ViewTimeAPI.Elements.PROJECT_INPUT.getElemName() to DEFAULT_PROJECT.name.value,
            ViewTimeAPI.Elements.TIME_INPUT.getElemName() to "1",
            ViewTimeAPI.Elements.DETAIL_INPUT.getElemName() to "not much to say",
            ViewTimeAPI.Elements.DATE_INPUT.getElemName() to DEFAULT_DATE_STRING,
            ViewTimeAPI.Elements.BEING_EDITED.getElemName() to "true",
        ))
        val sd = makeETServerData(data)

        val result = EnterTimeAPI.handlePost(sd)

        assertTrue(
            result.headers.joinToString(";"),
            result.headers.contains("Location: result?rtn=timeentries&suc=false&custommsg=Integer+must+not+be+a+null+value")
        )
    }


    /**
     * If a time period (future or past, doesn't matter) has been submitted, it isn't possible to
     * create a new time entry for it.
     */
    @Test
    fun testEdit_DateInvalid_DateEntryDisallowedForSubmittedTime() {
        tru.isInASubmittedPeriodBehavior = { true }
        val data = PostBodyData(mapOf(
            ViewTimeAPI.Elements.PROJECT_INPUT.getElemName() to DEFAULT_PROJECT.name.value,
            ViewTimeAPI.Elements.TIME_INPUT.getElemName() to "1",
            ViewTimeAPI.Elements.DETAIL_INPUT.getElemName() to "not much to say",
            ViewTimeAPI.Elements.DATE_INPUT.getElemName() to DEFAULT_DATE_STRING,
            ViewTimeAPI.Elements.ID_INPUT.getElemName() to "1"
        ))
        val sd = makeETServerData(data)

        val result = EnterTimeAPI.handlePost(sd)

        assertTrue(
            result.headers.joinToString(";"),
            result.headers.contains("Location: result?msg=NO_TIME_ENTRY_ALLOWED_IN_SUBMITTED_PERIOD")
        )
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
     * this should confirm what happens when a user successfully enters their time
     */
    private fun assertSuccessfulTimeEntry(result: PreparedResponseData) {
        assertEquals(StatusCode.SEE_OTHER, result.statusCode)
        assertTrue(result.headers.any { it.matches(redirectRegex)})
    }

    companion object {
        val redirectRegex = """Location: timeentries\?date=....-..-..""".toRegex()

        val happyPathData = PostBodyData(mapOf(
            ViewTimeAPI.Elements.PROJECT_INPUT.getElemName() to DEFAULT_PROJECT.name.value,
            ViewTimeAPI.Elements.TIME_INPUT.getElemName() to "1",
            ViewTimeAPI.Elements.DETAIL_INPUT.getElemName() to "not much to say",
            ViewTimeAPI.Elements.DATE_INPUT.getElemName() to DEFAULT_DATE_STRING
        ))
    }

    /**
     * Helper method for the kinds of [ServerData] we will
     * ordinarily see in entering time.
     */
    private fun makeETServerData(data: PostBodyData, user: User = DEFAULT_REGULAR_USER): ServerData {
        return makeServerData(data, tru, au, user = user, path = EnterTimeAPI.path)
    }

}
