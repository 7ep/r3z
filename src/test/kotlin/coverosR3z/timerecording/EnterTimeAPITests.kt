package coverosR3z.timerecording

import coverosR3z.*
import coverosR3z.authentication.*
import coverosR3z.authentication.persistence.AuthenticationPersistence
import coverosR3z.authentication.types.CurrentUser
import coverosR3z.authentication.utility.AuthenticationUtilities
import coverosR3z.misc.exceptions.InexactInputsException
import coverosR3z.logging.resetLogSettingsToDefault
import coverosR3z.logging.turnOffAllLogging
import coverosR3z.misc.utility.getTime
import coverosR3z.misc.types.Date
import coverosR3z.misc.utility.toStr
import coverosR3z.persistence.utility.PureMemoryDatabase
import coverosR3z.server.*
import coverosR3z.server.utility.AuthStatus
import coverosR3z.server.utility.doPOSTAuthenticated
import coverosR3z.timerecording.api.EnterTimeAPI
import coverosR3z.timerecording.persistence.TimeEntryPersistence
import coverosR3z.timerecording.types.*
import coverosR3z.timerecording.utility.TimeRecordingUtilities
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

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
        val data = mapOf(
                EnterTimeAPI.Elements.PROJECT_INPUT.elemName to "1",
                EnterTimeAPI.Elements.TIME_INPUT.elemName to "60",
                EnterTimeAPI.Elements.DETAIL_INPUT.elemName to "not much to say",
                EnterTimeAPI.Elements.DATE_INPUT.elemName to DEFAULT_DATE_STRING)
        val response = EnterTimeAPI.handlePOST(tru, DEFAULT_USER.employeeId ,data).fileContents
        assertTrue("we should have gotten the success page.  Got: $response", toStr(response).contains("SUCCESS"))
    }

    /**
     * If we are missing required data
     */
    @Test
    fun testEnterTimeAPI_missingProject() {
        val data = mapOf(
                EnterTimeAPI.Elements.TIME_INPUT.elemName to "60",
                EnterTimeAPI.Elements.DETAIL_INPUT.elemName to "not much to say",
                EnterTimeAPI.Elements.DATE_INPUT.elemName to DEFAULT_DATE_STRING)
        val ex = assertThrows(InexactInputsException::class.java){ doPOSTAuthenticated(AuthStatus.AUTHENTICATED, EnterTimeAPI.requiredInputs, data) { EnterTimeAPI.handlePOST(tru, DEFAULT_USER.employeeId, data) } }
        assertEquals("expected keys: [project_entry, time_entry, detail_entry, date_entry]. received keys: [time_entry, detail_entry, date_entry]", ex.message)
    }

    /**
     * If we are missing required data
     */
    @Test
    fun testEnterTimeAPI_missingTimeEntry() {
        val data = mapOf(
                EnterTimeAPI.Elements.PROJECT_INPUT.elemName to "1",
                EnterTimeAPI.Elements.DETAIL_INPUT.elemName to "not much to say",
                EnterTimeAPI.Elements.DATE_INPUT.elemName to DEFAULT_DATE_STRING)
        val ex = assertThrows(InexactInputsException::class.java){ doPOSTAuthenticated(AuthStatus.AUTHENTICATED, EnterTimeAPI.requiredInputs, data) { EnterTimeAPI.handlePOST(tru, DEFAULT_USER.employeeId, data) } }
        assertEquals("expected keys: [project_entry, time_entry, detail_entry, date_entry]. received keys: [project_entry, detail_entry, date_entry]", ex.message)
    }

    /**
     * If we are missing required data
     */
    @Test
    fun testEnterTimeAPI_missingDetailEntry() {
        val data = mapOf(
                EnterTimeAPI.Elements.PROJECT_INPUT.elemName to "1",
                EnterTimeAPI.Elements.TIME_INPUT.elemName to "60",
                EnterTimeAPI.Elements.DATE_INPUT.elemName to DEFAULT_DATE_STRING,
        )
        val ex = assertThrows(InexactInputsException::class.java){ doPOSTAuthenticated(AuthStatus.AUTHENTICATED, EnterTimeAPI.requiredInputs, data) { EnterTimeAPI.handlePOST(tru, DEFAULT_USER.employeeId, data) } }
        assertEquals("expected keys: [project_entry, time_entry, detail_entry, date_entry]. received keys: [project_entry, time_entry, date_entry]", ex.message)
    }

    /**
     * If we are missing required data
     */
    @Test
    fun testEnterTimeAPI_missingEmployee() {
        val data = mapOf(
                EnterTimeAPI.Elements.PROJECT_INPUT.elemName to "1",
                EnterTimeAPI.Elements.TIME_INPUT.elemName to "60",
                EnterTimeAPI.Elements.DETAIL_INPUT.elemName to "not much to say",
                EnterTimeAPI.Elements.DATE_INPUT.elemName to DEFAULT_DATE_STRING)
        val employeeId = null
        val ex = assertThrows(IllegalStateException::class.java){
            EnterTimeAPI.handlePOST(tru, employeeId, data)
        }
        assertEquals(employeeIdNotNullMsg, ex.message)
    }

    /**
     * If we pass in something that cannot be parsed as an integer as the project id
     */
    @Test
    fun testEnterTimeAPI_nonNumericProject() {
        val data = mapOf(EnterTimeAPI.Elements.PROJECT_INPUT.elemName to "aaaaa", EnterTimeAPI.Elements.TIME_INPUT.elemName to "60", EnterTimeAPI.Elements.DETAIL_INPUT.elemName to "not much to say", EnterTimeAPI.Elements.DATE_INPUT.elemName to A_RANDOM_DAY_IN_JUNE_2020.epochDay.toString())
        val ex = assertThrows(java.lang.IllegalArgumentException::class.java){ EnterTimeAPI.handlePOST(tru, DEFAULT_USER.employeeId,data) }
        assertEquals("Must be able to parse aaaaa as integer", ex.message)
    }

    /**
     * If we pass in a negative number as the project id
     */
    @Test
    fun testEnterTimeAPI_negativeProject() {
        val data = mapOf(
            EnterTimeAPI.Elements.PROJECT_INPUT.elemName to "-1",
            EnterTimeAPI.Elements.TIME_INPUT.elemName to "60",
            EnterTimeAPI.Elements.DETAIL_INPUT.elemName to "not much to say",
            EnterTimeAPI.Elements.DATE_INPUT.elemName to A_RANDOM_DAY_IN_JUNE_2020.epochDay.toString())
        val ex = assertThrows(IllegalArgumentException::class.java){ EnterTimeAPI.handlePOST(tru, DEFAULT_USER.employeeId,data) }
        assertEquals("Valid identifier values are 1 or above", ex.message)
    }

    /**
     * If we pass in 0 as the project id
     */
    @Test
    fun testEnterTimeAPI_zeroProject() {
        val data = mapOf(
            EnterTimeAPI.Elements.PROJECT_INPUT.elemName to "0",
            EnterTimeAPI.Elements.TIME_INPUT.elemName to "60",
            EnterTimeAPI.Elements.DETAIL_INPUT.elemName to "not much to say",
            EnterTimeAPI.Elements.DATE_INPUT.elemName to A_RANDOM_DAY_IN_JUNE_2020.epochDay.toString())
        val ex = assertThrows(IllegalArgumentException::class.java){ EnterTimeAPI.handlePOST(tru, DEFAULT_USER.employeeId,data) }
        assertEquals("Valid identifier values are 1 or above", ex.message)
    }

    /**
     * If the project id passed is above the maximum id
     */
    @Test
    fun testEnterTimeAPI_aboveMaxProject() {
        val data = mapOf(
            EnterTimeAPI.Elements.PROJECT_INPUT.elemName to (maximumProjectsCount +1).toString(),
            EnterTimeAPI.Elements.TIME_INPUT.elemName to "60",
            EnterTimeAPI.Elements.DETAIL_INPUT.elemName to "not much to say",
            EnterTimeAPI.Elements.DATE_INPUT.elemName to A_RANDOM_DAY_IN_JUNE_2020.epochDay.toString())
        val ex = assertThrows(IllegalArgumentException::class.java){ EnterTimeAPI.handlePOST(tru, DEFAULT_USER.employeeId,data) }
        assertEquals("No project id allowed over $maximumProjectsCount", ex.message)
    }


    /**
     * If the time entered is more than a day's worth
     */
    @Test
    fun testEnterTimeAPI_aboveMaxTime() {
        val data = mapOf(
            EnterTimeAPI.Elements.PROJECT_INPUT.elemName to "1",
            EnterTimeAPI.Elements.TIME_INPUT.elemName to ((60*60*24)+1).toString(),
            EnterTimeAPI.Elements.DETAIL_INPUT.elemName to "not much to say",
            EnterTimeAPI.Elements.DATE_INPUT.elemName to A_RANDOM_DAY_IN_JUNE_2020.epochDay.toString())
        val ex = assertThrows(IllegalArgumentException::class.java){ EnterTimeAPI.handlePOST(tru, DEFAULT_USER.employeeId,data) }
        assertEquals("${lessThanTimeInDayMsg}86401", ex.message)
    }

    /**
     * If the time entered is negative
     */
    @Test
    fun testEnterTimeAPI_negativeTime() {
        val data = mapOf(
            EnterTimeAPI.Elements.PROJECT_INPUT.elemName to "1",
            EnterTimeAPI.Elements.TIME_INPUT.elemName to "-60",
            EnterTimeAPI.Elements.DETAIL_INPUT.elemName to "not much to say",
            EnterTimeAPI.Elements.DATE_INPUT.elemName to A_RANDOM_DAY_IN_JUNE_2020.epochDay.toString())
        val ex = assertThrows(IllegalArgumentException::class.java){ EnterTimeAPI.handlePOST(tru, DEFAULT_USER.employeeId,data) }
        assertEquals("$noNegativeTimeMsg-60", ex.message)
    }

    /**
     * If the time entered is zero, it's fine.
     */
    @Test
    fun testEnterTimeAPI_zeroTime() {
        val data = mapOf(
                EnterTimeAPI.Elements.PROJECT_INPUT.elemName to "1",
                EnterTimeAPI.Elements.TIME_INPUT.elemName to "0",
                EnterTimeAPI.Elements.DETAIL_INPUT.elemName to "not much to say",
                EnterTimeAPI.Elements.DATE_INPUT.elemName to DEFAULT_DATE_STRING)
        val result = EnterTimeAPI.handlePOST(tru, DEFAULT_USER.employeeId,data).fileContents
        assertTrue("we should have gotten the success page.  Got: $result", toStr(result).contains("SUCCESS"))
    }

    /**
     * If the time entered is non-numeric, like "a"
     */
    @Test
    fun testEnterTimeAPI_nonNumericTime() {
        val data = mapOf(
                EnterTimeAPI.Elements.PROJECT_INPUT.elemName to "1",
                EnterTimeAPI.Elements.TIME_INPUT.elemName to "aaa",
                EnterTimeAPI.Elements.DETAIL_INPUT.elemName to "not much to say",
                EnterTimeAPI.Elements.DATE_INPUT.elemName to DEFAULT_DATE_STRING)
        val ex = assertThrows(java.lang.IllegalArgumentException::class.java){ doPOSTAuthenticated(AuthStatus.AUTHENTICATED, EnterTimeAPI.requiredInputs, data) { EnterTimeAPI.handlePOST(tru, DEFAULT_USER.employeeId, data) } }
        assertEquals("Must be able to parse aaa as integer", ex.message)
    }

    /**
     * What should happen if we send too many inputs to the API?
     * It should complain.  We want precision.
     *
     * In this test we expect project, time, detail, and date to
     * be sent.  If we get project, time, detail, FOO, and date,
     * we throw the exception
     *
     * See [InexactInputsException]
     */
    @Test
    fun testDoPOST_TooManyInputs() {
        val data = mapOf(
                EnterTimeAPI.Elements.PROJECT_INPUT.elemName to "1",
                EnterTimeAPI.Elements.TIME_INPUT.elemName to "60",
                EnterTimeAPI.Elements.DETAIL_INPUT.elemName to "not much to say",
                "foo" to "bar",
                EnterTimeAPI.Elements.DATE_INPUT.elemName to DEFAULT_DATE_STRING)

        val ex = assertThrows(InexactInputsException::class.java) { doPOSTAuthenticated(AuthStatus.AUTHENTICATED, EnterTimeAPI.requiredInputs, data) { EnterTimeAPI.handlePOST(tru, DEFAULT_USER.employeeId, data) } }
        assertEquals("expected keys: [project_entry, time_entry, detail_entry, date_entry]. received keys: [project_entry, time_entry, detail_entry, foo, date_entry]", ex.message)
    }

    /**
     * Just how quickly does it go, from this level?
     *
     * With 1000 requests, it takes .180 seconds = 5,555 requests per second.
     *
     * See [ServerPerformanceTests.testEnterTime_PERFORMANCE]
     */
    @Test
    fun testEnterTimeAPI_PERFORMANCE() {
        val numberOfRequests = 100

        turnOffAllLogging()
        // set up real database
        val pmd = PureMemoryDatabase()
        val tep  = TimeEntryPersistence(pmd)
        val au = AuthenticationUtilities(AuthenticationPersistence(pmd))
        val employee : Employee = tep.persistNewEmployee(DEFAULT_EMPLOYEE_NAME)
        val user = au.register(DEFAULT_USER.name, DEFAULT_PASSWORD, employee.id).user
        val tru = TimeRecordingUtilities(tep, CurrentUser(user))
        val project : Project = tep.persistNewProject(DEFAULT_PROJECT_NAME)
        val projectId = project.id.value.toString()

        val (time, _) = getTime {
            for (i in 1..numberOfRequests) {

                val data = mapOf(
                    EnterTimeAPI.Elements.PROJECT_INPUT.elemName to projectId,
                    EnterTimeAPI.Elements.TIME_INPUT.elemName to "1",
                    EnterTimeAPI.Elements.DETAIL_INPUT.elemName to "not much to say",
                    EnterTimeAPI.Elements.DATE_INPUT.elemName to Date(A_RANDOM_DAY_IN_JUNE_2020.epochDay + (i / 20)).stringValue
                )

                val response = EnterTimeAPI.handlePOST(tru, employee.id, data).fileContents
                assertTrue(
                    "we should have gotten the success page.  Got: $response",
                    toStr(response).contains("SUCCESS")
                )
            }
        }
        resetLogSettingsToDefault()
        println(time)
    }

    /*
     _ _       _                  __ __        _    _           _
    | | | ___ | | ___  ___  _ _  |  \  \ ___ _| |_ | |_  ___  _| | ___
    |   |/ ._>| || . \/ ._>| '_> |     |/ ._> | |  | . |/ . \/ . |<_-<
    |_|_|\___.|_||  _/\___.|_|   |_|_|_|\___. |_|  |_|_|\___/\___|/__/
                 |_|
     alt-text: Helper Methods
     */

}