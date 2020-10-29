package coverosR3z.persistence

import coverosR3z.domainobjects.*
import coverosR3z.exceptions.EmployeeNotRegisteredException
import kotlinx.serialization.Serializable

/**
 * Why use those heavy-handed database applications when you
 * can simply store your data in simple collections?
 *
 * Here, things are simple.  Anything you need, you make.
 */
@Serializable
class PureMemoryDatabase {

    private val employees : MutableSet<Employee> = mutableSetOf()
    private val users : MutableSet<User> = mutableSetOf()
    private val projects : MutableSet<Project> = mutableSetOf()
    private val timeEntries : MutableMap<Employee, MutableSet<TimeEntry>> = mutableMapOf()

    fun addTimeEntry(timeEntry : TimeEntryPreDatabase) {
        var employeeTimeEntries = timeEntries[timeEntry.employee]
        if (employeeTimeEntries == null) {
            employeeTimeEntries = mutableSetOf()
            timeEntries[timeEntry.employee] = employeeTimeEntries
        }
        val newIndex = employeeTimeEntries.size + 1
        employeeTimeEntries.add(TimeEntry(
                newIndex,
                timeEntry.employee,
                timeEntry.project,
                timeEntry.time,
                timeEntry.date,
                timeEntry.details))
    }

    fun addNewProject(projectName: ProjectName) : Int {
        val newIndex = projects.size + 1
        projects.add(Project(newIndex, projectName.value))
        return newIndex
    }

    fun addNewEmployee(employeename: EmployeeName) : Int {
        val newIndex = employees.size + 1
        employees.add(Employee(newIndex, employeename.value))
        return newIndex
    }

    fun addNewUser(userName: UserName, hash: Hash, salt: String, employeeId: Int?) : Int {
        val newIndex = users.size + 1
        users.add(User(newIndex, userName.value, hash, salt, employeeId))
        return newIndex
    }

    /**
     * gets the number of minutes a particular [Employee] has worked
     * on a certain date.
     *
     * @throws [EmployeeNotRegisteredException] if the employee isn't known.
     */
    fun getMinutesRecordedOnDate(employee: Employee, date: Date): Int {
        val employeeTimeEntries = timeEntries[employee]
                ?: if (!employees.contains(employee)) {
                    throw EmployeeNotRegisteredException()
                } else {
                    return 0
                }
        return employeeTimeEntries
                .filter { te -> te.employee.id == employee.id && te.date == date }
                .sumBy { te -> te.time.numberOfMinutes }
    }

    fun getAllTimeEntriesForEmployee(employee: Employee): List<TimeEntry> {
        val employees = checkNotNull(timeEntries[employee]) {"This employee, ${employee.name}, does not exist in the map of time entries"}
        return employees.filter{ te -> te.employee.id == employee.id}
    }

    fun getAllTimeEntriesForEmployeeOnDate(employee: Employee, date: Date): List<TimeEntry> {
        // Return an empty list if there are no entries for the employee, rather than default NPE behavior
        val employeesEntries = timeEntries[employee] ?: return emptyList()
        return employeesEntries.filter{ te -> te.employee.id == employee.id && te.date == date}
    }

    fun getUserByName(name: UserName) : User? {
        return users.singleOrNull { u -> u.name == name.value }
    }

    fun getProjectById(id: Int) : Project? {
        require(id > 0)
        return projects.singleOrNull { p -> p.id == id }
    }

    fun getEmployeeById(id: Int): Employee? {
        require(id > 0)
        return employees.singleOrNull { u -> u.id == id}
    }

    fun getAllEmployees() : List<Employee> {
        return employees.toList()
    }

    fun getAllProjects(): List<Project> {
        return projects.toList()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PureMemoryDatabase

        if (employees != other.employees) return false
        if (projects != other.projects) return false
        if (timeEntries != other.timeEntries) return false

        return true
    }

    override fun hashCode(): Int {
        var result = employees.hashCode()
        result = 31 * result + projects.hashCode()
        result = 31 * result + timeEntries.hashCode()
        return result
    }


}