package coverosR3z.persistence

import coverosR3z.domainobjects.*
import coverosR3z.exceptions.EmployeeNotRegisteredException
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.SetSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import java.io.File
import java.lang.IllegalStateException

/**
 * Why use those heavy-handed database applications when you
 * can simply store your data in simple collections?
 *
 * Here, things are simple.  Anything you need, you make.
 */
class PureMemoryDatabase(private val employees: MutableSet<Employee> = mutableSetOf(),
                         private val users: MutableSet<User> = mutableSetOf(),
                         private val projects: MutableSet<Project> = mutableSetOf(),
                         private val timeEntries: MutableMap<Employee, MutableMap<Date, MutableSet<TimeEntry>>> = mutableMapOf(),
                        /**
                         * a map between randomly-created letter-number strings, and a given
                         * user.  If a user exists in this data, it means they are currently authenticated.
                         */
                         private @Serializable val sessions: MutableMap<String, Session> = mutableMapOf()
) {


    fun copy(): PureMemoryDatabase {
        return PureMemoryDatabase(
            employees = this.employees.map{Employee(it.id, it.name)}.toMutableSet(),
            users = this.users.map{User(it.id, it.name, it.hash, it.salt, it.employeeId)}.toMutableSet(),
            projects = this.projects.map{Project(it.id, it.name)}.toMutableSet(),
            timeEntries = this.timeEntries.map {Pair(it.key, it.value)}.toMap(mutableMapOf()),
            sessions = this.sessions.map {Pair(it.key, it.value)}.toMap(mutableMapOf())
        )
    }

    fun addTimeEntry(timeEntry : TimeEntryPreDatabase, timeEntries : MutableMap<Employee, MutableMap<Date, MutableSet<TimeEntry>>> = this.timeEntries) {
        addTimeEntryStatic(timeEntries, timeEntry.date, timeEntry.project, timeEntry.employee, timeEntry.time, timeEntry.details, null)
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
    fun getMinutesRecordedOnDate(employee: Employee, date: Date): Time {
        // if we're asking for time on a non-registered employee, throw an exception
        if (!employees.contains(employee)) {
            throw EmployeeNotRegisteredException()
        }

        val employeeTimeEntries = timeEntries[employee] ?: return Time(0)

        // if the employee hasn't entered any time on this date, return 0 minutes
        val entriesOnDate = employeeTimeEntries[date] ?: return Time(0)

        val sum = entriesOnDate.sumBy{ it.time.numberOfMinutes }
        return Time(sum)
    }

    /**
     * Return the list of entries for this employee, or just return an empty list otherwise
     */
    fun getAllTimeEntriesForEmployee(employee: Employee): Map<Date, Set<TimeEntry>> {
        return timeEntries[employee] ?: emptyMap()
    }

    fun getAllTimeEntriesForEmployeeOnDate(employee: Employee, date: Date): Set<TimeEntry> {
        return timeEntries[employee]?.get(date) ?: emptySet()
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
        sessions[sessionToken] = Session(user, time)
    }

    fun getUserBySessionToken(sessionToken: String): User {
        return sessions[sessionToken]?.user ?: NO_USER
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

    companion object {
        private val jsonSerializer : Json = Json{prettyPrint = true; allowStructuredMapKeys = true}
        val databaseFileName = "pmd_records_"
        val databaseFileSuffix = ".json"

        fun serializeToDisk(pmd: PureMemoryDatabase, directory: String) {
            val projects = serializeProjects(pmd)
            val employees = serializeEmployees(pmd)
            val sessions = serializeSessions(pmd)
            val users = serializeUsers(pmd)

            File(directory).mkdirs()

            writeToDisk(directory, projects, "projects")
            writeToDisk(directory, employees, "employees")
            writeToDisk(directory, sessions, "sessions")
            writeToDisk(directory, users, "users")
            writeTimeEntriesToDisk(pmd, directory)

        }

        /**
         * The time entries data structure is a bit more complex, since we are grouping
         * it by employee, and then within that, by date, and within that, the set of time
         * entries for that date.
         */
        private fun writeTimeEntriesToDisk(pmd: PureMemoryDatabase, directory: String) {
            for (employee in pmd.timeEntries.keys) {
                for (date: Date in pmd.timeEntries[employee]!!.keys) {
                    val timeentries = pmd.timeEntries[employee]?.get(date)?.toSet() ?: emptySet()
                    val timeentriesSerialized = jsonSerializer.encodeToString(SetSerializer(TimeEntry.serializer()), timeentries)
                    val subDirectory = directory + "${employee.id.value}/${date.stringValue}/"
                    File(subDirectory).mkdirs()
                    val dbFileTimeEntries = File(subDirectory + databaseFileName + "timeentries" + databaseFileSuffix)
                    dbFileTimeEntries.writeText(timeentriesSerialized)
                }
            }
        }

        private fun writeToDisk(directory: String, value: String, name : String) {
            val dbFileUsers = File(directory + databaseFileName + name + databaseFileSuffix)
            dbFileUsers.writeText(value)
        }

        private fun serializeUsers(pmd: PureMemoryDatabase): String {
            return jsonSerializer.encodeToString(SetSerializer(User.serializer()), pmd.users)
        }

        private fun serializeSessions(pmd: PureMemoryDatabase): String {
            return jsonSerializer.encodeToString(MapSerializer(String.serializer(), Session.serializer()), pmd.sessions)
        }

        private fun serializeEmployees(pmd: PureMemoryDatabase): String {
            return jsonSerializer.encodeToString(SetSerializer(Employee.serializer()), pmd.employees)
        }

        private fun serializeProjects(pmd: PureMemoryDatabase): String {
            return jsonSerializer.encodeToString(SetSerializer(Project.serializer()), pmd.projects)
        }

        fun deserializeFromDisk(directory: String) : PureMemoryDatabase {
            val projectsFile = readFile(directory, "projects")
            val employeesFile = readFile(directory, "employees")
            val sessionsFile = readFile(directory, "sessions")
            val usersFile = readFile(directory, "users")

            val projects = jsonSerializer.decodeFromString( SetSerializer(Project.serializer()), projectsFile).toMutableSet()
            val employees = jsonSerializer.decodeFromString( SetSerializer(Employee.serializer()), employeesFile).toMutableSet()
            val sessions = jsonSerializer.decodeFromString( MapSerializer(String.serializer(), Session.serializer()), sessionsFile).toMutableMap()
            val users = jsonSerializer.decodeFromString( SetSerializer(User.serializer()), usersFile).toMutableSet()

            val fullTimeEntries : MutableMap<Employee, MutableMap<Date, MutableSet<TimeEntry>>> = mutableMapOf()
            for (employeeDirectory: File in File(directory).listFiles()?.filter { it.isDirectory } ?: throw IllegalStateException("no files found in top directory")) {
                for (dateDirectory : File in employeeDirectory.listFiles()?.filter{ it.isDirectory } ?: throw IllegalStateException("no files found in employees directory")) {
                    val employee: Employee = employees.single { it.id == EmployeeId.make(employeeDirectory.name) }
                    val date : Date = Date.make(dateDirectory.name)
                    val dbTimeEntries = File(dateDirectory, databaseFileName + "timeentries" + databaseFileSuffix)
                    val timeEntriesFile = dbTimeEntries.readText()
                    val timeEntries: Set<TimeEntry> = jsonSerializer.decodeFromString( SetSerializer(TimeEntry.serializer()), timeEntriesFile)
                    for (te in timeEntries) {
                        addTimeEntryStatic(fullTimeEntries, date, te.project, employee, te.time, te.details, te.id)
                    }
                }
            }

            return PureMemoryDatabase(employees, users, projects, fullTimeEntries, sessions)
        }

        private fun readFile(directory: String, name : String): String {
            return File(directory + databaseFileName + name + databaseFileSuffix).readText()
        }

        /**
         * Static version of this code so we can use it during deserialization as well as
         * for regular usage
         */
        private fun addTimeEntryStatic(timeEntries: MutableMap<Employee, MutableMap<Date, MutableSet<TimeEntry>>>,
                                       date: Date, project: Project, employee : Employee, time : Time, details : Details, index : Int?) {
            // get the data for a particular employee
            var employeeTimeEntries = timeEntries[employee]

            // if the data is null (the employee has never added time entries), create an empty map for them
            // and set that as the variable we'll use for the rest of this method
            if (employeeTimeEntries == null) {
                employeeTimeEntries = mutableMapOf()
                timeEntries[employee] = employeeTimeEntries
            }

            // get the data for a particular employee and date
            var employeeTimeDateEntries = employeeTimeEntries[date]

            // if the data is null on a particular date, create an empty map for that date
            // and set that as the variable we'll use for the rest of this method
            if (employeeTimeDateEntries == null) {
                employeeTimeDateEntries = mutableSetOf()
                employeeTimeEntries[date] = employeeTimeDateEntries
            }

            // add the new data
            employeeTimeDateEntries.add(TimeEntry(
                    index ?: employeeTimeDateEntries.size + 1,
                    employee,
                    project,
                    time,
                    date,
                    details))
        }

    }

}