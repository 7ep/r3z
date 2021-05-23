package coverosR3z.timerecording.persistence

import coverosR3z.authentication.types.CurrentUser
import coverosR3z.system.misc.types.Date
import coverosR3z.timerecording.types.*

interface ITimeEntryPersistence {
    fun persistNewTimeEntry(entry: TimeEntryPreDatabase) : TimeEntry
    fun persistNewProject(projectName: ProjectName) : Project
    fun persistNewEmployee(employeename: EmployeeName): Employee

    fun overwriteTimeEntry(newEntry: TimeEntry) : TimeEntry


    /**
     * Provided a employee and date, give the number of minutes they worked on that date
     */
    fun queryMinutesRecorded(employee: Employee, date: Date): Time

    /**
     * Get all the time entries for a particular employee
     */
    fun readTimeEntries(employee: Employee): Set<TimeEntry>
    fun readTimeEntriesOnDate(employee: Employee, date: Date): Set<TimeEntry>
    fun getProjectByName(name : ProjectName) : Project
    fun getProjectById(id: ProjectId): Project
    fun getAllProjects(): List<Project>
    fun getAllEmployees(): List<Employee>
    fun getEmployeeById(id: EmployeeId): Employee
    fun isInASubmittedPeriod(employee: Employee, date: Date): Boolean
    fun persistNewSubmittedTimePeriod(employee: Employee, timePeriod: TimePeriod) : SubmittedPeriod
    fun getSubmittedTimePeriod(employee: Employee, timePeriod: TimePeriod) : SubmittedPeriod
    fun getTimeEntriesForTimePeriod(employee: Employee, timePeriod: TimePeriod): Set<TimeEntry>
    fun unsubmitTimePeriod(stp: SubmittedPeriod)
    fun deleteTimeEntry(timeEntry: TimeEntry): Boolean
    fun findTimeEntryById(id: TimeEntryId): TimeEntry
    fun approveTimesheet(stp: SubmittedPeriod): Boolean
    fun unapproveTimesheet(stp: SubmittedPeriod): Boolean
    fun getEmployeeByName(employeeName: EmployeeName): Employee
    fun isProjectUsedForTimeEntry(project: Project): Boolean
    fun deleteProject(project: Project): Boolean
}