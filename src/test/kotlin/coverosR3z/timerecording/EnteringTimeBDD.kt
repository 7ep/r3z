package coverosR3z.timerecording

import coverosR3z.*
import coverosR3z.domainobjects.*
import coverosR3z.exceptions.ExceededDailyHoursAmountException
import coverosR3z.logging.logInfo
import org.junit.Assert.*
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

    /**
     * Just a happy path for entering a time entry
     */
    @Test
    fun `capability to enter time`() {
        val (expectedStatus, tru, entry) = `given I have worked 1 hour on project "A" on Monday`()

        // when I enter in that time
        val recordStatus = tru.recordTime(entry)

        // then the system indicates it has persisted the new information
        assertEquals("the system indicates it has persisted the new information", expectedStatus, recordStatus)
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

    private fun `given I have worked 1 hour on project "A" on Monday`(): Triple<RecordTimeResult, TimeRecordingUtilities, TimeEntryPreDatabase> {
        val expectedStatus = RecordTimeResult(null, StatusEnum.SUCCESS)
        val tru = createTimeRecordingUtility()
        val newProject: Project = tru.createProject(ProjectName("A"))
        val newEmployee: Employee = tru.createEmployee(EmployeeName("B"))
        val entry = createTimeEntryPreDatabase(project = newProject, employee = newEmployee)
        return Triple(expectedStatus, tru, entry)
    }

    private fun `given I have worked 6 hours on project "A" on Monday with a lot of notes`(): Triple<TimeRecordingUtilities, TimeEntryPreDatabase, RecordTimeResult> {
        val tru = createTimeRecordingUtility()
        val newProject: Project = tru.createProject(DEFAULT_PROJECT_NAME)
        val newEmployee : Employee = tru.createEmployee(DEFAULT_EMPLOYEENAME)
        val entry = createTimeEntryPreDatabase(
                employee = newEmployee,
                project = newProject,
                time = Time(60 * 6),
                details = Details("Four score and seven years ago, blah blah blah".repeat(10))
        )
        val expectedStatus = RecordTimeResult(null, StatusEnum.SUCCESS)
        return Triple(tru, entry, expectedStatus)
    }

    private fun `given the employee has already entered 24 hours of time entries before`(): Triple<TimeRecordingUtilities, Project, Employee> {
        val tru = createTimeRecordingUtility()
        val newProject: Project = tru.createProject(ProjectName("A"))
        val newEmployee: Employee = tru.createEmployee(EmployeeName("B"))
        val existingTimeForTheDay = createTimeEntryPreDatabase(employee = newEmployee, project = newProject, time = Time(60 * 24))
        tru.recordTime(existingTimeForTheDay)
        return Triple(tru, newProject, newEmployee)
    }



}