package coverosR3z.timerecording

import coverosR3z.DEFAULT_PROJECT
import coverosR3z.DEFAULT_EMPLOYEE
import coverosR3z.domainobjects.*


class FakeTimeEntryPersistence(
        var minutesRecorded : Int = 0,
        var persistNewTimeEntryBehavior : () -> Unit = {},
        var persistNewProjectBehavior : () -> Project = { DEFAULT_PROJECT }) : ITimeEntryPersistence {


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

}