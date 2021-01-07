package coverosR3z.timerecording

import coverosR3z.DEFAULT_EMPLOYEE
import coverosR3z.DEFAULT_PROJECT
import coverosR3z.DEFAULT_TIME_ENTRY
import coverosR3z.domainobjects.*
import coverosR3z.misc.types.Date
import coverosR3z.timerecording.persistence.ITimeEntryPersistence

/**
 * Used as a mock object for testing
 */
class FakeTimeEntryPersistence(
        var minutesRecorded : Time = Time(0),
        var persistNewTimeEntryBehavior : () -> TimeEntry = { DEFAULT_TIME_ENTRY },
        var persistNewProjectBehavior : () -> Project = { DEFAULT_PROJECT },
        var getProjectByNameBehavior : () -> Project = { NO_PROJECT },
        var getProjectByIdBehavior : (id : ProjectId) -> Project = { NO_PROJECT },
        var getEmployeeByIdBehavior : (id : EmployeeId) -> Employee = { NO_EMPLOYEE },
        var overwriteTimeEntryBehavior : () -> Unit = {},
) : ITimeEntryPersistence {


    override fun persistNewTimeEntry(entry: TimeEntryPreDatabase) : TimeEntry {
        return persistNewTimeEntryBehavior()
    }

    override fun persistNewProject(projectName: ProjectName): Project {
        return persistNewProjectBehavior()
    }

    override fun persistNewEmployee(employeename: EmployeeName): Employee {
        return DEFAULT_EMPLOYEE
    }


    override fun queryMinutesRecorded(employee: Employee, date: Date): Time {
        return minutesRecorded
    }

    override fun readTimeEntries(employee: Employee): Set<TimeEntry> {
        return setOf()
    }

    override fun readTimeEntriesOnDate(employee: Employee, date: Date): Set<TimeEntry> {
        return setOf()
    }

    override fun getProjectByName(name: ProjectName): Project {
        return getProjectByNameBehavior()
    }

    override fun getProjectById(id: ProjectId): Project {
        return getProjectByIdBehavior(id)
    }

    override fun getAllProjects(): List<Project> {
        return listOf()
    }

    override fun getAllEmployees(): List<Employee> {
        return listOf()
    }

    override fun getEmployeeById(id: EmployeeId): Employee {
        return getEmployeeByIdBehavior(id)
    }

    override fun overwriteTimeEntry(empId: EmployeeId, id: Int, newEntry: TimeEntry) {
        overwriteTimeEntryBehavior()
    }

}