package coverosR3z.timerecording.utility

import coverosR3z.authentication.types.CurrentUser
import coverosR3z.misc.types.Date
import coverosR3z.timerecording.types.*

interface ITimeRecordingUtilities {
    fun changeUser(cu : CurrentUser) : ITimeRecordingUtilities

    fun recordTime(entry: TimeEntryPreDatabase): RecordTimeResult

    /**
     * This assumes you are changing your own time entries,
     * which implies you have an employee id
     */
    fun changeEntry(newEntry: TimeEntry): RecordTimeResult

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
    fun getEntriesForEmployeeOnDate(employeeId: EmployeeId, date: Date): Set<TimeEntry>
    fun getAllEntriesForEmployee(employeeId: EmployeeId): Set<TimeEntry>
    fun listAllProjects(): List<Project>
    fun findProjectById(id: ProjectId): Project
    fun findEmployeeById(id: EmployeeId): Employee
    fun listAllEmployees(): List<Employee>
}
