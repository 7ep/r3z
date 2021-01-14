package coverosR3z.timerecording

import coverosR3z.authentication.types.CurrentUser
import coverosR3z.authentication.types.SYSTEM_USER
import coverosR3z.logging.resetLogSettingsToDefault
import coverosR3z.logging.turnOnAllLogging
import coverosR3z.misc.*
import coverosR3z.persistence.utility.PureMemoryDatabase
import coverosR3z.timerecording.exceptions.ExceededDailyHoursAmountException
import coverosR3z.timerecording.persistence.TimeEntryPersistence
import coverosR3z.timerecording.types.*
import coverosR3z.timerecording.utility.TimeRecordingUtilities
import org.junit.Assert.*
import org.junit.Test

class TimeRecordingUtilityTests {

    /**
     * Happy path - record time successfully
     */
    @Test
    fun `record time for someone`() {
        val fakeTimeEntryPersistence = FakeTimeEntryPersistence(minutesRecorded = Time(60))
        val utils = TimeRecordingUtilities(fakeTimeEntryPersistence, CurrentUser(SYSTEM_USER))
        val entry = createTimeEntryPreDatabase()

        val actualResult = utils.recordTime(entry)

        assertEquals("expect to see a success indicator", StatusEnum.SUCCESS, actualResult.status)
    }

    /**
     * Negative case - what happens if we receive a request to enter
     * time for an invalid project?
     */
    @IntegrationTest
    @Test
    fun `Should fail to record time for non-existent project`() {
        // it's an invalid project because the project doesn't exist
        val pmd = PureMemoryDatabase()
        val tep = TimeEntryPersistence(pmd)
        val utils = TimeRecordingUtilities(tep, CurrentUser(SYSTEM_USER))
        val entry = createTimeEntryPreDatabase(project= Project(ProjectId(1), ProjectName("an invalid project")))
        val expectedResult = RecordTimeResult(StatusEnum.INVALID_PROJECT)

        val actualResult = utils.recordTime(entry)

        assertEquals("Expect to see a message about invalid project", expectedResult, actualResult)
    }

    /**
     * Negative case - what happens if we receive a request to enter
     * time for an invalid employee?
     */
    @IntegrationTest
    @Test
    fun `Should fail to record time for non-existent employee`() {
        // it's an invalid project because the project doesn't exist
        val pmd = PureMemoryDatabase()
        val tep = TimeEntryPersistence(pmd)
        val utils = TimeRecordingUtilities(tep, CurrentUser(SYSTEM_USER))
        val project = utils.createProject(DEFAULT_PROJECT_NAME)
        val entry = createTimeEntryPreDatabase(project = project)
        val expectedResult = RecordTimeResult(StatusEnum.INVALID_EMPLOYEE)

        val actualResult = utils.recordTime(entry)

        assertEquals("Expect to see a message about invalid employee", expectedResult, actualResult)
    }

    /**
     * Negative case - what happens if we ask the system to record
     * any more time when we've already recorded the maximum for the day?
     * (It should throw an exception)
     */
    @IntegrationTest
    @Test
    fun `Should throw ExceededDailyHoursException when too asked to record more than 24 hours total in a day for 24 hours`() {
        val twentyFourHours = Time(24 * 60)
        val pmd = PureMemoryDatabase()
        val tep = TimeEntryPersistence(pmd)
        val utils = TimeRecordingUtilities(tep, CurrentUser(SYSTEM_USER))
        val project = utils.createProject(DEFAULT_PROJECT_NAME)
        val employee = utils.createEmployee(DEFAULT_EMPLOYEE_NAME)
        val entry1 = createTimeEntryPreDatabase(time= twentyFourHours, project= project, employee = employee)
        utils.recordTime(entry1)
        val entry2 = createTimeEntryPreDatabase(time= Time(1), project= project, employee = employee)

        assertThrows(ExceededDailyHoursAmountException::class.java) { utils.recordTime(entry2) }
    }

    /**
     * Negative case - what happens if we ask the system to record
     * any more time when we've already recorded the maximum for the day?
     * (It should throw an exception)
     */
    @Test
    fun `Should throw ExceededDailyHoursException when too asked to record more than 24 hours total in a day for 23 hours`() {
        val twentyThreeHours = Time(23 * 60)
        val pmd = PureMemoryDatabase()
        val tep = TimeEntryPersistence(pmd)
        val utils = TimeRecordingUtilities(tep, CurrentUser(SYSTEM_USER))
        val project = utils.createProject(DEFAULT_PROJECT_NAME)
        val employee = utils.createEmployee(DEFAULT_EMPLOYEE_NAME)
        val entry1 = createTimeEntryPreDatabase(time= twentyThreeHours, project= project, employee = employee)
        utils.recordTime(entry1)
        val entry2 = createTimeEntryPreDatabase(time= Time(60 * 2), project= project, employee = employee)

        assertThrows(ExceededDailyHoursAmountException::class.java) { utils.recordTime(entry2) }
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
    fun `creating a time entry should record its details`() {
        val timeEntry = createTimeEntryPreDatabase(date = A_RANDOM_DAY_IN_JUNE_2020)
        assertEquals(DEFAULT_EMPLOYEE, timeEntry.employee)
        assertEquals(DEFAULT_TIME, timeEntry.time)
        assertEquals(DEFAULT_PROJECT, timeEntry.project)
        assertEquals(Details(), timeEntry.details)
    }

    @Test
    fun `a employee should have a unique integer identifier`() {
        val employee = Employee(EmployeeId(1), EmployeeName("someone"))
        assertEquals(1, employee.id.value)
    }

    @Test
    fun `a employee should have a name`() {
        val name = "this is my name bro"
        val id = 1

        val employee = Employee(EmployeeId(id), EmployeeName(name))

        assertEquals(id, employee.id.value)
        assertEquals(name, employee.name.value)
    }

    @Test
    fun `a time should contain an integer number of minutes`() {
        val time = Time((60 * 3) + 15)
        assertEquals(THREE_HOURS_FIFTEEN, time)
    }

    @Test
    fun `a project should have a name and an id`() {
        val project = Project(ProjectId(1), ProjectName("some project name"))
        assertEquals(ProjectId(1), project.id)
        assertEquals(ProjectName("some project name"), project.name)
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
        val ex = assertThrows(IllegalArgumentException::class.java ) { Details("A".repeat(MAX_DETAILS_LENGTH + 1)) }
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

    @Test fun `there should be no difference between details with no args and details with empty string`() {
        val actual = Details("")
        val expected = Details()
        assertEquals(expected, actual)
    }

    /**
     * a basic happy path
     */
    @Test fun `can create project`() {
        val fakeTimeEntryPersistence = FakeTimeEntryPersistence(
                persistNewProjectBehavior = { DEFAULT_PROJECT })
        val utils = TimeRecordingUtilities(fakeTimeEntryPersistence, CurrentUser(SYSTEM_USER))
        val expected = utils.createProject(DEFAULT_PROJECT_NAME)
        assertEquals(expected, DEFAULT_PROJECT)
    }

    /**
     * Trying to create an already-existing project should throw exception
     */
    @Test fun testCannotCreateMultipleProjectsWithSameName() {
        val fakeTimeEntryPersistence = FakeTimeEntryPersistence(
                getProjectByNameBehavior = { DEFAULT_PROJECT })
        val utils = TimeRecordingUtilities(fakeTimeEntryPersistence, CurrentUser(SYSTEM_USER))
        val ex  = assertThrows(java.lang.IllegalArgumentException::class.java) {utils.createProject(DEFAULT_PROJECT_NAME)}
        assertEquals("Cannot create a new project if one already exists by that same name", ex.message)
    }

    /**
     * List all of two projects
     */
    @Test fun testCanListAllProjects() {
        val utils = TimeRecordingUtilities(TimeEntryPersistence(PureMemoryDatabase()), CurrentUser(SYSTEM_USER))
        val expectedList = mutableListOf<Project>()
        expectedList.add(utils.createProject(DEFAULT_PROJECT_NAME))
        expectedList.add(utils.createProject(ProjectName("second")))
        val allProjects : List<Project> = utils.listAllProjects()
        assertEquals(expectedList, allProjects)
    }

    /**
     * List all and no projects in database - [emptyList]
     */
    @Test fun testCanListAllProjects_NoProjects() {
        val utils = TimeRecordingUtilities(TimeEntryPersistence(PureMemoryDatabase()), CurrentUser(SYSTEM_USER))
        val allProjects : List<Project> = utils.listAllProjects()
        assertEquals(emptyList<Project>(), allProjects)
    }

    /**
     * happy path
     */
    @Test fun testCanGetProjectById() {
        val utils = TimeRecordingUtilities(TimeEntryPersistence(PureMemoryDatabase()), CurrentUser(SYSTEM_USER))
        val createdProject = utils.createProject(DEFAULT_PROJECT_NAME)
        val foundProject = utils.findProjectById(createdProject.id)
        assertEquals(createdProject, foundProject)
    }

    /**
     * what if the project doesn't exist? [NO_PROJECT]
     */
    @Test fun testCanGetProjectById_NotFound() {
        val utils = TimeRecordingUtilities(TimeEntryPersistence(PureMemoryDatabase()), CurrentUser(SYSTEM_USER))
        val foundProject = utils.findProjectById(ProjectId(1))
        assertEquals(NO_PROJECT, foundProject)
    }

    /**
     * Given we have a time entry in the database, let's
     * edit its values
     */
    @Test
    fun testCanEditTimeEntry() {
        // arrange
        turnOnAllLogging()
        val tru = TimeRecordingUtilities(TimeEntryPersistence(PureMemoryDatabase()), CurrentUser(DEFAULT_USER))
        tru.createProject(DEFAULT_PROJECT_NAME)
        tru.createEmployee(DEFAULT_EMPLOYEE_NAME)
        val (_, newTimeEntry) = tru.recordTime(createTimeEntryPreDatabase(time = Time(1)))
        val expected = RecordTimeResult(StatusEnum.SUCCESS,
            TimeEntry(TimeEntryId(1), DEFAULT_EMPLOYEE, DEFAULT_PROJECT, Time(2), A_RANDOM_DAY_IN_JUNE_2020))

        // act
        val actual: RecordTimeResult = tru.changeEntry(newTimeEntry!!.copy(time = Time(2)))

        // assert
        assertEquals(expected, actual)
        resetLogSettingsToDefault()
    }

    /**
     * Nothing much different should take place if we're overwriting
     * with the exact same values, just want to make that explicit in a test
     */
    @Test
    fun testCanEditTimeEntry_Unchanged() {
        // arrange
        val tru = TimeRecordingUtilities(TimeEntryPersistence(PureMemoryDatabase()), CurrentUser(DEFAULT_USER))
        tru.createProject(DEFAULT_PROJECT_NAME)
        tru.createEmployee(DEFAULT_EMPLOYEE_NAME)
        val result = tru.recordTime(createTimeEntryPreDatabase(time = Time(1)))

        // act
        val actual: RecordTimeResult = tru.changeEntry(result.newTimeEntry!!)

        // assert
        assertEquals(result, actual)
    }

    /**
     * Someone who isn't this employee cannot change the time entry
     */
    @Test
    fun testCanEditTimeEntry_DisallowDifferentEmployeeToChange() {
        // arrange
        val tru = TimeRecordingUtilities(TimeEntryPersistence(PureMemoryDatabase()), CurrentUser(DEFAULT_USER))
        tru.createProject(DEFAULT_PROJECT_NAME)
        tru.createEmployee(DEFAULT_EMPLOYEE_NAME)
        val result: RecordTimeResult = tru.recordTime(createTimeEntryPreDatabase(time = Time(1)))
        val truOtherUser = TimeRecordingUtilities(TimeEntryPersistence(PureMemoryDatabase()), CurrentUser(DEFAULT_USER_2))
        val expected = RecordTimeResult(status= StatusEnum.USER_EMPLOYEE_MISMATCH, newTimeEntry=null)

        // act
        val actual: RecordTimeResult = truOtherUser.changeEntry(result.newTimeEntry!!)

        // assert
        assertEquals(expected, actual)
    }

    @Test
    fun testCanEditTimeEntry_InvalidProject() {
        // arrange
        val tru = TimeRecordingUtilities(TimeEntryPersistence(PureMemoryDatabase()), CurrentUser(DEFAULT_USER))
        tru.createEmployee(DEFAULT_EMPLOYEE_NAME)
        tru.createProject(DEFAULT_PROJECT_NAME)
        val (_, newTimeEntry) = tru.recordTime(createTimeEntryPreDatabase(time = Time(1)))
        val expected = RecordTimeResult(status= StatusEnum.INVALID_PROJECT, newTimeEntry=null)

        val result = tru.changeEntry(newTimeEntry!!.copy(project = Project(ProjectId(5), ProjectName("fake"))))

        assertEquals(expected, result)
    }

}
