package coverosR3z.timerecording

import coverosR3z.domainobjects.*
import coverosR3z.logging.logInfo
import coverosR3z.persistence.ProjectIntegrityViolationException
import coverosR3z.persistence.PureMemoryDatabase
import coverosR3z.persistence.EmployeeIntegrityViolationException

class TimeEntryPersistence(val pmd : PureMemoryDatabase) : ITimeEntryPersistence {

    override fun persistNewTimeEntry(entry: TimeEntryPreDatabase) {
        logInfo("persisting a new timeEntry, $entry")
        isEntryValid(entry)
        pmd.addTimeEntry(entry)
    }

    /**
     * This will throw an exception if the project or employee in
     * this timeentry don't exist in the list of projects / employees
     */
    private fun isEntryValid(entry: TimeEntryPreDatabase) {
        pmd.getProjectById(entry.project.id) ?: throw ProjectIntegrityViolationException()
        pmd.getEmployeeById(entry.employee.id) ?: throw EmployeeIntegrityViolationException()
    }

    override fun persistNewProject(projectName: ProjectName): Project {
        logInfo("Recording a new project, ${projectName.value}, to the database")
        val newId = pmd.addNewProject(projectName)
        assert(newId > 0) {"A valid project will receive a positive id"}
        return Project(newId, projectName.value)
    }

    override fun persistNewEmployee(employeename: EmployeeName): Employee {
        logInfo("Recording a new employee, ${employeename.value}, to the database")

        val newId = pmd.addNewEmployee(employeename)

        assert(newId > 0) {"A valid employee will receive a positive id"}
        return Employee(newId, employeename.value)
    }

    override fun queryMinutesRecorded(employee: Employee, date: Date): Int {
        val minutes = pmd.getMinutesRecordedOnDate(employee, date)
        return minutes
    }

    override fun readTimeEntries(employee: Employee): List<TimeEntry> {
        return pmd.getAllTimeEntriesForEmployee(employee)
    }

    override fun readTimeEntriesOnDate(employee: Employee, date: Date): List<TimeEntry> {
        return pmd.getAllTimeEntriesForEmployeeOnDate(employee, date)
    }

}