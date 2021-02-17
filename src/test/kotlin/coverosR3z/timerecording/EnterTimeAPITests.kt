package coverosR3z.timerecording

import coverosR3z.authentication.FakeAuthenticationUtilities
import coverosR3z.authentication.persistence.AuthenticationPersistence
import coverosR3z.authentication.types.CurrentUser
import coverosR3z.authentication.utility.AuthenticationUtilities
import coverosR3z.misc.*
import coverosR3z.misc.exceptions.InexactInputsException
import coverosR3z.misc.types.Date
import coverosR3z.misc.utility.getTime
import coverosR3z.persistence.utility.PureMemoryDatabase
import coverosR3z.server.ServerPerformanceTests
import coverosR3z.server.types.*
import coverosR3z.timerecording.api.EnterTimeAPI
import coverosR3z.timerecording.persistence.TimeEntryPersistence
import coverosR3z.timerecording.types.*
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
    }

    /**
     * If we pass in valid information, it should indicate success
     */
    @Test
    fun testEnterTimeAPI() {
        val data = PostBodyData(mapOf(
                EnterTimeAPI.Elements.PROJECT_INPUT.getElemName() to "1",
                EnterTimeAPI.Elements.TIME_INPUT.getElemName() to "1",
                EnterTimeAPI.Elements.DETAIL_INPUT.getElemName() to "not much to say",
                EnterTimeAPI.Elements.DATE_INPUT.getElemName() to DEFAULT_DATE_STRING
        ))
        val sd = ServerData(au, tru, AnalyzedHttpData(data = data), authStatus = AuthStatus.AUTHENTICATED, testLogger)
        val result = EnterTimeAPI.handlePost(sd)

        assertSuccessfulTimeEntry(result)
    }


    /**
     * If we are missing required data
     */
    @Test
    fun testEnterTimeAPI_missingProject() {
        val data = PostBodyData(mapOf(
                EnterTimeAPI.Elements.TIME_INPUT.getElemName() to "60",
                EnterTimeAPI.Elements.DETAIL_INPUT.getElemName() to "not much to say",
                EnterTimeAPI.Elements.DATE_INPUT.getElemName() to DEFAULT_DATE_STRING
        ))
        val sd = ServerData(au, tru, AnalyzedHttpData(data = data), authStatus = AuthStatus.AUTHENTICATED, testLogger)
        val ex = assertThrows(InexactInputsException::class.java){ EnterTimeAPI.handlePost(sd) }
        assertEquals("expected keys: [project_entry, time_entry, detail_entry, date_entry]. received keys: [time_entry, detail_entry, date_entry]", ex.message)
    }

    /**
     * If we are missing required data
     */
    @Test
    fun testEnterTimeAPI_missingTimeEntry() {
        val data = PostBodyData(mapOf(
                EnterTimeAPI.Elements.PROJECT_INPUT.getElemName() to "1",
                EnterTimeAPI.Elements.DETAIL_INPUT.getElemName() to "not much to say",
                EnterTimeAPI.Elements.DATE_INPUT.getElemName() to DEFAULT_DATE_STRING
        ))
        val sd = ServerData(au, tru, AnalyzedHttpData(data = data), authStatus = AuthStatus.AUTHENTICATED, testLogger)
        val ex = assertThrows(InexactInputsException::class.java){ EnterTimeAPI.handlePost(sd) }
        assertEquals("expected keys: [project_entry, time_entry, detail_entry, date_entry]. received keys: [project_entry, detail_entry, date_entry]", ex.message)
    }

    /**
     * If we are missing required data
     */
    @Test
    fun testEnterTimeAPI_missingDetailEntry() {
        val data = PostBodyData(mapOf(
                EnterTimeAPI.Elements.PROJECT_INPUT.getElemName() to "1",
                EnterTimeAPI.Elements.TIME_INPUT.getElemName() to "60",
                EnterTimeAPI.Elements.DATE_INPUT.getElemName() to DEFAULT_DATE_STRING,
        ))
        val sd = ServerData(au, tru, AnalyzedHttpData(data = data), authStatus = AuthStatus.AUTHENTICATED, testLogger)
        val ex = assertThrows(InexactInputsException::class.java){ EnterTimeAPI.handlePost(sd) }
        assertEquals("expected keys: [project_entry, time_entry, detail_entry, date_entry]. received keys: [project_entry, time_entry, date_entry]", ex.message)
    }

    /**
     * If we are missing required data
     */
    @Test
    fun testEnterTimeAPI_missingEmployee() {
        val data = PostBodyData(mapOf(
                EnterTimeAPI.Elements.PROJECT_INPUT.getElemName() to "1",
                EnterTimeAPI.Elements.TIME_INPUT.getElemName() to "1",
                EnterTimeAPI.Elements.DETAIL_INPUT.getElemName() to "not much to say",
                EnterTimeAPI.Elements.DATE_INPUT.getElemName() to DEFAULT_DATE_STRING
        ))
        val sd = ServerData(au, tru, AnalyzedHttpData(data = data, user = DEFAULT_USER_NO_EMPLOYEE), authStatus = AuthStatus.AUTHENTICATED, testLogger)
        val ex = assertThrows(IllegalStateException::class.java){ EnterTimeAPI.handlePost(sd) }
        assertEquals(employeeIdNotNullMsg, ex.message)
    }

    /**
     * If we pass in something that cannot be parsed as an integer as the project id
     */
    @Test
    fun testEnterTimeAPI_nonNumericProject() {
        val data = PostBodyData(mapOf(EnterTimeAPI.Elements.PROJECT_INPUT.getElemName() to "aaaaa", EnterTimeAPI.Elements.TIME_INPUT.getElemName() to "60", EnterTimeAPI.Elements.DETAIL_INPUT.getElemName() to "not much to say", EnterTimeAPI.Elements.DATE_INPUT.getElemName() to A_RANDOM_DAY_IN_JUNE_2020.epochDay.toString()))
        val sd = ServerData(au, tru, AnalyzedHttpData(data = data), authStatus = AuthStatus.AUTHENTICATED, testLogger)
        val ex = assertThrows(java.lang.IllegalArgumentException::class.java){ EnterTimeAPI.handlePost(sd) }
        assertEquals("Must be able to parse aaaaa as integer", ex.message)
    }

    /**
     * If we pass in a negative number as the project id
     */
    @Test
    fun testEnterTimeAPI_negativeProject() {
        val data = PostBodyData(mapOf(
            EnterTimeAPI.Elements.PROJECT_INPUT.getElemName() to "-1",
            EnterTimeAPI.Elements.TIME_INPUT.getElemName() to "60",
            EnterTimeAPI.Elements.DETAIL_INPUT.getElemName() to "not much to say",
            EnterTimeAPI.Elements.DATE_INPUT.getElemName() to A_RANDOM_DAY_IN_JUNE_2020.epochDay.toString()))
        val sd = ServerData(au, tru, AnalyzedHttpData(data = data), authStatus = AuthStatus.AUTHENTICATED, testLogger)
        val ex = assertThrows(IllegalArgumentException::class.java){ EnterTimeAPI.handlePost(sd) }
        assertEquals("Valid identifier values are 1 or above", ex.message)
    }

    /**
     * If we pass in 0 as the project id
     */
    @Test
    fun testEnterTimeAPI_zeroProject() {
        val data = PostBodyData(mapOf(
            EnterTimeAPI.Elements.PROJECT_INPUT.getElemName() to "0",
            EnterTimeAPI.Elements.TIME_INPUT.getElemName() to "60",
            EnterTimeAPI.Elements.DETAIL_INPUT.getElemName() to "not much to say",
            EnterTimeAPI.Elements.DATE_INPUT.getElemName() to A_RANDOM_DAY_IN_JUNE_2020.epochDay.toString()))
        val sd = ServerData(au, tru, AnalyzedHttpData(data = data), authStatus = AuthStatus.AUTHENTICATED, testLogger)
        val ex = assertThrows(IllegalArgumentException::class.java){ EnterTimeAPI.handlePost(sd) }
        assertEquals("Valid identifier values are 1 or above", ex.message)
    }

    /**
     * If the project id passed is above the maximum id
     */
    @Test
    fun testEnterTimeAPI_aboveMaxProject() {
        val data = PostBodyData(mapOf(
            EnterTimeAPI.Elements.PROJECT_INPUT.getElemName() to (maximumProjectsCount +1).toString(),
            EnterTimeAPI.Elements.TIME_INPUT.getElemName() to "60",
            EnterTimeAPI.Elements.DETAIL_INPUT.getElemName() to "not much to say",
            EnterTimeAPI.Elements.DATE_INPUT.getElemName() to A_RANDOM_DAY_IN_JUNE_2020.epochDay.toString()))
        val sd = ServerData(au, tru, AnalyzedHttpData(data = data), authStatus = AuthStatus.AUTHENTICATED, testLogger)
        val ex = assertThrows(IllegalArgumentException::class.java){ EnterTimeAPI.handlePost(sd) }
        assertEquals("No project id allowed over $maximumProjectsCount", ex.message)
    }


    /**
     * If the time entered is more than a day's worth
     */
    @Test
    fun testEnterTimeAPI_aboveMaxTime() {
        val data = PostBodyData(mapOf(
            EnterTimeAPI.Elements.PROJECT_INPUT.getElemName() to "1",
            EnterTimeAPI.Elements.TIME_INPUT.getElemName() to ((24 * 60) + 0.01).toString(),
            EnterTimeAPI.Elements.DETAIL_INPUT.getElemName() to "not much to say",
            EnterTimeAPI.Elements.DATE_INPUT.getElemName() to A_RANDOM_DAY_IN_JUNE_2020.epochDay.toString()))
        val sd = ServerData(au, tru, AnalyzedHttpData(data = data), authStatus = AuthStatus.AUTHENTICATED, testLogger)
        val ex = assertThrows(IllegalArgumentException::class.java){ EnterTimeAPI.handlePost(sd) }
        assertEquals("${lessThanTimeInDayMsg}86401", ex.message)
    }

    /**
     * If the time entered is negative
     */
    @Test
    fun testEnterTimeAPI_negativeTime() {
        val data = PostBodyData(mapOf(
            EnterTimeAPI.Elements.PROJECT_INPUT.getElemName() to "1",
            EnterTimeAPI.Elements.TIME_INPUT.getElemName() to "-1",
            EnterTimeAPI.Elements.DETAIL_INPUT.getElemName() to "not much to say",
            EnterTimeAPI.Elements.DATE_INPUT.getElemName() to A_RANDOM_DAY_IN_JUNE_2020.epochDay.toString()))
        val sd = ServerData(au, tru, AnalyzedHttpData(data = data), authStatus = AuthStatus.AUTHENTICATED, testLogger)
        val ex = assertThrows(IllegalArgumentException::class.java){ EnterTimeAPI.handlePost(sd) }
        assertEquals("$noNegativeTimeMsg-60", ex.message)
    }

    /**
     * If the time entered is zero, it's fine.
     */
    @Test
    fun testEnterTimeAPI_zeroTime() {
        val data = PostBodyData(mapOf(
                EnterTimeAPI.Elements.PROJECT_INPUT.getElemName() to "1",
                EnterTimeAPI.Elements.TIME_INPUT.getElemName() to "0",
                EnterTimeAPI.Elements.DETAIL_INPUT.getElemName() to "not much to say",
                EnterTimeAPI.Elements.DATE_INPUT.getElemName() to DEFAULT_DATE_STRING
        ))
        val sd = ServerData(au, tru, AnalyzedHttpData(data = data), authStatus = AuthStatus.AUTHENTICATED, testLogger)
        val result = EnterTimeAPI.handlePost(sd)

        assertSuccessfulTimeEntry(result)
    }

    /**
     * If the time entered is non-numeric, like "a"
     */
    @Test
    fun testEnterTimeAPI_nonNumericTime() {
        val data = PostBodyData(mapOf(
                EnterTimeAPI.Elements.PROJECT_INPUT.getElemName() to "1",
                EnterTimeAPI.Elements.TIME_INPUT.getElemName() to "aaa",
                EnterTimeAPI.Elements.DETAIL_INPUT.getElemName() to "not much to say",
                EnterTimeAPI.Elements.DATE_INPUT.getElemName() to DEFAULT_DATE_STRING
        ))
        val sd = ServerData(au, tru, AnalyzedHttpData(data = data), authStatus = AuthStatus.AUTHENTICATED, testLogger)
        val ex = assertThrows(java.lang.IllegalArgumentException::class.java){ EnterTimeAPI.handlePost(sd) }
        assertEquals("Must be able to parse aaa as double", ex.message)
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
        val pmd = PureMemoryDatabase()
        val tep  = TimeEntryPersistence(pmd, logger = testLogger)
        val au = AuthenticationUtilities(AuthenticationPersistence(pmd, logger = testLogger), testLogger)
        val employee : Employee = tep.persistNewEmployee(DEFAULT_EMPLOYEE_NAME)
        val user = au.register(DEFAULT_USER.name, DEFAULT_PASSWORD, employee.id).user
        val tru = TimeRecordingUtilities(tep, CurrentUser(user), testLogger)
        val project : Project = tep.persistNewProject(DEFAULT_PROJECT_NAME)
        val projectId = project.id.value.toString()

        val (time, _) = getTime {
            for (i in 1..numberOfRequests) {

                val data = PostBodyData(mapOf(
                    EnterTimeAPI.Elements.PROJECT_INPUT.getElemName() to projectId,
                    EnterTimeAPI.Elements.TIME_INPUT.getElemName() to "1",
                    EnterTimeAPI.Elements.DETAIL_INPUT.getElemName() to "not much to say",
                    EnterTimeAPI.Elements.DATE_INPUT.getElemName() to Date(A_RANDOM_DAY_IN_JUNE_2020.epochDay + (i / 20)).stringValue
                ))

                val sd = ServerData(au, tru, AnalyzedHttpData(data = data), authStatus = AuthStatus.AUTHENTICATED, testLogger)
                val result = EnterTimeAPI.handlePost(sd)

                assertSuccessfulTimeEntry(result)

            }
        }
        testLogger.resetLogSettingsToDefault()
        println(time)
        File("${granularPerfArchiveDirectory}testEnterTimeAPI_PERFORMANCE")
            .appendText("${Date.now().stringValue}\trequests: $numberOfRequests\ttime: $time milliseconds\n")
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
     * this should confirm what happens when a user successfully enters their time
     */
    private fun assertSuccessfulTimeEntry(result: PreparedResponseData) {
        assertEquals(StatusCode.SEE_OTHER, result.statusCode)
        assertTrue(result.headers.any { it.matches(redirectRegex)})
    }

    companion object {
        val redirectRegex = """Location: timeentries\?date=....-..-..""".toRegex()
    }

}