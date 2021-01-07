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
        return pmd.actOnTimeEntries (shouldSerialize = true) { entries ->

            // add the new data
            val newIndex = entries.nextIndex.getAndIncrement()

            logTrace { "new time-entry index is $newIndex" }
            val newTimeEntry = TimeEntry(
                newIndex,
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

    /**
     * This will throw an exception if the project or employee in
     * this timeentry don't exist in the list of projects / employees
     */
    private fun isEntryValid(entry: TimeEntryPreDatabase) {
        check(getProjectById(entry.project.id) != NO_PROJECT) {"a time entry with no project is invalid"}
        check(getEmployeeById(entry.employee.id) != NO_EMPLOYEE) {"a time entry with no employee is invalid"}
    }

    override fun persistNewProject(projectName: ProjectName): Project {
        return pmd.actOnProjects (shouldSerialize = true) { projects ->
            val newProject = Project(ProjectId(projects.nextIndex.getAndIncrement()), ProjectName(projectName.value))
            projects.add(newProject)
            logDebug { "Recorded a new project, \"${projectName.value}\", id: ${newProject.id.value}, to the database" }
            newProject
        }
    }

    override fun persistNewEmployee(employeename: EmployeeName): Employee {
        return pmd.actOnEmployees (shouldSerialize = true) { employees ->
            val newEmployee = Employee(EmployeeId(employees.nextIndex.getAndIncrement()), EmployeeName(employeename.value))
            employees.add(newEmployee)
            logDebug { "Recorded a new employee, \"${employeename.value}\", id: ${newEmployee.id.value}, to the database" }
            newEmployee
        }
    }

    override fun queryMinutesRecorded(employee: Employee, date: Date): Time {
        return pmd.actOnTimeEntries(action =
        fun(timeEntries: ConcurrentSet<TimeEntry>): Time {

            // if the employee hasn't entered any time on this date, return 0 minutes
            val totalMinutes = timeEntries.filter { it.date == date && it.employee == employee }.sumBy { te -> te.time.numberOfMinutes }
            return Time(totalMinutes)
        })
    }

    override fun readTimeEntries(employee: Employee): Set<TimeEntry> {
        return pmd.actOnTimeEntries { timeEntries -> timeEntries.filter { it.employee == employee } }.toSet()
    }

    override fun readTimeEntriesOnDate(employee: Employee, date: Date): Set<TimeEntry> {
        return pmd.actOnTimeEntries { timeEntries -> timeEntries.filter { it.employee == employee && it.date == date } }.toSet()
    }

    override fun getProjectByName(name: ProjectName): Project {
        return pmd.actOnProjects { it.singleOrNull { p -> p.name == name } ?: NO_PROJECT }
    }

    override fun getProjectById(id: ProjectId): Project {
        return pmd.actOnProjects { it.singleOrNull { p -> p.id == id } ?: NO_PROJECT }
    }

    override fun getAllProjects(): List<Project> {
        return pmd.actOnProjects { it.toList() }
    }

    override fun getAllEmployees(): List<Employee> {
        return pmd.actOnEmployees { it.toList() }
    }

    override fun getEmployeeById(id: EmployeeId): Employee {
        return pmd.actOnEmployees { employees -> employees.singleOrNull {it.id == id} ?: NO_EMPLOYEE }
    }

    override fun overwriteTimeEntry(empId: EmployeeId, id: Int, newEntry: TimeEntry)  {
        val employee = getEmployeeById(empId)
        pmd.actOnTimeEntries {
            val setOfTimeEntries = readTimeEntries(employee)
            check(setOfTimeEntries.count{it.id == id} == 1) {"There must be exactly one tme entry found to edit"}
            //TODO - finish this
        }

    }

}