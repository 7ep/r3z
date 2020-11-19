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

    /**
     * a map between randomly-created letter-number strings, and a given
     * user.  If a user exists in this data, it means they are currently authenticated.
     */
    private val sessions : MutableMap<String, Pair<User, DateTime>> = mutableMapOf()

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
        projects.add(Project(ProjectId(newIndex), ProjectName(projectName.value)))
        return newIndex
    }

    fun addNewEmployee(employeename: EmployeeName) : Int {
        val newIndex = employees.size + 1
        employees.add(Employee(EmployeeId(newIndex), EmployeeName(employeename.value)))
        return newIndex
    }

    fun addNewUser(userName: UserName, hash: Hash, salt: Salt, employeeId: EmployeeId?) : Int {
        val newIndex = users.size + 1
        users.add(User(UserId(newIndex), userName, hash, salt, employeeId))
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

    /**
     * Return the list of entries for this employee, or just return an empty list otherwise
     */
    fun getAllTimeEntriesForEmployee(employee: Employee): List<TimeEntry> {
        return timeEntries[employee]?.filter{ te -> te.employee.id == employee.id} ?: emptyList()
    }

    fun getAllTimeEntriesForEmployeeOnDate(employee: Employee, date: Date): List<TimeEntry> {
        // Return an empty list if there are no entries for the employee, rather than default NPE behavior
        val employeesEntries = timeEntries[employee] ?: return emptyList()
        return employeesEntries.filter{ te -> te.employee.id == employee.id && te.date == date}
    }

    fun getUserByName(name: UserName) : User {
        return users.singleOrNull { u -> u.name == name } ?: NO_USER
    }

    fun getProjectById(id: ProjectId) : Project {
        return projects.singleOrNull { p -> p.id == id } ?: NO_PROJECT
    }

    fun getProjectByName(name: ProjectName): Project {
        return projects.singleOrNull { p -> p.name == name } ?: NO_PROJECT
    }

    fun getEmployeeById(id: EmployeeId): Employee {
        return employees.singleOrNull {it.id == id} ?: NO_EMPLOYEE
    }

    fun getAllEmployees() : List<Employee> {
        return employees.toList()
    }

    fun getAllProjects(): List<Project> {
        return projects.toList()
    }


    fun addNewSession(sessionToken: String, user: User, time: DateTime) {
        require (sessions[sessionToken] == null) {"There must not already exist a session for (${user.name}) if we are to create one"}
        sessions[sessionToken] = Pair(user, time)
    }

    fun getUserBySessionToken(sessionToken: String): User {
        return sessions[sessionToken]?.first ?: NO_USER
    }

    fun removeSessionByToken(sessionToken: String) {
        checkNotNull(sessions[sessionToken]) {"There must exist a session in the database for ($sessionToken) in order to delete it"}
        sessions.remove(sessionToken)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PureMemoryDatabase

        if (employees != other.employees) return false
        if (users != other.users) return false
        if (projects != other.projects) return false
        if (timeEntries != other.timeEntries) return false
        if (sessions != other.sessions) return false

        return true
    }

    override fun hashCode(): Int {
        var result = employees.hashCode()
        result = 31 * result + users.hashCode()
        result = 31 * result + projects.hashCode()
        result = 31 * result + timeEntries.hashCode()
        result = 31 * result + sessions.hashCode()
        return result
    }


}