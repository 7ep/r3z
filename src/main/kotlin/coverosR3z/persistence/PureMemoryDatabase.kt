package coverosR3z.persistence

import coverosR3z.domainobjects.*
import coverosR3z.exceptions.EmployeeNotRegisteredException
import coverosR3z.exceptions.NoTimeEntriesOnDiskException
import coverosR3z.logging.logStart
import coverosR3z.logging.logTrace
import coverosR3z.logging.logWarn
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.SetSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileNotFoundException
import coverosR3z.persistence.PureMemoryDatabase.TimeEntrySerializationSurrogate.Companion as Tess

/**
 * Why use those heavy-handed database applications when you
 * can simply store your data in simple collections?
 *
 * Here, things are simple.  Anything you need, you make.
 *
 * @param dbDirectory if this is null, the database won't use the disk at all.  If you set it to a directory, like
 *                      File("db/") the database will use that directory for all persistence.
 */
class PureMemoryDatabase(private val employees: MutableSet<Employee> = mutableSetOf(),
                         private val users: MutableSet<User> = mutableSetOf(),
                         private val projects: MutableSet<Project> = mutableSetOf(),
                         private val timeEntries: MutableMap<Employee, MutableMap<Date, MutableSet<TimeEntry>>> = mutableMapOf(),

                        /**
                         * a map between randomly-created letter-number strings, and a given
                         * user.  If a user exists in this data, it means they are currently authenticated.
                         */
                         private val sessions: MutableMap<String, Session> = mutableMapOf(),
                         private val dbDirectory : String? = null
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

    @Synchronized
    fun addTimeEntry(timeEntry : TimeEntryPreDatabase, timeEntries : MutableMap<Employee, MutableMap<Date, MutableSet<TimeEntry>>> = this.timeEntries) {
        addTimeEntryStatic(timeEntries, dbDirectory, timeEntry.date, timeEntry.project, timeEntry.employee, timeEntry.time, timeEntry.details)
    }

    @Synchronized
    fun addNewProject(projectName: ProjectName) : Int {
        logTrace("PMD: adding new project, \"${projectName.value}\"")
        val newIndex = projects.size + 1
        logTrace("PMD: new project index: $newIndex")
        projects.add(Project(ProjectId(newIndex), ProjectName(projectName.value)))
        serializeProjectsToDisk(this, dbDirectory)
        return newIndex
    }

    @Synchronized
    fun addNewEmployee(employeename: EmployeeName) : Int {
        logTrace("PMD: adding new employee, \"${employeename.value}\"")
        val newIndex = employees.size + 1
        logTrace("PMD: new employee index: $newIndex")
        employees.add(Employee(EmployeeId(newIndex), EmployeeName(employeename.value)))
        serializeEmployeesToDisk(this, dbDirectory)
        return newIndex
    }

    @Synchronized
    fun addNewUser(userName: UserName, hash: Hash, salt: Salt, employeeId: EmployeeId?) : Int {
        logTrace("PMD: adding new user, \"${userName.value}\"")
        val newIndex = users.size + 1
        logTrace("PMD: new user index: $newIndex")
        users.add(User(UserId(newIndex), userName, hash, salt, employeeId))
        serializeUsersToDisk(this, dbDirectory)
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
    fun getAllTimeEntriesForEmployee(employee: Employee): Set<TimeEntry> {
        return timeEntries[employee]?.flatMap { it.value }?.toSet() ?: emptySet()
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

    fun getAllUsers(): List<User> {
        return users.toList()
    }

    fun getAllProjects(): List<Project> {
        return projects.toList()
    }

    fun getAllSessions(): Map<String, Session> {
        return sessions.toMap()
    }


    @Synchronized
    fun addNewSession(sessionToken: String, user: User, time: DateTime) {
        require (sessions[sessionToken] == null) {"There must not already exist a session for (${user.name}) if we are to create one"}
        sessions[sessionToken] = Session(user, time)
        serializeSessionsToDisk(this, dbDirectory)
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

        /**
         * This factory method handles the nitty-gritty about starting
         * the database with respect to the files on disk.  If you plan
         * to use the database with the disk, here's a great place to
         * start.  If you are just going to use the database in memory-only,
         * you may as well just instantiate [PureMemoryDatabase]
         */
        fun start(dbDirectory: String) : PureMemoryDatabase {

            /** The version of the database.  Update when we have
             * real users and we're changing live prod data.
             */
            val dbVersion = 1

            // first we assume the database has been previously persisted
            val restoredPMD = deserializeFromDisk(dbDirectory)

            return if (restoredPMD != null) {
                // return the restored database
                restoredPMD
            } else {
                logStart("No existing database found, building new database")
                // if nothing is there, we build a new database
                // and add a clean set of directories
                logStart("Creating new PureMemoryDatabase")
                val pmd = PureMemoryDatabase(dbDirectory = dbDirectory)

                logStart("creating the database directory at \"$dbDirectory\"")
                File(dbDirectory).mkdirs()

                logStart("Writing the version of the database ($dbVersion) to version.txt")
                File(dbDirectory + "version.txt").writeText(dbVersion.toString())

                logStart("creating an initial employee")
                pmd.addNewEmployee(EmployeeName("Administrator"))

                pmd
            }
        }

        private val jsonSerializer : Json = Json{prettyPrint = false; allowStructuredMapKeys = true}
        private const val databaseFileSuffix = ".json"

        private fun serializeUsersToDisk(pmd: PureMemoryDatabase, dbDirectory : String?) {
            if (dbDirectory == null) {
                logTrace("database directory was null, skipping serialization for Users")
                return
            }
            val users = serializeUsers(pmd)
            writeDbFile(users, "users", dbDirectory)
        }

        private fun serializeSessionsToDisk(pmd: PureMemoryDatabase, dbDirectory : String?) {
            if (dbDirectory == null) {
                logTrace("database directory was null, skipping serialization for Sessions")
                return
            }
            val sessions = serializeSessions(pmd)
            writeDbFile(sessions, "sessions", dbDirectory)
        }

        private fun serializeEmployeesToDisk(pmd: PureMemoryDatabase, dbDirectory : String?) {
            if (dbDirectory == null) {
                logTrace("database directory was null, skipping serialization for Employees")
                return
            }
            val employees = serializeEmployees(pmd)
            writeDbFile(employees, "employees", dbDirectory)
        }

        private fun serializeProjectsToDisk(pmd: PureMemoryDatabase, dbDirectory : String?) {
            if (dbDirectory == null) {
                logTrace("database directory was null, skipping serialization for Projects")
                return
            }
            val projects = serializeProjects(pmd)
            writeDbFile(projects, "projects", dbDirectory)
        }

        private fun writeTimeEntriesForEmployeeOnDate(employeeDateTimeEntries: Set<TimeEntry>, employee: Employee, filename: String, dbDirectory : String?) {
            if (dbDirectory == null) {
                logTrace("database directory was null, skipping serialization for time entries")
                return
            }
            val timeentriesSerialized = serializeTimeEntries(employeeDateTimeEntries)
            val subDirectory = dbDirectory + "timeentries/" + "${employee.id.value}/"
            File(subDirectory).mkdirs()
            writeDbFile(timeentriesSerialized, filename, subDirectory)
        }

        private fun writeDbFile(value: String, name : String, dbDirectory: String) {
            val pathname = dbDirectory + name + databaseFileSuffix
            val dbFileUsers = File(pathname)
            logTrace("about to write to $pathname")
            dbFileUsers.writeText(value)
        }

        fun serializeTimeEntries(employeeDateTimeEntries: Set<TimeEntry>): String {
            val minimizedTimeEntries = employeeDateTimeEntries.map { Tess.toSurrogate(it) }.toSet()
            return jsonSerializer.encodeToString(SetSerializer(Tess.serializer()), minimizedTimeEntries)
        }

        private fun serializeUsers(pmd: PureMemoryDatabase): String {
            val minimizedUsers = pmd.users.map {UserSurrogate.toSurrogate(it)}.toSet()
            return jsonSerializer.encodeToString(SetSerializer(UserSurrogate.serializer()), minimizedUsers )
        }

        private fun serializeSessions(pmd: PureMemoryDatabase): String {
            val newMap = mutableMapOf<String, SessionSurrogate>()
            pmd.sessions.mapValuesTo(newMap, {SessionSurrogate.toSurrogate (it.value)})
            return jsonSerializer.encodeToString(MapSerializer(String.serializer(), SessionSurrogate.serializer()), newMap)
        }

        private fun serializeEmployees(pmd: PureMemoryDatabase): String {
            val minimizedEmployees = pmd.employees.map {EmployeeSurrogate.toSurrogate(it)}.toSet()
            return jsonSerializer.encodeToString(SetSerializer(EmployeeSurrogate.serializer()), minimizedEmployees)
        }

        private fun serializeProjects(pmd: PureMemoryDatabase): String {
            val minimizedProjects = pmd.projects.map {ProjectSurrogate.toSurrogate(it)}.toSet()
            return jsonSerializer.encodeToString(SetSerializer(ProjectSurrogate.serializer()), minimizedProjects)
        }

        /**
         * Deserializes the database from file, or returns null if no
         * database directory exists
         */
        fun deserializeFromDisk(dbDirectory: String) : PureMemoryDatabase? {
            if (! File(dbDirectory).exists()) {
                logStart("directory $dbDirectory did not exist.  Returning null for the PureMemoryDatabase")
                return null
            }

            val projects = readAndDeserializeProjects(dbDirectory)
            val users = readAndDeserializeUsers(dbDirectory)
            val sessions = readAndDeserializeSessions(dbDirectory, users.toSet())
            val employees = readAndDeserializeEmployees(dbDirectory)
            val fullTimeEntries = readAndDeserializeTimeEntries(dbDirectory, employees, projects)

            return PureMemoryDatabase(employees, users, projects, fullTimeEntries, sessions, dbDirectory)
        }

        private fun readAndDeserializeTimeEntries(dbDirectory: String, employees: MutableSet<Employee>, projects: MutableSet<Project>): MutableMap<Employee, MutableMap<Date, MutableSet<TimeEntry>>> {
            return try {
                val fullTimeEntries: MutableMap<Employee, MutableMap<Date, MutableSet<TimeEntry>>> = mutableMapOf()

                for (employeeDirectory: File in File(dbDirectory + "timeentries/").listFiles()?.filter { it.isDirectory } ?: throw NoTimeEntriesOnDiskException()) {
                    val employee: Employee = employees.single { it.id == EmployeeId.make(employeeDirectory.name) }
                    val simpleTimeEntries = mutableSetOf<TimeEntry>()

                    // loop through all the files of time entries for this employee, collecting them
                    for (monthlyTimeEntries: File in employeeDirectory.listFiles()?.filter { it.isFile } ?: throw IllegalStateException("no files found in employees directory")) {
                        simpleTimeEntries.addAll(deserializeTimeEntries(monthlyTimeEntries.readText(), setOf(employee), projects))
                    }

                    val orderedTimeEntries: MutableMap<Date, MutableSet<TimeEntry>> = simpleTimeEntries.groupBy{it.date}.mapValues {it.value.toMutableSet()}.toMutableMap()
                    fullTimeEntries[employee] = orderedTimeEntries
                }

                fullTimeEntries
            } catch (ex : NoTimeEntriesOnDiskException) {
                logWarn("No time entries were found on disk, initializing new empty data")
                mutableMapOf()
            }
        }

        private fun readAndDeserializeUsers(dbDirectory: String): MutableSet<User> {
            return try {
                val usersFile = readFile(dbDirectory, "users")
                deserializeUsers(usersFile)
            } catch (ex: FileNotFoundException) {
                logWarn("users file missing, creating empty")
                mutableSetOf()
            }
        }

        private fun readAndDeserializeSessions(dbDirectory: String, users: Set<User>): MutableMap<String, Session> {
            return try {
                val sessionsFile = readFile(dbDirectory, "sessions")
                deserializeSessions(sessionsFile, users)
            } catch (ex: FileNotFoundException) {
                logWarn("sessions file missing, creating empty")
                mutableMapOf()
            }
        }

        private fun readAndDeserializeEmployees(dbDirectory: String): MutableSet<Employee> {
            return try {
                val employeesFile = readFile(dbDirectory, "employees")
                deserializeEmployees(employeesFile)
            } catch (ex: FileNotFoundException) {
                logWarn("employees file missing, creating empty")
                mutableSetOf()
            }
        }

        private fun readAndDeserializeProjects(dbDirectory: String): MutableSet<Project> {
            return try {
                val projectsFile = readFile(dbDirectory, "projects")
                deserializeProjects(projectsFile)
            } catch (ex: FileNotFoundException) {
                logWarn("projects file missing, creating empty")
                mutableSetOf()
            }
        }

        private fun deserializeProjects(projectsFile: String): MutableSet<Project> {
            val read: Set<ProjectSurrogate> = jsonSerializer.decodeFromString(SetSerializer(ProjectSurrogate.serializer()), projectsFile)
            return read.map {ProjectSurrogate.fromSurrogate(it)}.toMutableSet()
        }

        private fun deserializeEmployees(employeesFile: String): MutableSet<Employee> {
            val read: Set<EmployeeSurrogate> = jsonSerializer.decodeFromString(SetSerializer(EmployeeSurrogate.serializer()), employeesFile)
            return read.map {EmployeeSurrogate.fromSurrogate(it)}.toMutableSet()
        }

        private fun deserializeSessions(sessionsFile: String, users: Set<User>): MutableMap<String, Session> {
            val read: Map<String, SessionSurrogate> = jsonSerializer.decodeFromString(MapSerializer(String.serializer(), SessionSurrogate.serializer()), sessionsFile)
            val newMap = mutableMapOf<String, Session>()
            read.mapValuesTo(newMap, {SessionSurrogate.fromSurrogate(it.value, users)})
            return newMap
        }

        private fun deserializeUsers(usersFile: String): MutableSet<User> {
            val read: Set<UserSurrogate> = jsonSerializer.decodeFromString(SetSerializer(UserSurrogate.serializer()), usersFile)
            return read.map {UserSurrogate.fromSurrogate(it)}.toMutableSet()
        }

        fun deserializeTimeEntries(timeEntriesFile: String, employees: Set<Employee>, projects: Set<Project>): Set<TimeEntry> {
            val tessEntries = jsonSerializer.decodeFromString(SetSerializer(Tess.serializer()), timeEntriesFile)
            return tessEntries.map { Tess.fromSurrogate(it, employees, projects) }.toSet()
        }

        private fun readFile(dbDirectory: String, name : String): String {
            return File(dbDirectory + name + databaseFileSuffix).readText()
        }

        /**
         * Static version of this code so we can use it during deserialization as well as
         * for regular usage
         */
        private fun addTimeEntryStatic(timeEntries: MutableMap<Employee, MutableMap<Date, MutableSet<TimeEntry>>>, dbDirectory: String?,
                                       date: Date, project: Project, employee : Employee, time : Time, details : Details) {
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
            val newIndex = employeeTimeDateEntries.size + 1
            logTrace("new time-entry index is $newIndex")

            employeeTimeDateEntries.add(TimeEntry(newIndex, employee, project, time, date, details))

            // get all the time entries for the month, to serialize
            val allTimeEntriesForMonth: Set<TimeEntry> = employeeTimeEntries.flatMap { it.value.filter { it.date.month() == date.month()} }.toSet()
            val filename = "${date.year()}_${date.month()}"
            logTrace("filename to store time-entries is $filename")

            // write it to disk
            writeTimeEntriesForEmployeeOnDate(allTimeEntriesForMonth, employee, filename, dbDirectory)
        }

    }


    /**
     * Don't be alarmed, this is just a sneaky way to create far smaller text
     * files when we serialize [TimeEntry].
     *
     * Instead of all the types, we just do what we can to store the raw
     * values in a particular order, which cuts down the size by like 95%
     *
     * So basically, right before serializing we convert our list of time
     * entries to this, and right after deserializing we convert this to
     * full time entries. Win-Win!
     *
     * We do throw a lot of information away when we convert this over.  We'll
     * see if that hurts our performance.
     *
     * @param i the integer identifier of the Time Entry
     * @param e the [Employee] id
     * @param p the [Project] id
     * @param t the [Time], in minutes
     * @param d the [Date], as epoch days
     * @param dtl the [Details], as a string
     */
    @Serializable
    private data class TimeEntrySerializationSurrogate(val i: Int, val e: Int, val p: Int, val t : Int, val d : Int, val dtl: String) {
        companion object {

            fun toSurrogate(te : TimeEntry) : TimeEntrySerializationSurrogate {
                return TimeEntrySerializationSurrogate(te.id, te.employee.id.value, te.project.id.value, te.time.numberOfMinutes, te.date.epochDay, te.details.value)
            }

            fun fromSurrogate(te: TimeEntrySerializationSurrogate, employees: Set<Employee>, projects: Set<Project>) : TimeEntry {
                return TimeEntry(te.i, employees.single { it.id == EmployeeId(te.e)}, projects.single { it.id == ProjectId(te.p)}, Time(te.t), Date(te.d), Details(te.dtl))
            }

        }
    }

    /**
     * A surrogate. See longer description for another surrogate at [TimeEntrySerializationSurrogate]
     */
    @Serializable
    private data class UserSurrogate(val id: Int, val name: String, val hash: String, val salt: String, val empId: Int?) {
        companion object {

            fun toSurrogate(u : User) : UserSurrogate {
                return UserSurrogate(u.id.value, u.name.value, u.hash.value, u.salt.value, u.employeeId?.value)
            }

            fun fromSurrogate(us: UserSurrogate) : User {
                val empId : EmployeeId? = if (us.empId == null) {
                    null
                } else {
                    EmployeeId(us.empId)
                }
                return User(UserId(us.id), UserName(us.name), Hash(us.hash), Salt(us.salt), empId)
            }

        }
    }

    /**
     * A surrogate. See longer description for another surrogate at [TimeEntrySerializationSurrogate]
     */
    @Serializable
    private data class EmployeeSurrogate(val id: Int, val name: String) {
        companion object {

            fun toSurrogate(e : Employee) : EmployeeSurrogate {
                return EmployeeSurrogate(e.id.value, e.name.value)
            }

            fun fromSurrogate(es: EmployeeSurrogate) : Employee {
                return Employee(EmployeeId(es.id), EmployeeName(es.name))
            }

        }
    }

    /**
     * A surrogate. See longer description for another surrogate at [TimeEntrySerializationSurrogate]
     */
    @Serializable
    private data class ProjectSurrogate(val id: Int, val name: String) {
        companion object {

            fun toSurrogate(p : Project) : ProjectSurrogate {
                return ProjectSurrogate(p.id.value, p.name.value)
            }

            fun fromSurrogate(ps: ProjectSurrogate) : Project {
                return Project(ProjectId(ps.id), ProjectName(ps.name))
            }

        }
    }

    /**
     * A surrogate. See longer description for another surrogate at [TimeEntrySerializationSurrogate]
     */
    @Serializable
    private data class SessionSurrogate(val id: Int, val epochSecond: Long) {
        companion object {

            fun toSurrogate(s : Session) : SessionSurrogate {
                return SessionSurrogate(s.user.id.value, s.dt.epochSecond)
            }

            fun fromSurrogate(ss: SessionSurrogate, users: Set<User>) : Session {
                val user = users.single{it.id.value == ss.id}
                return Session(user, DateTime(ss.epochSecond))
            }

        }
    }

}