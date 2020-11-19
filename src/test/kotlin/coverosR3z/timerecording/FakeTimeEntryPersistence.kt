package coverosR3z.timerecording

import coverosR3z.DEFAULT_PROJECT
import coverosR3z.DEFAULT_EMPLOYEE
import coverosR3z.domainobjects.*

/**
 * Used as a mock object for testing
 */
class FakeTimeEntryPersistence(
        var minutesRecorded : Int = 0,
        var persistNewTimeEntryBehavior : () -> Unit = {},
        var persistNewProjectBehavior : () -> Project = { DEFAULT_PROJECT },
        var getProjectByNameBehavior : () -> Project = { NO_PROJECT },
        var getProjectByIdBehavior : (id : ProjectId) -> Project = { NO_PROJECT },
        var getEmployeeByIdBehavior : (id : EmployeeId) -> Employee = { NO_EMPLOYEE },
) : ITimeEntryPersistence {


    override fun persistNewTimeEntry(entry: TimeEntryPreDatabase) {
        persistNewTimeEntryBehavior()
    }

    override fun persistNewProject(projectName: ProjectName): Project {
        return persistNewProjectBehavior()
    }

    override fun persistNewEmployee(employeename: EmployeeName): Employee {
        return DEFAULT_EMPLOYEE
    }


    override fun queryMinutesRecorded(employee: Employee, date: Date): Int {
        return minutesRecorded
    }

    override fun readTimeEntries(employee: Employee): List<TimeEntry> {
        return listOf()
    }

    override fun readTimeEntriesOnDate(employee: Employee, date: Date): List<TimeEntry> {
        return listOf()
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

}