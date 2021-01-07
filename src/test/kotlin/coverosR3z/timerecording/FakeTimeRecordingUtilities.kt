package coverosR3z.timerecording

import coverosR3z.DEFAULT_EMPLOYEE
import coverosR3z.DEFAULT_PROJECT
import coverosR3z.authentication.types.CurrentUser
import coverosR3z.misc.types.Date
import coverosR3z.timerecording.types.*
import coverosR3z.timerecording.utility.ITimeRecordingUtilities

/**
 * Used as a mock object for testing
 */
class FakeTimeRecordingUtilities(
    var recordTimeBehavior : () -> RecordTimeResult = { RecordTimeResult() },
    var createProjectBehavior : () -> Project = { DEFAULT_PROJECT },
    var createEmployeeBehavior : () -> Employee = { DEFAULT_EMPLOYEE },
    var getEntriesForEmployeeOnDateBehavior : () -> Set<TimeEntry> = { emptySet() },
    var getAllEntriesForEmployeeBehavior : () -> Set<TimeEntry> = {emptySet() },
    var changeUserBehavior : () -> ITimeRecordingUtilities = { FakeTimeRecordingUtilities() },
    var listAllProjectsBehavior : () -> List<Project> = {emptyList()},
    var findProjectByIdBehavior : () -> Project = { NO_PROJECT },
    var listAllEmployeesBehavior : () -> List<Employee> = {emptyList()},
    var findEmployeeByIdBehavior : () -> Employee = { NO_EMPLOYEE },
    var changeEntryBehavior : () -> Unit = {}
        ) : ITimeRecordingUtilities {

    override fun changeUser(cu: CurrentUser): ITimeRecordingUtilities {
        return changeUserBehavior()
    }

    override fun recordTime(entry: TimeEntryPreDatabase): RecordTimeResult {
        return recordTimeBehavior()
    }

    override fun createProject(projectName: ProjectName): Project {
        return createProjectBehavior()
    }

    override fun createEmployee(employeename: EmployeeName): Employee {
        return createEmployeeBehavior()
    }

    override fun getEntriesForEmployeeOnDate(employeeId: EmployeeId, date: Date): Set<TimeEntry> {
        return getEntriesForEmployeeOnDateBehavior()
    }

    override fun getAllEntriesForEmployee(employeeId: EmployeeId): Set<TimeEntry> {
        return getAllEntriesForEmployeeBehavior()
    }

    override fun listAllProjects(): List<Project> {
        return listAllProjectsBehavior()
    }

    override fun findProjectById(id: ProjectId): Project {
        return findProjectByIdBehavior()
    }

    override fun findEmployeeById(id: EmployeeId): Employee {
        return findEmployeeByIdBehavior()
    }

    override fun listAllEmployees(): List<Employee> {
        return listAllEmployeesBehavior()
    }

    override fun changeEntry(id: Int, newEntry: TimeEntry) {
        changeEntryBehavior()
    }


}