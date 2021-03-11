package coverosR3z.timerecording

import coverosR3z.misc.DEFAULT_EMPLOYEE
import coverosR3z.misc.DEFAULT_PROJECT
import coverosR3z.authentication.types.CurrentUser
import coverosR3z.misc.DEFAULT_SUBMITTED_PERIOD
import coverosR3z.misc.types.Date
import coverosR3z.timerecording.types.*
import coverosR3z.timerecording.utility.ITimeRecordingUtilities

/**
 * Used as a mock object for testing
 */
class FakeTimeRecordingUtilities(
    var createTimeEntryBehavior : () -> RecordTimeResult = { RecordTimeResult() },
    var changeEntryBehavior : () -> RecordTimeResult = { RecordTimeResult() },
    var createProjectBehavior : () -> Project = { DEFAULT_PROJECT },
    var createEmployeeBehavior : () -> Employee = { DEFAULT_EMPLOYEE },
    var getEntriesForEmployeeOnDateBehavior : () -> Set<TimeEntry> = { emptySet() },
    var getAllEntriesForEmployeeBehavior : () -> Set<TimeEntry> = { emptySet() },
    var getTimeEntriesForTimePeriodBehavior : () -> Set<TimeEntry> = { emptySet() },
    var changeUserBehavior : () -> ITimeRecordingUtilities = { FakeTimeRecordingUtilities() },
    var listAllProjectsBehavior : () -> List<Project> = { emptyList() },
    var findProjectByIdBehavior : () -> Project = { NO_PROJECT },
    var listAllEmployeesBehavior : () -> List<Employee> = { emptyList() },
    var findEmployeeByIdBehavior : () -> Employee = { NO_EMPLOYEE },
    var submitTimePeriodBehavior : () -> SubmittedPeriod = { DEFAULT_SUBMITTED_PERIOD },
    var unsubmitTimePeriodBehavior : () -> Unit = {},
    var getSubmittedTimePeriodBehavior : () -> SubmittedPeriod = { DEFAULT_SUBMITTED_PERIOD },
    var isInASubmittedPeriodBehavior : () -> Boolean = { false },
    ) : ITimeRecordingUtilities {

    override fun changeUser(cu: CurrentUser): ITimeRecordingUtilities {
        return changeUserBehavior()
    }

    override fun createTimeEntry(entry: TimeEntryPreDatabase): RecordTimeResult {
        return createTimeEntryBehavior()
    }

    override fun changeEntry(entry: TimeEntry): RecordTimeResult{
        return changeEntryBehavior()
    }

    override fun createProject(projectName: ProjectName): Project {
        return createProjectBehavior()
    }

    override fun createEmployee(employeename: EmployeeName): Employee {
        return createEmployeeBehavior()
    }

    override fun getEntriesForEmployeeOnDate(employee: Employee, date: Date): Set<TimeEntry> {
        return getEntriesForEmployeeOnDateBehavior()
    }

    override fun getAllEntriesForEmployee(employee: Employee): Set<TimeEntry> {
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
    override fun submitTimePeriod(timePeriod: TimePeriod): SubmittedPeriod{
        return submitTimePeriodBehavior()
    }

    override fun unsubmitTimePeriod(timePeriod: TimePeriod) {
        unsubmitTimePeriodBehavior()
    }

    override fun getSubmittedTimePeriod(timePeriod: TimePeriod): SubmittedPeriod {
        return getSubmittedTimePeriodBehavior()
    }

    override fun getTimeEntriesForTimePeriod(employee: Employee, timePeriod: TimePeriod): Set<TimeEntry> {
        return getTimeEntriesForTimePeriodBehavior()
    }

    override fun isInASubmittedPeriod(employee: Employee, date: Date): Boolean {
        return isInASubmittedPeriodBehavior()
    }
}