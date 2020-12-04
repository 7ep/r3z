package coverosR3z.timerecording

import coverosR3z.domainobjects.*
import coverosR3z.logging.logDebug
import coverosR3z.persistence.PureMemoryDatabase

class TimeEntryPersistence(val pmd : PureMemoryDatabase) : ITimeEntryPersistence {

    override fun persistNewTimeEntry(entry: TimeEntryPreDatabase) {
        logDebug("persisting a new timeEntry")
        isEntryValid(entry)
        pmd.addTimeEntry(entry)
    }

    /**
     * This will throw an exception if the project or employee in
     * this timeentry don't exist in the list of projects / employees
     */
    private fun isEntryValid(entry: TimeEntryPreDatabase) {
        check(pmd.getProjectById(entry.project.id) != NO_PROJECT) {"a time entry with no project is invalid"}
        check(pmd.getEmployeeById(entry.employee.id) != NO_EMPLOYEE) {"a time entry with no employee is invalid"}
    }

    override fun persistNewProject(projectName: ProjectName): Project {
        logDebug("Recording a new project, ${projectName.value}, to the database")
        return pmd.addNewProject(projectName)
    }

    override fun persistNewEmployee(employeename: EmployeeName): Employee {
        logDebug("Recording a new employee, \"${employeename.value}\", to the database")
        return pmd.addNewEmployee(employeename)
    }

    override fun queryMinutesRecorded(employee: Employee, date: Date): Time {
        return pmd.getMinutesRecordedOnDate(employee, date)
    }

    override fun readTimeEntries(employee: Employee): Set<TimeEntry> {
        return pmd.getAllTimeEntriesForEmployee(employee)
    }

    override fun readTimeEntriesOnDate(employee: Employee, date: Date): Set<TimeEntry> {
        return pmd.getAllTimeEntriesForEmployeeOnDate(employee, date)
    }

    override fun getProjectByName(name: ProjectName): Project {
        return pmd.getProjectByName(name)
    }

    override fun getProjectById(id: ProjectId): Project {
        return pmd.getProjectById(id)
    }

    override fun getAllProjects(): List<Project> {
        return pmd.getAllProjects()
    }

    override fun getAllEmployees(): List<Employee> {
        return pmd.getAllEmployees()
    }

    override fun getEmployeeById(id: EmployeeId): Employee {
        return pmd.getEmployeeById(id)
    }

}