package com.coveros.r3z.timerecording

import com.coveros.r3z.domainobjects.*
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert
import org.junit.Test

class TimeRecordingTests {

    private val threeHoursFifteen = (3 * 60) + 15
    private val mockTimeEntryPersistence = mockAPersistenceLayer()
    private val utils = TimeRecordingUtilities(mockTimeEntryPersistence)

    /**
     * Happy path - record time successfully
     */
    @Test
    fun `record time for someone`() {
        val entry = makeDefaultTimeEntryHelper()
        val expectedResult = RecordTimeResult(id =1, status = StatusEnum.SUCCESS)

        val actualResult = utils.recordTime(entry)

        Assert.assertEquals("expect to see a success indicator", expectedResult, actualResult)
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
        val entry = makeDefaultTimeEntryHelper(project=Project(null, "an invalid project"))
        val expectedResult = RecordTimeResult(id =null, status = StatusEnum.INVALID_PROJECT)

        val actualResult = utils.recordTime(entry)

        Assert.assertEquals("Expect to see a success indicator", expectedResult, actualResult)
    }

    /**
     * Generates a default time entry for use in testing
     */
    private fun makeDefaultTimeEntryHelper(
            user : User = User(1, ""),
            time : Time = Time(300),
            project : Project = Project(1, "project"),
            details : Details = Details("testing, testing")
    ): TimeEntry {
        return TimeEntry(user, project, time, details)
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
        val expectedDataEntry = generateDataEntry()
        val user = User(1, "")
        val project = Project(1, "a")
        val time = Time(threeHoursFifteen)
        val details = Details("sample comment")
        val actualDataEntry : TimeEntry = makeDataEntry(user, project, time, details)
        Assert.assertEquals(expectedDataEntry, actualDataEntry)
    }

    @Test
    fun `a user should have a unique integer identifier`() {
        val user = User(1, "")
        Assert.assertEquals(1, user.id)
    }

    @Test
    fun `a user should have a name`() {
        val name = "this is my name bro"
        val id : Long = 1

        val user = User(id, name)

        Assert.assertEquals(id, user.id)
        Assert.assertEquals(name, user.name)
    }

    @Test
    fun `a time should have a decimal representation of its value`() {
        val time = Time(threeHoursFifteen)
        Assert.assertEquals(threeHoursFifteen, time.numberOfMinutes)
    }

    @Test
    fun `a project should have a name and an id`() {
        val project = Project(1, "some project name")
        Assert.assertEquals(1, project.id)
        Assert.assertEquals("some project name", project.name)
    }

    @Test
    fun `details should have a string representation` () {
        val actual = Details("Testing, testing")
        val expected = "Testing, testing"
        Assert.assertEquals(expected, actual.value)
    }

    /**
     * Crazy-long details are shunned
     */
    @Test fun `details shouldn't be too long`() {
        Assert.assertThrows(AssertionError::class.java) { Details("way too long wayyyy too long  ".repeat(30)) }
    }

    @Test fun `there should be no difference between details with no args and details with ""`() {
        val actual = Details("")
        val expected = Details()
        Assert.assertEquals(expected, actual)
    }


    /**
     * A helper method to create data entries for timekeeping
     */
    private fun generateDataEntry() : TimeEntry {
        return TimeEntry(User(1, ""), Project(1, "a"), Time(threeHoursFifteen), Details())
    }

    private fun makeDataEntry(user: User, project: Project, time: Time, details: Details) : TimeEntry {
        return TimeEntry(User(1, ""), Project(1, "a"), Time(threeHoursFifteen), Details())
    }

}