package coverosR3z.timerecording

import coverosR3z.domainobjects.*

interface ITimeEntryPersistence {
    fun persistNewTimeEntry(entry: TimeEntryPreDatabase)
    fun persistNewProject(projectName: ProjectName) : Project
    fun persistNewEmployee(employeename: EmployeeName): Employee

    /**
     * Provided a employee and date, give the number of minutes they worked on that date
     */
    fun queryMinutesRecorded(employee: Employee, date: Date): Time

    /**
     * Get all the time entries for a particular employee
     */
    fun readTimeEntries(employee: Employee): Map<Date, Set<TimeEntry>>
    fun readTimeEntriesOnDate(employee: Employee, date: Date): Set<TimeEntry>
    fun getProjectByName(name : ProjectName) : Project
    fun getProjectById(id: ProjectId): Project
    fun getAllProjects(): List<Project>
    fun getAllEmployees(): List<Employee>
    fun getEmployeeById(id: EmployeeId): Employee
}