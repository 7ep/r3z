package coverosR3z.timerecording.persistence

import coverosR3z.authentication.types.CurrentUser
import coverosR3z.authentication.types.SYSTEM_USER
import coverosR3z.logging.logDebug
import coverosR3z.logging.logTrace
import coverosR3z.misc.types.Date
import coverosR3z.persistence.utility.PureMemoryDatabase
import coverosR3z.timerecording.types.*

class TimeEntryPersistence(
    private val pmd : PureMemoryDatabase,
    private val cu : CurrentUser = CurrentUser(SYSTEM_USER)) : ITimeEntryPersistence {

    override fun setCurrentUser(cu: CurrentUser): ITimeEntryPersistence {
        return TimeEntryPersistence(pmd, cu)
    }

    override fun persistNewTimeEntry(entry: TimeEntryPreDatabase) : TimeEntry {
        isEntryValid(entry)
        return pmd.TimeEntryDataAccess().actOn { entries ->

            // add the new data
            val newIndex = entries.nextIndex.getAndIncrement()

            logTrace(cu) {"new time-entry index is $newIndex" }
            val newTimeEntry = TimeEntry(
                TimeEntryId(newIndex),
                entry.employee,
                entry.project,
                entry.time,
                entry.date,
                entry.details
            )
            entries.add(newTimeEntry)
            logDebug(cu) {"recorded a new timeEntry: $newTimeEntry"}
            newTimeEntry
        }
    }

    override fun overwriteTimeEntry(newEntry: TimeEntry) : TimeEntry {
        isEntryValid(newEntry.toTimeEntryPreDatabase())
        val oldEntry = pmd.TimeEntryDataAccess().actOn {entries ->
            entries.single{it.id == newEntry.id}
        }

        check (oldEntry.employee == newEntry.employee) {"Employee field of a time entry may not be changed"}

        pmd.TimeEntryDataAccess().actOn { entries ->
            entries.remove(oldEntry)
            entries.add(newEntry)
        }

        logDebug(cu) {"modified an existing timeEntry: $newEntry"}
        logTrace(cu) { "old time-entry is $oldEntry and new time-entry is $newEntry" }
        return newEntry
    }

    /**
     * This will throw an exception if the project or employee in
     * this time entry don't exist in the list of projects / employees
     * or is missing in the time entry
     */
    private fun isEntryValid(entry: TimeEntryPreDatabase) {
        check(getProjectById(entry.project.id) != NO_PROJECT) {timeEntryInvalidNoProject}
        check(getEmployeeById(entry.employee.id) != NO_EMPLOYEE) {timeEntryInvalidNoEmployee}
        check(pmd.EmployeeDataAccess().actOn { it.any{employee -> employee == entry.employee} }) {timeEntryInvalidBadEmployee}
        check(pmd.ProjectDataAccess().read { it.any{project -> project == entry.project} }) {timeEntryInvalidBadProject}
    }

    override fun persistNewProject(projectName: ProjectName): Project {
        return pmd.ProjectDataAccess().actOn { projects ->
            val newProject = Project(ProjectId(projects.nextIndex.getAndIncrement()), ProjectName(projectName.value))
            projects.add(newProject)
            logDebug(cu) { "Recorded a new project, \"${projectName.value}\", id: ${newProject.id.value}, to the database" }
            newProject
        }
    }

    override fun persistNewEmployee(employeename: EmployeeName): Employee {
        return pmd.EmployeeDataAccess().actOn { employees ->
            val newEmployee = Employee(EmployeeId(employees.nextIndex.getAndIncrement()), EmployeeName(employeename.value))
            employees.add(newEmployee)
            logDebug(cu) { "Recorded a new employee, \"${employeename.value}\", id: ${newEmployee.id.value}, to the database" }
            newEmployee
        }
    }

    override fun queryMinutesRecorded(employee: Employee, date: Date): Time {
        return pmd.TimeEntryDataAccess().read (
            fun(timeEntries): Time {

                // if the employee hasn't entered any time on this date, return 0 minutes
                val totalMinutes = timeEntries.filter { it.date == date && it.employee == employee }.sumBy { te -> te.time.numberOfMinutes }
                return Time(totalMinutes)
            })
    }

    override fun readTimeEntries(employee: Employee): Set<TimeEntry> {
        return pmd.TimeEntryDataAccess().read { timeEntries -> timeEntries.filter { it.employee == employee } }.toSet()
    }

    override fun readTimeEntriesOnDate(employee: Employee, date: Date): Set<TimeEntry> {
        return pmd.TimeEntryDataAccess().read { timeEntries -> timeEntries.filter { it.employee == employee && it.date == date } }.toSet()
    }

    override fun getProjectByName(name: ProjectName): Project {
        return pmd.ProjectDataAccess().read { it.singleOrNull { p -> p.name == name } ?: NO_PROJECT }
    }

    override fun getProjectById(id: ProjectId): Project {
        return pmd.ProjectDataAccess().read { it.singleOrNull { p -> p.id == id } ?: NO_PROJECT }
    }

    override fun getAllProjects(): List<Project> {
        return pmd.ProjectDataAccess().read { it.toList() }
    }

    override fun getAllEmployees(): List<Employee> {
        return pmd.EmployeeDataAccess().read { it.toList() }
    }

    override fun getEmployeeById(id: EmployeeId): Employee {
        return pmd.EmployeeDataAccess().read { employees -> employees.singleOrNull {it.id == id} ?: NO_EMPLOYEE }
    }

    override fun isInASubmittedPeriod(employeeId: EmployeeId, date: Date): Boolean {
        // The following closure returns a boolean depending on whether the provided date falls within
        // any of the submission date ranges for the provided employee
        return pmd.SubmittedPeriodsAccess().read {
                submissions -> submissions.filter{it.employeeId == employeeId}.any{it.bounds.contains(date)}
        }
    }

    companion object {
        const val timeEntryInvalidNoProject = "a time entry with no project is invalid"
        const val timeEntryInvalidNoEmployee = "a time entry with no employee is invalid"
        const val timeEntryInvalidBadEmployee = "a time entry with a non-registered employee is invalid"
        const val timeEntryInvalidBadProject = "a time entry with a non-registered project is invalid"
    }
}