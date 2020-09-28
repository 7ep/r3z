package coverosR3z.timerecording

import coverosR3z.*
import coverosR3z.authentication.CurrentUserAccessor
import coverosR3z.domainobjects.*
import coverosR3z.exceptions.ExceededDailyHoursAmountException
import coverosR3z.persistence.PureMemoryDatabase
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test


/**
 * Feature: Entering time
 *
 * Employee story:
 *      As an employee
 *      I want to record my time
 *      So that I am easily able to document my time in an organized way
 */
class EnteringTimeBDD {

    val currentUserAccessor = CurrentUserAccessor()

    @Before
    fun init() {
        currentUserAccessor.clearCurrentUserTestOnly()
    }

    /**
     * Just a happy path for entering a time entry
     */
    @Test
    fun `I can add a time entry`() {
        // Given I am logged in
        val (tru, _) = initializeAUserAndLogin(currentUserAccessor)

        // When I add a time entry
        val entry = createTimeEntryPreDatabase(Employee(1, "Alice"))
        val result = tru.recordTime(entry)

        // Then it proceeds successfully
        assertEquals(RecordTimeResult(StatusEnum.SUCCESS), result)
    }

    /**
     * Just another flavor of happy path
     */
    @Test
    fun `A employee enters six hours on a project with copious notes`() {
        val (tru, entry, expectedStatus) = `given I have worked 6 hours on project "A" on Monday with a lot of notes`()

        // when I enter in that time
        val recordStatus = tru.recordTime(entry)

        // then the system indicates it has persisted the new information
        assertEquals("the system indicates it has persisted the new information", expectedStatus, recordStatus)
    }

    @Test
    fun `A employee has already entered 24 hours for the day, they cannot enter more time on a new entry`() {
        val (tru, newProject: Project, newEmployee: Employee) = `given the employee has already entered 24 hours of time entries before`()

        // when they enter in a new time entry for one hour
        val entry = createTimeEntryPreDatabase(time = Time(30), project = newProject, employee = newEmployee)

        // then the system disallows it
        assertThrows(ExceededDailyHoursAmountException::class.java) { tru.recordTime(entry) }
    }

    /*
     _ _       _                  __ __        _    _           _
    | | | ___ | | ___  ___  _ _  |  \  \ ___ _| |_ | |_  ___  _| | ___
    |   |/ ._>| || . \/ ._>| '_> |     |/ ._> | |  | . |/ . \/ . |<_-<
    |_|_|\___.|_||  _/\___.|_|   |_|_|_|\___. |_|  |_|_|\___/\___|/__/
                 |_|
     alt-text: Helper Methods
     */

    private fun `given I have worked 1 hour on project "A" on Monday`(): Triple<RecordTimeResult, TimeRecordingUtilities, TimeEntryPreDatabase> {
        val expectedStatus = RecordTimeResult(StatusEnum.SUCCESS)
        val tru = createTimeRecordingUtility()
        val newProject: Project = tru.createProject(ProjectName("A"))
        val newEmployee: Employee = tru.createEmployee(EmployeeName("B"))
        val entry = createTimeEntryPreDatabase(project = newProject, employee = newEmployee)
        return Triple(expectedStatus, tru, entry)
    }

    private fun `given I have worked 6 hours on project "A" on Monday with a lot of notes`(): Triple<TimeRecordingUtilities, TimeEntryPreDatabase, RecordTimeResult> {
        val tru = createTimeRecordingUtility()
        val newProject: Project = tru.createProject(DEFAULT_PROJECT_NAME)
        val newEmployee : Employee = tru.createEmployee(DEFAULT_EMPLOYEE_NAME)
        val entry = createTimeEntryPreDatabase(
                employee = newEmployee,
                project = newProject,
                time = Time(60 * 6),
                details = Details("Four score and seven years ago, blah blah blah".repeat(10))
        )
        val expectedStatus = RecordTimeResult(StatusEnum.SUCCESS)
        return Triple(tru, entry, expectedStatus)
    }

    private fun `given the employee has already entered 24 hours of time entries before`(): Triple<TimeRecordingUtilities, Project, Employee> {
        currentUserAccessor.clearCurrentUserTestOnly()
        currentUserAccessor.set(User(1, "Zim", Hash.createHash(""), "", 1))
        val timeEntryPersistence : ITimeEntryPersistence = TimeEntryPersistence(PureMemoryDatabase())
        val tru = TimeRecordingUtilities(timeEntryPersistence, currentUserAccessor)
        val newProject: Project = tru.createProject(ProjectName("A"))
        val newEmployee: Employee = tru.createEmployee(EmployeeName("B"))
        val existingTimeForTheDay = createTimeEntryPreDatabase(employee = newEmployee, project = newProject, time = Time(60 * 24))
        tru.recordTime(existingTimeForTheDay)
        return Triple(tru, newProject, newEmployee)
    }

}