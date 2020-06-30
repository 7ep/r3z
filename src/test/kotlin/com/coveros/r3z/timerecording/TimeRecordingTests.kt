package com.coveros.r3z.timerecording

import com.coveros.r3z.A_RANDOM_DAY_IN_JUNE_2020
import com.coveros.r3z.THREE_HOURS_FIFTEEN
import com.coveros.r3z.exceptions.ExceededDailyHoursAmountException
import com.coveros.r3z.createTimeEntry
import com.coveros.r3z.domainobjects.*
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class TimeRecordingTests {

    private val mockTimeEntryPersistence = mockAPersistenceLayer()
    private val utils = TimeRecordingUtilities(mockTimeEntryPersistence)


    /**
     * Happy path - record time successfully
     */
    @Test
    fun `record time for someone`() {
        val entry = makeDefaultTimeEntryHelper()
        val expectedResult = RecordTimeResult(id =1, status = StatusEnum.SUCCESS)
        every { mockTimeEntryPersistence.queryMinutesRecorded(any(), any()) } returns 60

        val actualResult = utils.recordTime(entry)

        assertEquals("expect to see a success indicator", expectedResult, actualResult)
    }


    @Test fun `creating a time entry enters a log item`() {
        val entry = makeDefaultTimeEntryHelper()
        every { mockTimeEntryPersistence.queryMinutesRecorded(any(), any()) } returns 60
        utils.recordTime(entry)

        val logs : List<String> = getLogForTimeRecordingUtils()

        assertTrue("expect to see an entry about recording Time", logs.contains("a new time entry was recorded with this info: foo bar"))
    }

    /**
     * Really simplistic mock - make persistNewTimeEntry always return 1
     */
    private fun mockAPersistenceLayer(): TimeEntryPersistence {
        val tep = mockk<TimeEntryPersistence>()
        every { tep.persistNewTimeEntry(any()) } returns 1
        return tep
    }

    /**
     * Negative case - what happens if we receive a request to enter
     * time for an invalid project?
     */
    @Test
    fun `Should fail to record time for non-existent project`() {
        // it's an invalid project because the project doesn't exist
        val entry = makeDefaultTimeEntryHelper(project=Project(1, "an invalid project"))
        val expectedResult = RecordTimeResult(id =null, status = StatusEnum.INVALID_PROJECT)
        every { mockTimeEntryPersistence.queryMinutesRecorded(any(), any()) } returns 60

        val actualResult = utils.recordTime(entry)

        assertEquals("Expect to see a success indicator", expectedResult, actualResult)
    }

    /**
     * Negative case - what happens if we ask the system to record
     * any more time when we've already recorded the maximum for the day?
     * (It should throw an exception)
     */
    @Test
    fun `Should throw ExceededDailyHoursException when too asked to record more than 24 hours total in a day for 24 hours`() {
        // it's an invalid project because the project doesn't exist
        val entry = makeDefaultTimeEntryHelper(time=Time(1), project=Project(1, "an invalid project"))
        `setup 24 hours already recorded for the day`()

        assertThrows(ExceededDailyHoursAmountException::class.java) { utils.recordTime(entry) }
    }

    /**
     * Negative case - what happens if we ask the system to record
     * any more time when we've already recorded the maximum for the day?
     * (It should throw an exception)
     */
    @Test
    fun `Should throw ExceededDailyHoursException when too asked to record more than 24 hours total in a day for 23 hours`() {
        // it's an invalid project because the project doesn't exist
        val entry = makeDefaultTimeEntryHelper(time=Time(60*2), project=Project(1, "an invalid project"))
        `setup 23 hours already recorded for the day`()

        assertThrows(ExceededDailyHoursAmountException::class.java) { utils.recordTime(entry) }
    }

    @Test
    fun `just checking that two similar time entries are considered equal`() {
        assertEquals(createTimeEntry(), createTimeEntry())
    }

    private fun `setup 24 hours already recorded for the day`() {
        val twentyFourHours: Long = 24 * 60
        every { mockTimeEntryPersistence.queryMinutesRecorded(any(), any()) } returns twentyFourHours
    }

    private fun `setup 23 hours already recorded for the day`() {
        val twentyThreeHours: Long = 23 * 60
        every { mockTimeEntryPersistence.queryMinutesRecorded(any(), any()) } returns twentyThreeHours
    }

    /**
     * Generates a default time entry for use in testing
     */
    private fun makeDefaultTimeEntryHelper(
            user : User = User(1, ""),
            time : Time = THREE_HOURS_FIFTEEN,
            project : Project = Project(1, "project"),
            date : Date = A_RANDOM_DAY_IN_JUNE_2020,
            details : Details = Details("testing, testing")
    ): TimeEntry {
        return TimeEntry(user, project, time, date, details)
    }

    /**
     * recordTime() should:
     * - put the time entry in a database
     * - returns a status based on how the above action went
     * - error on invalid timeEntry (e.g. project is invalid, user is invalid)
     */

    /**
     * Now, this is something functional.  On the input, we want various
     * data, on the output we want a nice data object that has the relevant
     * interrelated data
     */
    @Test
    fun `make time entry`() {
        val timeEntry : TimeEntry = createTimeEntry(date = A_RANDOM_DAY_IN_JUNE_2020)
        assertEquals(User(1, "I"), timeEntry.user)
        assertEquals(Time(60), timeEntry.time)
        assertEquals(Project(1, "A"), timeEntry.project)
        assertEquals(Details(), timeEntry.details)
    }

    @Test
    fun `a user should have a unique integer identifier`() {
        val user = User(1, "")
        assertEquals(1, user.id)
    }

    @Test
    fun `a user should have a name`() {
        val name = "this is my name bro"
        val id : Long = 1

        val user = User(id, name)

        assertEquals(id, user.id)
        assertEquals(name, user.name)
    }

    @Test
    fun `a time should contain an integer number of minutes`() {
        val time = Time((60 * 3) + 15)
        assertEquals(THREE_HOURS_FIFTEEN, time)
    }

    @Test
    fun `a project should have a name and an id`() {
        val project = Project(1, "some project name")
        assertEquals(1, project.id)
        assertEquals("some project name", project.name)
    }

    @Test
    fun `details should have a string representation` () {
        val actual = Details("Testing, testing")
        val expected = "Testing, testing"
        assertEquals(expected, actual.value)
    }

    /**
     * Both the database the type disallow a details message longer than 500 characters.
     * Testing that 501 throws an exception
     */
    @Test fun `Details should throw an exception if longer than 500 characters`() {
        val ex = assertThrows(AssertionError::class.java ) {Details("A".repeat(501))}
        assertTrue(ex.message.toString().contains("lord's prayer"))
    }

    /**
     * Both the database the type disallow a details message longer than 500 characters.
     * Testing that 499 doesn't throw an exception
     */
    @Test fun `Details should allow an input of 499 characters`() {
        val value = "A".repeat(499)
        val details = Details(value)
        assertEquals(value, details.value)
    }

    /**
     * Both the database the type disallow a details message longer than 500 characters.
     * Testing that 500 doesn't throw an exception
     */
    @Test fun `Details should allow an input of 500 characters`() {
        val value = "A".repeat(500)
        val details = Details(value)
        assertEquals(value, details.value)
    }

    @Test fun `there should be no difference between details with no args and details with ""`() {
        val actual = Details("")
        val expected = Details()
        assertEquals(expected, actual)
    }

    @Test fun `Can't record a time entry that has 0 minutes`() {
        val ex = assertThrows(AssertionError::class.java ) {Time(0)}
        assertTrue(ex.message.toString().contains("Doesn't make sense to have zero or negative time"))
    }

    @Test fun `Can't record a time entry that has -1 minutes`() {
        val ex = assertThrows(AssertionError::class.java ) {Time(-1)}
        assertTrue(ex.message.toString().contains("Doesn't make sense to have zero or negative time"))
    }

    /**
     * it's easy to get this confused, but [LocalDate.toEpochDay] gives a number of days,
     * but [java.sql.Date] actually needs a number of milliseconds since 1970, not days.
     */
    @Test fun `java sql Date needs to be accurately converted to our Date class`() {

        val epochDate = LocalDate.parse("2020-07-01").atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val sqlDate = java.sql.Date(epochDate)
        val actual = Date.convertSqlDateToOurDate(sqlDate)
        val expected = Date(2020, Month.JUL, 1)
        assertEquals(expected, actual)
    }

    /**
     * a basic happy path
     */
    @Test fun `can create project`() {
        every { mockTimeEntryPersistence.persistNewProject(any()) } returns Project(1, "test project")
        val expected = utils.createProject(ProjectName("test project"))
        val actual = Project(1, "test project")
        assertEquals(expected, actual)
    }


}
