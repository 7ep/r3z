package coverosR3z.timerecording

import coverosR3z.misc.*
import coverosR3z.misc.types.Date
import coverosR3z.persistence.utility.PureMemoryDatabase.Companion.createEmptyDatabase
import coverosR3z.timerecording.exceptions.MultipleSubmissionsInPeriodException
import coverosR3z.timerecording.exceptions.SubmissionNotFoundException
import coverosR3z.timerecording.persistence.ITimeEntryPersistence
import coverosR3z.timerecording.persistence.TimeEntryPersistence
import coverosR3z.timerecording.types.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.experimental.categories.Category

class TimeEntryPersistenceTests {

    private lateinit var tep : ITimeEntryPersistence

    @Before fun init() {
        tep = TimeEntryPersistence(createEmptyDatabase(), logger = testLogger)
    }

    @Category(IntegrationTestCategory::class)
    @Test fun `can record a time entry to the database`() {
        val newProject = tep.persistNewProject(ProjectName("test project"))
        val newEmployee = tep.persistNewEmployee(EmployeeName("test employee"))
        tep.persistNewTimeEntry(createTimeEntryPreDatabase(project = newProject, employee = newEmployee))
        val count = tep.readTimeEntries(newEmployee).size
        assertEquals("There should be exactly one entry in the database", 1, count)
    }

    @Category(IntegrationTestCategory::class)
    @Test fun `can get all time entries by a employee`() {
        tep.persistNewEmployee(DEFAULT_EMPLOYEE_NAME)
        val newProject: Project = tep.persistNewProject(ProjectName("test project"))
        val entry1 = createTimeEntryPreDatabase(employee = DEFAULT_EMPLOYEE, project = newProject, date = A_RANDOM_DAY_IN_JUNE_2020_PLUS_ONE)
        val entry2 = createTimeEntryPreDatabase(employee = DEFAULT_EMPLOYEE, project = newProject, date = A_RANDOM_DAY_IN_JUNE_2020)
        tep.persistNewTimeEntry(entry1)
        tep.persistNewTimeEntry(entry2)
        val expectedResult = listOf(entry1, entry2)

        val actualResult = tep.readTimeEntries(DEFAULT_EMPLOYEE)

        val msg = "what we entered and what we get back should be identical, instead got"
        val listOfResultsMinusId = actualResult.map { r -> TimeEntryPreDatabase(r.employee, r.project, r.time, r.date, r.details) }.toList()
        assertEquals(msg, expectedResult, listOfResultsMinusId)

    }

    /**
     * If we try to add a time entry with a project id that doesn't exist in
     * the database, we should get an exception back from the database
     */
    @Category(IntegrationTestCategory::class)
    @Test fun `Can't record a time entry that has a nonexistent project id`() {
        assertThrows(IllegalStateException::class.java) {
            tep.persistNewTimeEntry(createTimeEntryPreDatabase())
        }
    }

    /**
     * We need to be able to know how many hours a employee has worked for the purpose of validation
     */
    @Category(IntegrationTestCategory::class)
    @Test
    fun `Can query hours worked by a employee on a given day`() {
        val newProject = tep.persistNewProject(ProjectName("test project"))
        val newEmployee = tep.persistNewEmployee(EmployeeName("test employee"))
        tep.persistNewTimeEntry(
                createTimeEntryPreDatabase(
                        employee = newEmployee,
                        time = Time(60),
                        project = newProject,
                        date = A_RANDOM_DAY_IN_JUNE_2020
                )
        )

        val query = tep.queryMinutesRecorded(employee=newEmployee, date= A_RANDOM_DAY_IN_JUNE_2020)
        assertEquals(Time(60), query)
    }

    @Category(IntegrationTestCategory::class)
    @Test
    fun `if a employee has not worked on a given day, we return 0 as their minutes worked that day`() {
        val newEmployee: Employee = tep.persistNewEmployee(DEFAULT_EMPLOYEE_NAME)
        val minutesWorked = tep.queryMinutesRecorded(employee=newEmployee, date= A_RANDOM_DAY_IN_JUNE_2020)

        assertEquals("should be 0 since they didn't work that day", Time(0), minutesWorked)
    }

    @Category(IntegrationTestCategory::class)
    @Test
    fun `If a employee worked 24 hours total in a day, we should get that from queryMinutesRecorded`() {
        val newProject = tep.persistNewProject(ProjectName("test project"))
        val newEmployee = tep.persistNewEmployee(EmployeeName("test employee"))
        tep.persistNewTimeEntry(
                createTimeEntryPreDatabase(
                        employee = newEmployee,
                        time = Time(60),
                        project = newProject,
                        date = A_RANDOM_DAY_IN_JUNE_2020
                )
        )
        tep.persistNewTimeEntry(
                createTimeEntryPreDatabase(
                        employee = newEmployee,
                        time = Time(60 * 10),
                        project = newProject,
                        date = A_RANDOM_DAY_IN_JUNE_2020
                )
        )
        tep.persistNewTimeEntry(
                createTimeEntryPreDatabase(
                        employee = newEmployee,
                        time = Time(60 * 13),
                        project = newProject,
                        date = A_RANDOM_DAY_IN_JUNE_2020
                )
        )

        val query = tep.queryMinutesRecorded(employee=newEmployee, date= A_RANDOM_DAY_IN_JUNE_2020)

        assertEquals("we should get 24 hours worked for this day", Time(60 * 24), query)
    }


    @Category(IntegrationTestCategory::class)
    @Test
    fun `If a employee worked 8 hours a day for two days, we should get just 8 hours when checking one of those days`() {
        val newProject = tep.persistNewProject(ProjectName("test project"))
        val newEmployee = tep.persistNewEmployee(DEFAULT_EMPLOYEE_NAME)
        tep.persistNewTimeEntry(
                createTimeEntryPreDatabase(
                        employee = newEmployee,
                        time = Time(60 * 8),
                        project = newProject,
                        date = A_RANDOM_DAY_IN_JUNE_2020
                )
        )
        tep.persistNewTimeEntry(
                createTimeEntryPreDatabase(
                        employee = newEmployee,
                        time = Time(60 * 8),
                        project = newProject,
                        date = A_RANDOM_DAY_IN_JUNE_2020_PLUS_ONE
                )
        )

        val query = tep.queryMinutesRecorded(employee=newEmployee, date= A_RANDOM_DAY_IN_JUNE_2020)

        assertEquals("we should get 8 hours worked for this day", Time(60 * 8), query)
    }

    @Category(IntegrationTestCategory::class)
    @Test
    fun `should be able to add a new project`() {
        tep.persistNewProject(DEFAULT_PROJECT_NAME)

        val project = tep.getProjectById(DEFAULT_PROJECT.id)

        assertEquals(ProjectId(1), project.id)
    }

    @Category(IntegrationTestCategory::class)
    @Test
    fun `should be able to add a new employee`() {
        tep.persistNewEmployee(DEFAULT_EMPLOYEE_NAME)

        val employee = tep.getEmployeeById(DEFAULT_EMPLOYEE.id)

        assertEquals(1, employee.id.value)
    }

    @Category(IntegrationTestCategory::class)
    @Test
    fun `should be able to add a new time entry`() {
        val newProject = tep.persistNewProject(DEFAULT_PROJECT_NAME)
        val newEmployee = tep.persistNewEmployee(DEFAULT_EMPLOYEE_NAME)
        tep.persistNewTimeEntry(TimeEntryPreDatabase(newEmployee, newProject, DEFAULT_TIME, A_RANDOM_DAY_IN_JUNE_2020))

        val timeEntries = tep.readTimeEntriesOnDate(newEmployee, A_RANDOM_DAY_IN_JUNE_2020).first()

        assertEquals(DEFAULT_TIME_ENTRY.id, timeEntries.id)
        assertEquals(DEFAULT_EMPLOYEE, timeEntries.employee)
        assertEquals(DEFAULT_PROJECT, timeEntries.project)
        assertEquals(DEFAULT_TIME, timeEntries.time)
        assertEquals(A_RANDOM_DAY_IN_JUNE_2020, timeEntries.date)
    }


    /**
     * If I ask the database for all the time entries for a particular employee on
     * a date and there aren't any, I should get back an empty list, not a null.
     */
    @Category(IntegrationTestCategory::class)
    @Test
    fun testShouldReturnEmptyListIfNoEntries() {
        val result = tep.readTimeEntriesOnDate(DEFAULT_EMPLOYEE, A_RANDOM_DAY_IN_JUNE_2020)
        assertEquals(emptySet<TimeEntry>() , result)
    }

    @Category(IntegrationTestCategory::class)
    @Test
    fun testShouldEditTimeEntry() {
        // arrange an existing time entry and a modified time entry
        val startingTimeEntry = DEFAULT_TIME_ENTRY
        val revisedTimeEntry = startingTimeEntry.copy(time = Time(2))
        tep.persistNewProject(startingTimeEntry.project.name)
        tep.persistNewEmployee(startingTimeEntry.employee.name)
        tep.persistNewTimeEntry(startingTimeEntry.toTimeEntryPreDatabase())

        // overwrite it
        tep.overwriteTimeEntry(revisedTimeEntry)
        val readTimeEntry: TimeEntry = tep.readTimeEntriesOnDate(startingTimeEntry.employee, startingTimeEntry.date).single()

        assertEquals(revisedTimeEntry, readTimeEntry)
    }

    @Category(IntegrationTestCategory::class)
    @Test
    fun testShouldEditTimeEntry_NoChanges() {
        // arrange an existing time entry
        val startingTimeEntry = DEFAULT_TIME_ENTRY
        tep.persistNewProject(startingTimeEntry.project.name)
        tep.persistNewEmployee(startingTimeEntry.employee.name)
        tep.persistNewTimeEntry(startingTimeEntry.toTimeEntryPreDatabase())

        // overwrite it but causes no change
        tep.overwriteTimeEntry(startingTimeEntry)
        val readTimeEntry: TimeEntry = tep.readTimeEntriesOnDate(startingTimeEntry.employee, startingTimeEntry.date).single()

        assertEquals(startingTimeEntry, readTimeEntry)
    }

    /**
    The employee a user logs time to should not be able to be changed, since their user is bound to a single
    employee.
     */
    @Category(IntegrationTestCategory::class)
    @Test
    fun testCanEditTimeEntry_DisallowChangeToOtherEmployee() {
        // arrange an existing time entry and a modified time entry
        val startingTimeEntry = DEFAULT_TIME_ENTRY
        val newEmployee = Employee(EmployeeId(5), EmployeeName("alice"))
        val revisedTimeEntry = startingTimeEntry.copy(employee = newEmployee)
        tep.persistNewProject(startingTimeEntry.project.name)
        tep.persistNewEmployee(startingTimeEntry.employee.name)
        tep.persistNewTimeEntry(startingTimeEntry.toTimeEntryPreDatabase())

        // act and assert
        val ex = assertThrows(java.lang.IllegalStateException::class.java) {tep.overwriteTimeEntry(revisedTimeEntry)}
        assertEquals("a time entry with no employee is invalid", ex.message)
    }


    @Category(IntegrationTestCategory::class)
    @Test
    fun testSubmittedTimePeriods_addingNew() {
        tep.persistNewSubmittedTimePeriod(DEFAULT_EMPLOYEE, DEFAULT_TIME_PERIOD)

        val result = tep.isInASubmittedPeriod(DEFAULT_EMPLOYEE, Date.make("2021-02-03"))
        val expected = true
        assertEquals(expected, result)
    }

    @Category(IntegrationTestCategory::class)
    @Test
    fun testSubmittedTimePeriods_noneFound() {
        val result = tep.isInASubmittedPeriod(DEFAULT_EMPLOYEE, Date.make("2021-02-03"))
        assertFalse("nothing has been submitted, shouldn't be true", result)
    }

    @Category(IntegrationTestCategory::class)
    @Test
    fun testGetSubmittedPeriod() {
        tep.persistNewSubmittedTimePeriod(DEFAULT_EMPLOYEE, DEFAULT_TIME_PERIOD)
        val submittedTimePeriod = tep.getSubmittedTimePeriod(DEFAULT_EMPLOYEE, DEFAULT_TIME_PERIOD)

        assertEquals(DEFAULT_SUBMITTED_PERIOD, submittedTimePeriod)
    }

    @Category(IntegrationTestCategory::class)
    @Test
    fun testFailToGetNonexistentSubmission() {
        val ex = assertThrows(SubmissionNotFoundException::class.java) { tep.getSubmittedTimePeriod(DEFAULT_EMPLOYEE, DEFAULT_TIME_PERIOD)}
        assertEquals("no submission was found for DefaultEmployee on TimePeriod(start=Date(epochDay=18659, 2021-02-01), end=Date(epochDay=18673, 2021-02-15))", ex.message)
    }

    @Category(IntegrationTestCategory::class)
    @Test
    fun testCannotPersistTheSameSubmissionTwice() {
        tep.persistNewSubmittedTimePeriod(DEFAULT_EMPLOYEE, DEFAULT_TIME_PERIOD)
        val ex = assertThrows(MultipleSubmissionsInPeriodException::class.java) { tep.persistNewSubmittedTimePeriod(DEFAULT_EMPLOYEE, DEFAULT_TIME_PERIOD) }
        assertEquals("A submission already exists for ${DEFAULT_EMPLOYEE.name.value} on $DEFAULT_TIME_PERIOD", ex.message)
    }

    @Category(IntegrationTestCategory::class)
    @Test
    fun testGetTimeEntriesForPeriod() {
        val project = tep.persistNewProject(DEFAULT_PROJECT_NAME)
        val employee = tep.persistNewEmployee(DEFAULT_EMPLOYEE_NAME)
        val inputTimeEntry = tep.persistNewTimeEntry(
            createTimeEntryPreDatabase(
                project = project,
                employee = employee,
                date = DEFAULT_PERIOD_START_DATE))

        val result : Set<TimeEntry> = tep.getTimeEntriesForTimePeriod(employee, DEFAULT_TIME_PERIOD)

        assertEquals(inputTimeEntry, result.first())
    }

    /**
     * If I only have a time entry that is outside the time period,
     * we'll find nothing.
     */
    @Category(IntegrationTestCategory::class)
    @Test
    fun testGetTimeEntriesForPeriod_outOfRange() {
        val project = tep.persistNewProject(DEFAULT_PROJECT_NAME)
        val employee = tep.persistNewEmployee(DEFAULT_EMPLOYEE_NAME)
        tep.persistNewTimeEntry(
            createTimeEntryPreDatabase(
                project = project,
                employee = employee,
                date = A_RANDOM_DAY_IN_JUNE_2020))

        val result : Set<TimeEntry> = tep.getTimeEntriesForTimePeriod(employee, DEFAULT_TIME_PERIOD)

        assertFalse("There should be no time entries found because they exist outside our time period", result.any())
    }


}