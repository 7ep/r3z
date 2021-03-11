package coverosR3z.timerecording.utility

import coverosR3z.authentication.types.CurrentUser
import coverosR3z.misc.types.Date
import coverosR3z.timerecording.types.*

interface ITimeRecordingUtilities {
    /**
     * This allows us to create a new [TimeRecordingUtilities]
     * that is identical to the old except with a new [CurrentUser]
     * Only allowed by the System user
     */
    fun changeUser(cu : CurrentUser) : ITimeRecordingUtilities

    fun createTimeEntry(entry: TimeEntryPreDatabase): RecordTimeResult

    /**
     * This assumes you are changing your own time entries,
     * which implies you have an employee id
     */
    fun changeEntry(entry: TimeEntry): RecordTimeResult

    /**
     * Business code for creating a new project in the
     * system (persists it to the database)
     */
    fun createProject(projectName: ProjectName) : Project

    /**
     * Business code for creating a new employee in the
     * system (persists it to the database)
     */
    fun createEmployee(employeename: EmployeeName) : Employee
    fun getEntriesForEmployeeOnDate(employee: Employee, date: Date): Set<TimeEntry>
    fun getAllEntriesForEmployee(employee: Employee): Set<TimeEntry>
    fun listAllProjects(): List<Project>
    fun findProjectById(id: ProjectId): Project
    fun findEmployeeById(id: EmployeeId): Employee
    fun listAllEmployees(): List<Employee>
    fun submitTimePeriod(timePeriod: TimePeriod): SubmittedPeriod
    fun unsubmitTimePeriod(timePeriod: TimePeriod)
    fun getSubmittedTimePeriod(timePeriod: TimePeriod): SubmittedPeriod
    fun getTimeEntriesForTimePeriod(employee: Employee, timePeriod: TimePeriod): Set<TimeEntry>
    fun isInASubmittedPeriod(employee: Employee, date: Date): Boolean
}