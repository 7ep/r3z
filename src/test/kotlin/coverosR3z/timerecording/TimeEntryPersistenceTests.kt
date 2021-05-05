package coverosR3z.timerecording

import coverosR3z.system.misc.*
import coverosR3z.system.misc.types.Date
import coverosR3z.persistence.utility.PureMemoryDatabase.Companion.createEmptyDatabase
import coverosR3z.timerecording.exceptions.MultipleSubmissionsInPeriodException
import coverosR3z.timerecording.persistence.ITimeEntryPersistence
import coverosR3z.timerecording.persistence.TimeEntryPersistence
import coverosR3z.timerecording.types.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.experimental.categories.Category
import java.lang.IllegalArgumentException

@Category(IntegrationTestCategory::class)
class TimeEntryPersistenceTests {

    private lateinit var tep : ITimeEntryPersistence

    @Before fun init() {
        tep = TimeEntryPersistence(createEmptyDatabase(), logger = testLogger)
    }

    @Test fun `can record a time entry to the database`() {
        val newProject = tep.persistNewProject(DEFAULT_PROJECT_NAME)
        val newEmployee = tep.persistNewEmployee(DEFAULT_EMPLOYEE_NAME)
        tep.persistNewTimeEntry(createTimeEntryPreDatabase(project = newProject, employee = newEmployee))
        val count = tep.readTimeEntries(newEmployee).size
        assertEquals("There should be exactly one entry in the database", 1, count)
    }

    @Test fun `can get all time entries by a employee`() {
        tep.persistNewEmployee(DEFAULT_EMPLOYEE_NAME)
        val newProject: Project = tep.persistNewProject(DEFAULT_PROJECT_NAME)
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
    @Test fun `Can't record a time entry that has a nonexistent project id`() {
        assertThrows(IllegalStateException::class.java) {
            tep.persistNewTimeEntry(createTimeEntryPreDatabase())
        }
    }

    /**
     * We need to be able to know how many hours a employee has worked for the purpose of validation
     */
    @Test
    fun `Can query hours worked by a employee on a given day`() {
        val newProject = tep.persistNewProject(DEFAULT_PROJECT_NAME)
        val newEmployee = tep.persistNewEmployee(DEFAULT_EMPLOYEE_NAME)
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

    @Test
    fun `if a employee has not worked on a given day, we return 0 as their minutes worked that day`() {
        val newEmployee: Employee = tep.persistNewEmployee(DEFAULT_EMPLOYEE_NAME)
        val minutesWorked = tep.queryMinutesRecorded(employee=newEmployee, date= A_RANDOM_DAY_IN_JUNE_2020)

        assertEquals("should be 0 since they didn't work that day", Time(0), minutesWorked)
    }

    @Test
    fun `If a employee worked 24 hours total in a day, we should get that from queryMinutesRecorded`() {
        val newProject = tep.persistNewProject(DEFAULT_PROJECT_NAME)
        val newEmployee = tep.persistNewEmployee(DEFAULT_EMPLOYEE_NAME)
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


    @Test
    fun `If a employee worked 8 hours a day for two days, we should get just 8 hours when checking one of those days`() {
        val newProject = tep.persistNewProject(DEFAULT_PROJECT_NAME)
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

    @Test
    fun `should be able to add a new project`() {
        tep.persistNewProject(DEFAULT_PROJECT_NAME)

        val project = tep.getProjectById(DEFAULT_PROJECT.id)

        assertEquals(ProjectId(1), project.id)
    }

    @Test
    fun `should be able to add a new employee`() {
        tep.persistNewEmployee(DEFAULT_EMPLOYEE_NAME)

        val employee = tep.getEmployeeById(DEFAULT_EMPLOYEE.id)

        assertEquals(1, employee.id.value)
    }

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
    @Test
    fun testShouldReturnEmptyListIfNoEntries() {
        val result = tep.readTimeEntriesOnDate(DEFAULT_EMPLOYEE, A_RANDOM_DAY_IN_JUNE_2020)
        assertEquals(emptySet<TimeEntry>() , result)
    }

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


    @Test
    fun testSubmittedTimePeriods_addingNew() {
        tep.persistNewSubmittedTimePeriod(DEFAULT_EMPLOYEE, DEFAULT_TIME_PERIOD)

        val result = tep.isInASubmittedPeriod(DEFAULT_EMPLOYEE, Date.make("2021-02-03"))
        val expected = true
        assertEquals(expected, result)
    }

    @Test
    fun testSubmittedTimePeriods_noneFound() {
        val result = tep.isInASubmittedPeriod(DEFAULT_EMPLOYEE, Date.make("2021-02-03"))
        assertFalse("nothing has been submitted, shouldn't be true", result)
    }

    @Test
    fun testGetSubmittedPeriod() {
        tep.persistNewSubmittedTimePeriod(DEFAULT_EMPLOYEE, DEFAULT_TIME_PERIOD)
        val submittedTimePeriod = tep.getSubmittedTimePeriod(DEFAULT_EMPLOYEE, DEFAULT_TIME_PERIOD)

        assertEquals(DEFAULT_SUBMITTED_PERIOD, submittedTimePeriod)
    }

    @Test
    fun testFailToGetNonexistentSubmission() {
        val result = tep.getSubmittedTimePeriod(DEFAULT_EMPLOYEE, DEFAULT_TIME_PERIOD)
        assertEquals(NullSubmittedPeriod, result)
    }

    @Test
    fun testCannotPersistTheSameSubmissionTwice() {
        tep.persistNewSubmittedTimePeriod(DEFAULT_EMPLOYEE, DEFAULT_TIME_PERIOD)
        val ex = assertThrows(MultipleSubmissionsInPeriodException::class.java) { tep.persistNewSubmittedTimePeriod(DEFAULT_EMPLOYEE, DEFAULT_TIME_PERIOD) }
        assertEquals("A submission already exists for ${DEFAULT_EMPLOYEE.name.value} on $DEFAULT_TIME_PERIOD", ex.message)
    }

    @Test
    fun testGetTimeEntriesForPeriod() {
        val (_, employee, inputTimeEntry) = createTimeEntry()

        val result : Set<TimeEntry> = tep.getTimeEntriesForTimePeriod(employee, DEFAULT_TIME_PERIOD)

        assertEquals(inputTimeEntry, result.first())
    }

    /**
     * If I only have a time entry that is outside the time period,
     * we'll find nothing.
     */
    @Test
    fun testGetTimeEntriesForPeriod_outOfRange() {
        val (_, employee, _) = createTimeEntry(date = A_RANDOM_DAY_IN_JUNE_2020)

        val result : Set<TimeEntry> = tep.getTimeEntriesForTimePeriod(employee, DEFAULT_TIME_PERIOD)

        assertFalse("There should be no time entries found because they exist outside our time period", result.any())
    }

    /**
     * Successfully find a time entry
     */
    @Test
    fun testFindTimeEntry() {
        val (_, _, newTimeEntry) = createTimeEntry()
        val result = tep.findTimeEntryById(newTimeEntry.id)
        assertEquals(newTimeEntry, result)
    }

    /**
     * no time entry found
     */
    @Test
    fun testFindTimeEntry_NothingFound() {
        val result = tep.findTimeEntryById(DEFAULT_TIME_ENTRY.id)
        assertEquals(NO_TIMEENTRY, result)
    }

    /**
     * Successfully delete a time entry
     */
    @Test
    fun testDeleteTimeEntry() {
        val (_, _, newTimeEntry) = createTimeEntry()
        val result = tep.deleteTimeEntry(newTimeEntry)
        assertTrue(result)
        val findResult = tep.findTimeEntryById(newTimeEntry.id)
        assertEquals(NO_TIMEENTRY, findResult)
    }

    /**
     * no time entry found
     */
    @Test
    fun testDeleteTimeEntry_NothingFound() {
        val result = tep.deleteTimeEntry(DEFAULT_TIME_ENTRY)
        assertFalse(result)
    }

    @Test
    fun testApproveTime() {
        val employee = tep.persistNewEmployee(DEFAULT_EMPLOYEE_NAME)
        val stp: SubmittedPeriod = tep.persistNewSubmittedTimePeriod(employee, DEFAULT_TIME_PERIOD)
        assertEquals(ApprovalStatus.UNAPPROVED, stp.approvalStatus)
        assertTrue(tep.approveTimesheet(stp))
        val submittedPeriod = tep.getSubmittedTimePeriod(employee, DEFAULT_TIME_PERIOD)
        assertEquals(ApprovalStatus.APPROVED, submittedPeriod.approvalStatus)
    }

    @Test
    fun testGetEmployeeByName() {
        tep.persistNewEmployee(DEFAULT_EMPLOYEE_NAME)
        val result: Employee = tep.getEmployeeByName(DEFAULT_EMPLOYEE_NAME)
        assertEquals(DEFAULT_EMPLOYEE, result)
    }

    /**
     * If we successfully remove a project, we get true back
     */
    @Test
    fun testDeleteProject() {
        val project = tep.persistNewProject(DEFAULT_PROJECT_NAME)

        val result = tep.deleteProject(project)

        assertTrue(result)
    }

    /**
     * If we try to delete a project and it fails (this means
     * the project didn't exist in the set), we get false back
     */
    @Test
    fun testDeleteProject_NoProjectFound() {
        val result = tep.deleteProject(DEFAULT_PROJECT)

        assertFalse(result)
    }

    /**
     * If somehow [NO_PROJECT] is passed into this method,
     * we should throw an exception, since there is no valid
     * situation where we can handle that.
     */
    @Test
    fun testDeleteProject_PassInNoProject() {
        assertThrows(IllegalArgumentException::class.java) { tep.deleteProject(NO_PROJECT) }
    }

    /**
     * Check whether a particular project is used in *any* time entry
     * This is a check run when deleting a project, since if it
     * has been used anywhere, we cannot delete it.
     */
    @Test
    fun testIfProjectUsedInTimeEntry() {
        val (project, _, _) = createTimeEntry()
        assertTrue(tep.isProjectUsedForTimeEntry(project))
    }

    /**
     * Similar to [testIfProjectUsedInTimeEntry] but none found
     */
    @Test
    fun testIfProjectUsedInTimeEntry_NoneFound() {
        val (_, _, _) = createTimeEntry()
        assertFalse(tep.isProjectUsedForTimeEntry(DEFAULT_PROJECT_2))
    }

    /**
     * Similar to [testIfProjectUsedInTimeEntry] but if [NO_PROJECT]
     * is passed in, we'll throw an exception - there must be
     * in that case something coded wrong or something, because the only
     * correct answer for NO_PROJECT being in the time entries is to
     * throw an exception
     */
    @Test
    fun testIfProjectUsedInTimeEntry_SearchingNoProject() {
        assertThrows(IllegalArgumentException::class.java) { tep.isProjectUsedForTimeEntry(NO_PROJECT) }
    }


    /*
     _ _       _                  __ __        _    _           _
    | | | ___ | | ___  ___  _ _  |  \  \ ___ _| |_ | |_  ___  _| | ___
    |   |/ ._>| || . \/ ._>| '_> |     |/ ._> | |  | . |/ . \/ . |<_-<
    |_|_|\___.|_||  _/\___.|_|   |_|_|_|\___. |_|  |_|_|\___/\___|/__/
                 |_|
     alt-text: Helper Methods
     */

    private fun createTimeEntry(
        date: Date = DEFAULT_PERIOD_START_DATE,
        projectName: ProjectName = DEFAULT_PROJECT_NAME,
        employeeName: EmployeeName = DEFAULT_EMPLOYEE_NAME) : Triple<Project, Employee, TimeEntry> {
        val project = tep.persistNewProject(projectName)
        val employee = tep.persistNewEmployee(employeeName)
        val timeEntry = tep.persistNewTimeEntry(
            createTimeEntryPreDatabase(
                project = project,
                employee = employee,
                date = date))
        return Triple(project, employee, timeEntry)
    }

}