package coverosR3z.timerecording

import coverosR3z.authentication.types.CurrentUser
import coverosR3z.misc.DEFAULT_EMPLOYEE
import coverosR3z.misc.DEFAULT_PROJECT
import coverosR3z.misc.DEFAULT_SUBMITTED_PERIOD
import coverosR3z.misc.DEFAULT_TIME_ENTRY
import coverosR3z.misc.types.Date
import coverosR3z.timerecording.persistence.ITimeEntryPersistence
import coverosR3z.timerecording.types.*

/**
 * Used as a mock object for testing
 */
class FakeTimeEntryPersistence(
    var minutesRecorded : Time = Time(0),
    var persistNewTimeEntryBehavior : () -> TimeEntry = { DEFAULT_TIME_ENTRY },
    var persistNewProjectBehavior : () -> Project = { DEFAULT_PROJECT },
    var persistNewEmployeeBehavior : () -> Employee = { DEFAULT_EMPLOYEE },
    var getProjectByNameBehavior : () -> Project = { NO_PROJECT },
    var getProjectByIdBehavior : (id : ProjectId) -> Project = { NO_PROJECT },
    var getEmployeeByIdBehavior : (id : EmployeeId) -> Employee = { NO_EMPLOYEE },
    var overwriteTimeEntryBehavior : () -> TimeEntry = { DEFAULT_TIME_ENTRY },
    var setCurrentUserBehavior : () -> ITimeEntryPersistence = { FakeTimeEntryPersistence() },
    var setLockedEmployeeDateBehavior : () -> Boolean = { false },
    var persistNewSubmittedTimePeriodBehavior : () -> SubmittedPeriod = { DEFAULT_SUBMITTED_PERIOD },
    var unsubmitTimePeriodBehavior : () -> Unit = {},
    var getSubmittedTimePeriodBehavior : () -> SubmittedPeriod = { DEFAULT_SUBMITTED_PERIOD },
    var getTimeEntriesForTimePeriodBehavior : () -> Set<TimeEntry> = { emptySet() },
) : ITimeEntryPersistence {

    override fun setCurrentUser(cu: CurrentUser): ITimeEntryPersistence {
        return setCurrentUserBehavior()
    }

    override fun persistNewTimeEntry(entry: TimeEntryPreDatabase) : TimeEntry {
        return persistNewTimeEntryBehavior()
    }

    override fun persistNewProject(projectName: ProjectName): Project {
        return persistNewProjectBehavior()
    }

    override fun persistNewEmployee(employeename: EmployeeName): Employee {
        return persistNewEmployeeBehavior()
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

    override fun overwriteTimeEntry(newEntry: TimeEntry): TimeEntry {
        return overwriteTimeEntryBehavior()
    }

    override fun isInASubmittedPeriod(employee: Employee, date: Date): Boolean {
        return setLockedEmployeeDateBehavior()
    }

    override fun persistNewSubmittedTimePeriod(employee: Employee, timePeriod: TimePeriod) : SubmittedPeriod{
        return persistNewSubmittedTimePeriodBehavior()
    }

    override fun getSubmittedTimePeriod(employee: Employee, timePeriod: TimePeriod): SubmittedPeriod {
        return getSubmittedTimePeriodBehavior()
    }

    override fun getTimeEntriesForTimePeriod(employee: Employee, timePeriod: TimePeriod): Set<TimeEntry> {
        return getTimeEntriesForTimePeriodBehavior()
    }

    override fun unsubmitTimePeriod(stp: SubmittedPeriod) {
        unsubmitTimePeriodBehavior()
    }

}