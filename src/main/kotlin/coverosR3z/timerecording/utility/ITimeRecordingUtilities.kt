package coverosR3z.timerecording.utility

import coverosR3z.authentication.types.CurrentUser
import coverosR3z.system.misc.types.Date
import coverosR3z.timerecording.types.*

interface ITimeRecordingUtilities {

    /**
     * This allows us to create a new [TimeRecordingUtilities]
     * that is identical to the old except with a new [CurrentUser]
     * Only allowed by the System user
     */
    fun changeUser(cu : CurrentUser) : ITimeRecordingUtilities

    /**
     * Given a [TimeEntryPreDatabase], which has no id, check
     * to make sure it is a valid entry and if so, persist it.
     * See [changeEntry] and [deleteTimeEntry]
     */
    fun createTimeEntry(entry: TimeEntryPreDatabase): RecordTimeResult

    /**
     * This assumes you are changing your own time entries,
     * which implies you have an employee id.
     * See [createTimeEntry] and [deleteTimeEntry]
     */
    fun changeEntry(entry: TimeEntry): RecordTimeResult

    /**
     * Business code for creating a new project in the
     * system (persists it to the database)
     */
    fun createProject(projectName: ProjectName) : Project

    /**
     * Business code for creating a new employee in the
     * system (persists it to the database).
     * See [deleteEmployee]
     */
    fun createEmployee(employeename: EmployeeName) : Employee

    /**
     * Get the time entries for a particular employee on a particular date
     */
    fun getEntriesForEmployeeOnDate(employee: Employee, date: Date): Set<TimeEntry>

    /**
     * Simply get all the time entries for an employee
     */
    fun getAllEntriesForEmployee(employee: Employee): Set<TimeEntry>

    /**
     * List all the projects in the system
     */
    fun listAllProjects(): List<Project>

    /**
     * Given a project id, find the project associated with it
     * See [findProjectByName]
     */
    fun findProjectById(id: ProjectId): Project

    /**
     * Given an employee id, find the employee associated with it.
     * See [findEmployeeByName]
     */
    fun findEmployeeById(id: EmployeeId): Employee

    /**
     * Find an employee by name
     * See [findEmployeeById]
     */
    fun findEmployeeByName(name: EmployeeName): Employee

    /**
     * List all the employees in the system
     */
    fun listAllEmployees(): List<Employee>

    /**
     * Submit a [TimePeriod] for review by an approver.
     * See [unsubmitTimePeriod]
     */
    fun submitTimePeriod(timePeriod: TimePeriod): SubmittedPeriod

    /**
     * Unsubmit a [TimePeriod] so it can be edited
     * See [submitTimePeriod]
     */
    fun unsubmitTimePeriod(timePeriod: TimePeriod)

    /**
     * For the employee making the request, get the [SubmittedPeriod]
     */
    fun getSubmittedTimePeriod(timePeriod: TimePeriod): SubmittedPeriod

    /**
     * Get all the time entries for the selected [Employee] and [TimePeriod]
     */
    fun getTimeEntriesForTimePeriod(employee: Employee, timePeriod: TimePeriod): Set<TimeEntry>

    /**
     * Returns whether, for the [Employee] and [Date], it is a submitted [TimePeriod]
     */
    fun isInASubmittedPeriod(employee: Employee, date: Date): Boolean

    /**
     * Delete a time entry, like it says.  See [createTimeEntry] and [changeEntry]
     */
    fun deleteTimeEntry(timeEntry: TimeEntry): Boolean

    /**
     * Given the id of a [TimeEntry], return the whole thing
     */
    fun findTimeEntryById(id: TimeEntryId): TimeEntry

    /**
     * For the approver, mark a given employee's time entries as approved for a
     * given [TimePeriod], chosen by the starting date of that time period
     */
    fun approveTimesheet(employee: Employee, startDate: Date): ApprovalResultStatus

    /**
     * Returns whether, for an employee / startdate combination, we are looking
     * at an approved time period.  See [unapproveTimesheet]
     */
    fun isApproved(employee: Employee, startDate: Date) : ApprovalStatus

    /**
     * Unapprove a time period.  See [isApproved]
     */
    fun unapproveTimesheet(employee: Employee, startDate: Date): ApprovalResultStatus

    /**
     * Given a name, return the whole project.  See [findProjectById]
     */
    fun findProjectByName(name: ProjectName): Project

    /**
     * Deletes a project.  See [createProject]
     */
    fun deleteProject(project: Project): DeleteProjectResult

    /**
     * Just deletes the employee, doesn't check any
     * business rules.  If you want that, see [coverosR3z.timerecording.utility.DeleteEmployeeUtility.deleteEmployee]
     * See [createEmployee]
     */
    fun deleteEmployee(employee: Employee): Boolean

    /**
     * Returns whether this project is used in any time entries,
     * handy for whether we will allow a user to delete a project.
     */
    fun isProjectUsedForTimeEntry(project: Project): Boolean

    /**
     * Calculates time entered for the current week, assuming the
     * week is between Sunday and Saturday, for a particular employee
     * @param todayDate today's date
     */
    fun getTimeForWeek(employee: Employee, todayDate: Date): Time
}