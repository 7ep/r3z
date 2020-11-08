package coverosR3z.timerecording

import coverosR3z.DEFAULT_EMPLOYEE
import coverosR3z.DEFAULT_PROJECT
import coverosR3z.domainobjects.*

/**
 * Used as a mock object for testing
 */
class FakeTimeRecordingUtilities(
        var recordTimeBehavior : () -> RecordTimeResult = {RecordTimeResult()},
        var createProjectBehavior : () -> Project = { DEFAULT_PROJECT },
        var createEmployeeBehavior : () -> Employee = { DEFAULT_EMPLOYEE },
        var getEntriesForEmployeeOnDateBehavior : () -> List<TimeEntry> = { emptyList() },
        var getAllEntriesForEmployeeBehavior : () -> List<TimeEntry> = {emptyList() },

        ) : ITimeRecordingUtilities {

    override fun recordTime(entry: TimeEntryPreDatabase): RecordTimeResult {
        return recordTimeBehavior()
    }

    override fun createProject(projectName: ProjectName): Project {
        return createProjectBehavior()
    }

    override fun createEmployee(employeename: EmployeeName): Employee {
        return createEmployeeBehavior()
    }

    override fun getEntriesForEmployeeOnDate(employee: Employee, date: Date): List<TimeEntry> {
        return getEntriesForEmployeeOnDateBehavior()
    }

    override fun getAllEntriesForEmployee(employee: Employee): List<TimeEntry> {
        return getAllEntriesForEmployeeBehavior()
    }

}