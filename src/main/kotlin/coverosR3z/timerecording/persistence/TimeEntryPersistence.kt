package coverosR3z.timerecording.persistence

import coverosR3z.logging.logDebug
import coverosR3z.logging.logTrace
import coverosR3z.misc.types.Date
import coverosR3z.persistence.utility.PureMemoryDatabase
import coverosR3z.persistence.types.ConcurrentSet
import coverosR3z.timerecording.types.*

class TimeEntryPersistence(private val pmd : PureMemoryDatabase) : ITimeEntryPersistence {

    override fun persistNewTimeEntry(entry: TimeEntryPreDatabase) : TimeEntry {
        isEntryValid(entry)
        return pmd.actOnTimeEntries { entries ->

            // add the new data
            val newIndex = entries.nextIndex.getAndIncrement()

            logTrace { "new time-entry index is $newIndex" }
            val newTimeEntry = TimeEntry(
                TimeEntryId(newIndex),
                entry.employee,
                entry.project,
                entry.time,
                entry.date,
                entry.details
            )
            entries.add(newTimeEntry)
            logDebug{"recorded a new timeEntry: $newTimeEntry"}
            newTimeEntry
        }
    }

    override fun overwriteTimeEntry(newEntry: TimeEntry) : TimeEntry {
        isEntryValid(newEntry.toTimeEntryPreDatabase())
        val oldEntry = pmd.readTimeEntries{entries ->
            entries.single{it.id == newEntry.id}
        }

        check (oldEntry.employee == newEntry.employee) {"Employee field of a time entry may not be changed"}

        pmd.actOnTimeEntries{ entries ->
            entries.remove(oldEntry)
            entries.add(newEntry)
        }

        logDebug{"modified an existing timeEntry: $newEntry"}
        logTrace { "old time-entry index is $oldEntry and new time-entry is $newEntry" }
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
        check(pmd.actOnEmployees { it.any{employee -> employee == entry.employee} }) {timeEntryInvalidBadEmployee}
        check(pmd.readProjects { it.any{project -> project == entry.project} }) {timeEntryInvalidBadProject}
    }

    override fun persistNewProject(projectName: ProjectName): Project {
        return pmd.actOnProjects { projects ->
            val newProject = Project(ProjectId(projects.nextIndex.getAndIncrement()), ProjectName(projectName.value))
            projects.add(newProject)
            logDebug { "Recorded a new project, \"${projectName.value}\", id: ${newProject.id.value}, to the database" }
            newProject
        }
    }

    override fun persistNewEmployee(employeename: EmployeeName): Employee {
        return pmd.actOnEmployees { employees ->
            val newEmployee = Employee(EmployeeId(employees.nextIndex.getAndIncrement()), EmployeeName(employeename.value))
            employees.add(newEmployee)
            logDebug { "Recorded a new employee, \"${employeename.value}\", id: ${newEmployee.id.value}, to the database" }
            newEmployee
        }
    }

    override fun queryMinutesRecorded(employee: Employee, date: Date): Time {
        return pmd.readTimeEntries (
            fun(timeEntries): Time {

                // if the employee hasn't entered any time on this date, return 0 minutes
                val totalMinutes = timeEntries.filter { it.date == date && it.employee == employee }.sumBy { te -> te.time.numberOfMinutes }
                return Time(totalMinutes)
            })
    }

    override fun readTimeEntries(employee: Employee): Set<TimeEntry> {
        return pmd.readTimeEntries{ timeEntries -> timeEntries.filter { it.employee == employee } }.toSet()
    }

    override fun readTimeEntriesOnDate(employee: Employee, date: Date): Set<TimeEntry> {
        return pmd.readTimeEntries { timeEntries -> timeEntries.filter { it.employee == employee && it.date == date } }.toSet()
    }

    override fun getProjectByName(name: ProjectName): Project {
        return pmd.readProjects { it.singleOrNull { p -> p.name == name } ?: NO_PROJECT }
    }

    override fun getProjectById(id: ProjectId): Project {
        return pmd.readProjects { it.singleOrNull { p -> p.id == id } ?: NO_PROJECT }
    }

    override fun getAllProjects(): List<Project> {
        return pmd.readProjects { it.toList() }
    }

    override fun getAllEmployees(): List<Employee> {
        return pmd.actOnEmployees { it.toList() }
    }

    override fun getEmployeeById(id: EmployeeId): Employee {
        return pmd.actOnEmployees { employees -> employees.singleOrNull {it.id == id} ?: NO_EMPLOYEE }
    }

    companion object {
        const val timeEntryInvalidNoProject = "a time entry with no project is invalid"
        const val timeEntryInvalidNoEmployee = "a time entry with no employee is invalid"
        const val timeEntryInvalidBadEmployee = "a time entry with a non-registered employee is invalid"
        const val timeEntryInvalidBadProject = "a time entry with a non-registered project is invalid"
    }
}