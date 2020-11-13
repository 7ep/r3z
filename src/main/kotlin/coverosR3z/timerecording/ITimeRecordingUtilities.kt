package coverosR3z.timerecording

import coverosR3z.authentication.CurrentUser
import coverosR3z.domainobjects.*

interface ITimeRecordingUtilities {
    fun changeUser(cu : CurrentUser) : ITimeRecordingUtilities

    fun recordTime(entry: TimeEntryPreDatabase): RecordTimeResult

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
    fun getEntriesForEmployeeOnDate(employee: Employee, date: Date): List<TimeEntry>
    fun getAllEntriesForEmployee(employee: Employee): List<TimeEntry>
    fun listAllProjects(): List<Project>
    fun findProjectById(id: Int): Project
    fun findEmployeeById(id: Int): Employee
    fun listAllEmployees(): List<Employee>
}
