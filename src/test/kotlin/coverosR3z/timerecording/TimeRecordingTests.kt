package coverosR3z.timerecording

import coverosR3z.exceptions.ExceededDailyHoursAmountException
import coverosR3z.*
import coverosR3z.domainobjects.*
import coverosR3z.persistence.ProjectIntegrityViolationException
import org.junit.Assert.*
import org.junit.Test

class TimeRecordingTests {

    /**
     * Happy path - record time successfully
     */
    @Test
    fun `record time for someone`() {
        val fakeTimeEntryPersistence = FakeTimeEntryPersistence(minutesRecorded = 60)
        val utils = TimeRecordingUtilities(fakeTimeEntryPersistence)
        val entry = createTimeEntryPreDatabase()
        val expectedResult = RecordTimeResult(id = null, status = StatusEnum.SUCCESS)

        val actualResult = utils.recordTime(entry)

        assertEquals("expect to see a success indicator", expectedResult, actualResult)
    }

    /**
     * Negative case - what happens if we receive a request to enter
     * time for an invalid project?
     */
    @Test
    fun `Should fail to record time for non-existent project`() {
        // it's an invalid project because the project doesn't exist
        val fakeTimeEntryPersistence = FakeTimeEntryPersistence(
                minutesRecorded = 60,
                persistNewTimeEntryBehavior = { throw ProjectIntegrityViolationException() })
        val utils = TimeRecordingUtilities(fakeTimeEntryPersistence)
        val entry = createTimeEntryPreDatabase(project= Project(1, "an invalid project"))
        val expectedResult = RecordTimeResult(id = null, status = StatusEnum.INVALID_PROJECT)

        val actualResult = utils.recordTime(entry)

        assertEquals("Expect to see a message about invalid project", expectedResult, actualResult)
    }

    /**
     * Negative case - what happens if we ask the system to record
     * any more time when we've already recorded the maximum for the day?
     * (It should throw an exception)
     */
    @Test
    fun `Should throw ExceededDailyHoursException when too asked to record more than 24 hours total in a day for 24 hours`() {
        val twentyFourHours: Int = 24 * 60
        val fakeTimeEntryPersistence = FakeTimeEntryPersistence(
                minutesRecorded = twentyFourHours,
                persistNewTimeEntryBehavior = { throw ProjectIntegrityViolationException() })
        val utils = TimeRecordingUtilities(fakeTimeEntryPersistence)
        val entry = createTimeEntryPreDatabase(time= Time(1), project= Project(1, "an invalid project"))

        assertThrows(ExceededDailyHoursAmountException::class.java) { utils.recordTime(entry) }
    }

    /**
     * Negative case - what happens if we ask the system to record
     * any more time when we've already recorded the maximum for the day?
     * (It should throw an exception)
     */
    @Test
    fun `Should throw ExceededDailyHoursException when too asked to record more than 24 hours total in a day for 23 hours`() {
        val twentyThreeHours: Int = 23 * 60
        val fakeTimeEntryPersistence = FakeTimeEntryPersistence(
                minutesRecorded = twentyThreeHours,
                persistNewTimeEntryBehavior = { throw ProjectIntegrityViolationException() })
        val utils = TimeRecordingUtilities(fakeTimeEntryPersistence)
        val entry = createTimeEntryPreDatabase(time= Time(60 * 2), project= Project(1, "an invalid project"))

        assertThrows(ExceededDailyHoursAmountException::class.java) { utils.recordTime(entry) }
    }


    @Test
    fun `just checking that two similar time entries are considered equal`() {
        assertEquals(createTimeEntryPreDatabase(), createTimeEntryPreDatabase())
    }

    /**
     * Now, this is something functional.  On the input, we want various
     * data, on the output we want a nice data object that has the relevant
     * interrelated data
     */
    @Test
    fun `make time entry`() {
        val timeEntry = createTimeEntryPreDatabase(date = A_RANDOM_DAY_IN_JUNE_2020)
        assertEquals(Employee(1, "I"), timeEntry.employee)
        assertEquals(Time(60), timeEntry.time)
        assertEquals(Project(1, "A"), timeEntry.project)
        assertEquals(Details(), timeEntry.details)
    }

    @Test
    fun `a employee should have a unique integer identifier`() {
        val employee = Employee(1, "someone")
        assertEquals(1, employee.id)
    }

    @Test
    fun `a employee should have a name`() {
        val name = "this is my name bro"
        val id = 1

        val employee = Employee(id, name)

        assertEquals(id, employee.id)
        assertEquals(name, employee.name)
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
     * Both the database the type disallow a details message longer than some length of characters.
     * Testing that 1 more throws an exception
     */
    @Test fun `Details should throw an exception if one longer than max-length characters`() {
        val ex = assertThrows(AssertionError::class.java ) { Details("A".repeat(MAX_DETAILS_LENGTH + 1)) }
        assertTrue(ex.message.toString().contains("lord's prayer"))
    }

    /**
     * Both the database the type disallow a details message longer than 500 characters.
     * Testing that 499 doesn't throw an exception
     */
    @Test fun `Details should allow an input of max-length minus 1 characters`() {
        val value = "A".repeat(MAX_DETAILS_LENGTH - 1)
        val details = Details(value)
        assertEquals(value, details.value)
    }

    /**
     * Both the database the type disallow a details message longer than 500 characters.
     * Testing that max length doesn't throw an exception
     */
    @Test fun `Details should allow an input of max-length characters`() {
        val value = "A".repeat(MAX_DETAILS_LENGTH)
        val details = Details(value)
        assertEquals(value, details.value)
    }

    @Test fun `there should be no difference between details with no args and details with ""`() {
        val actual = Details("")
        val expected = Details()
        assertEquals(expected, actual)
    }

    @Test fun `Can't record a time entry that has 0 minutes`() {
        val ex = assertThrows(AssertionError::class.java ) { Time(0) }
        assertTrue(ex.message.toString().contains("Doesn't make sense to have zero or negative time"))
    }

    @Test fun `Can't record a time entry that has -1 minutes`() {
        val ex = assertThrows(AssertionError::class.java ) { Time(-1) }
        assertTrue(ex.message.toString().contains("Doesn't make sense to have zero or negative time"))
    }

    /**
     * a basic happy path
     */

    @Test fun `can create project`() {
        val fakeTimeEntryPersistence = FakeTimeEntryPersistence(
                persistNewProjectBehavior = { Project(1, "test project") })
        val utils = TimeRecordingUtilities(fakeTimeEntryPersistence)
        val expected = utils.createProject(ProjectName("test project"))
        val actual = Project(1, "test project")
        assertEquals(expected, actual)
    }

}
