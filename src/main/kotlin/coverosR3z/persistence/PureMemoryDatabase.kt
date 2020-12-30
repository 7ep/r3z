package coverosR3z.persistence

import coverosR3z.domainobjects.*
import coverosR3z.exceptions.DatabaseCorruptedException
import coverosR3z.exceptions.NoTimeEntriesOnDiskException
import coverosR3z.logging.logImperative
import coverosR3z.logging.logTrace
import coverosR3z.logging.logWarn
import coverosR3z.misc.*
import java.io.File
import java.io.FileNotFoundException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * Why use those heavy-handed database applications when you
 * can simply store your data in simple collections?
 *
 * Here, things are simple.  Anything you need, you make.
 *
 * @param dbDirectory if this is null, the database won't use the disk at all.  If you set it to a directory, like
 *                      File("db/") the database will use that directory for all persistence.
 */
class PureMemoryDatabase(private val employees: ConcurrentSet<Employee> = ConcurrentSet(),
                         private val users: ConcurrentSet<User> = ConcurrentSet(),
                         private val projects: ConcurrentSet<Project> = ConcurrentSet(),
                         private val timeEntries: ConcurrentHashMap<Employee, ConcurrentSet<TimeEntry>> = ConcurrentHashMap(),
                         private val sessions: ConcurrentSet<Session> = ConcurrentSet(),
                         private val dbDirectory : String? = null
) {

    /**
     * The next identifier for an employee that is created.
     */
    private val nextEmployeeIndex = AtomicInteger(employees.size()+1)
    private val nextProjectIndex = AtomicInteger(projects.size()+1)
    private val nextTimeEntryIndex = AtomicInteger(timeEntries.size+1)

    private val actionQueue = ActionQueue("DatabaseWriter")

    fun copy(): PureMemoryDatabase {
        return PureMemoryDatabase(

            employees = this.employees.map{Employee(it.id, it.name)}.toConcurrentSet(),
            users = this.users.map{User(it.id, it.name, it.hash, it.salt, it.employeeId)}.toConcurrentSet(),
            projects = this.projects.map{Project(it.id, it.name)}.toConcurrentSet(),
            timeEntries = this.timeEntries.map {Pair(it.key, it.value)}.toMap(ConcurrentHashMap()),
            sessions = this.sessions.map {Session(it.sessionId, it.user, it.dt)}.toConcurrentSet(),
        )
    }

    fun addTimeEntry(
        timeEntry: TimeEntryPreDatabase,
        timeEntries: ConcurrentHashMap<Employee, ConcurrentSet<TimeEntry>> = this.timeEntries
    ): TimeEntry {
        /**
         * Static version of this code so we can use it during deserialization as well as
         * for regular usage
         */
        // get the data for a particular employee
        var employeeTimeEntries = timeEntries[timeEntry.employee]

        // if the data is null (the employee has never added time entries), create an empty map for them
        // and set that as the variable we'll use for the rest of this method
        if (employeeTimeEntries == null) {
            employeeTimeEntries = ConcurrentSet()
            timeEntries[timeEntry.employee] = employeeTimeEntries
        }
        // add the new data
        val newIndex = nextTimeEntryIndex.getAndIncrement()

        logTrace("new time-entry index is $newIndex")
        val newTimeEntry = TimeEntry(
            newIndex,
            timeEntry.employee,
            timeEntry.project,
            timeEntry.time,
            timeEntry.date,
            timeEntry.details
        )
        employeeTimeEntries.add(newTimeEntry)
        // get all the time entries for the month, to serialize
        val allTimeEntriesForMonth: Set<TimeEntry> =
            employeeTimeEntries.filter { it.date.month() == timeEntry.date.month() }.toSet()
        val filename = "${timeEntry.date.year()}_${timeEntry.date.month()}"
        logTrace("filename to store time-entries is $filename")
        // write it to disk
        writeTimeEntriesForEmployeeOnDate(allTimeEntriesForMonth, timeEntry.employee, filename, dbDirectory)
        return newTimeEntry
        // return the time entry
    }

    fun addNewProject(projectName: ProjectName) : Project {
        logTrace("PMD: adding new project, \"${projectName.value}\"")
        val newProject = Project(ProjectId(nextProjectIndex.getAndIncrement()), ProjectName(projectName.value))
        projects.add(newProject)
        serializeProjectsToDisk(this, dbDirectory)
        return newProject
    }

    fun addNewEmployee(employeename: EmployeeName) : Employee {
        logTrace{"PMD: adding new employee, \"${employeename.value}\""}
        val newEmployee = Employee(EmployeeId(nextEmployeeIndex.getAndIncrement()), EmployeeName(employeename.value))
        employees.add(newEmployee)
        serializeEmployeesToDisk(this, dbDirectory)
        return newEmployee
    }

    /**
     * gets the number of minutes a particular [Employee] has worked
     * on a certain date.
     */
    fun getMinutesRecordedOnDate(employee: Employee, date: Date): Time {
        val employeeTimeEntries = timeEntries[employee] ?: return Time(0)

        // if the employee hasn't entered any time on this date, return 0 minutes
        val totalMinutes = employeeTimeEntries.filter { it.date == date }.sumBy { te -> te.time.numberOfMinutes }
        return Time(totalMinutes)
    }

    /**
     * Return the list of entries for this employee, or just return an empty list otherwise
     */
    fun getAllTimeEntriesForEmployee(employee: Employee): Set<TimeEntry> {
        return timeEntries[employee]?.toSet() ?: emptySet()
    }

    fun getAllTimeEntriesForEmployeeOnDate(employee: Employee, date: Date): Set<TimeEntry> {
        return timeEntries[employee]?.filter{it.date == date}?.toSet() ?: emptySet()
    }

    /**
     * carry out some action on the [User] set of data.
     * @param shouldSerialize if true, carry out serialization and persistence to disk
     * @param action a lambda to receive the set of users and do whatever you want with it
     */
    fun <R> actOnUsers(shouldSerialize : Boolean = false, action: (ConcurrentSet<User>) -> R) : R
    {
        val result = action.invoke(users)

        if (shouldSerialize)
            serializeUsersToDisk(this, dbDirectory)

        return result
    }

    /**
     * carry out some action on the [Session] set of data.
     * @param shouldSerialize if true, carry out serialization and persistence to disk
     * @param action a lambda to receive the set of sessions and do whatever you want with it
     */
    fun <R> actOnSessions(shouldSerialize : Boolean = false, action: (ConcurrentSet<Session>) -> R) : R
    {
        val result = action.invoke(sessions)

        if (shouldSerialize)
            serializeSessionsToDisk(this, dbDirectory)

        return result
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

    /**
     * Get a snapshot, a copy, at this point in time
     */
    fun getAllEmployees() : List<Employee> {
        return employees.toList()
    }

    /**
     * Get a snapshot, a copy, at this point in time
     */
    fun getAllProjects(): List<Project> {
        return projects.toList()
    }

    fun overwriteTimeEntry(employeeId: EmployeeId, id: Int, newEntry: TimeEntry) {
        val employee = employees.single { it.id == employeeId }
        val setOfTimeEntries = checkNotNull(timeEntries[employee])
        check(setOfTimeEntries.count{it.id == id} == 1) {"There must be exactly one tme entry found to edit"}
        setOfTimeEntries.add(newEntry)
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

    /**
     * This function will stop the database cleanly.
     *
     * In order to do this, we need to wait for our threads
     * to finish their work.  In particular, we
     * have offloaded our file writes to [actionQueue], which
     * has an internal thread for serializing all actions
     * on our database
     */
    fun stop() {
        actionQueue.stop()
    }

    companion object {

        private val serializedStringRegex = """ .*?: (.*?) """.toRegex()
        const val databaseFileSuffix = ".db"

        /**
         * This factory method handles the nitty-gritty about starting
         * the database with respect to the files on disk.  If you plan
         * to use the database with the disk, here's a great place to
         * start.  If you are just going to use the database in memory-only,
         * you may as well just instantiate [PureMemoryDatabase]
         */
        fun startWithDiskPersistence(dbDirectory: String) : PureMemoryDatabase {

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
                logImperative("No existing database found, building new database")
                // if nothing is there, we build a new database
                // and add a clean set of directories
                logImperative("Creating new PureMemoryDatabase")
                val pmd = PureMemoryDatabase(dbDirectory = dbDirectory)

                logImperative("creating the database directory at \"$dbDirectory\"")
                File(dbDirectory).mkdirs()

                logImperative("Writing the version of the database ($dbVersion) to version.txt")
                File(dbDirectory + "version.txt").writeText(dbVersion.toString())

                logImperative("creating an initial employee")
                pmd.addNewEmployee(EmployeeName("Administrator"))

                pmd
            }
        }

        /**
         * This starts the database with memory-only, that is
         * no disk persistence.  This is mainly
         * used for testing purposes
         */
        fun startMemoryOnly() : PureMemoryDatabase {
            val pmd = PureMemoryDatabase()

            logImperative("creating an initial employee")
            pmd.addNewEmployee(EmployeeName("Administrator"))

            return pmd
        }

        /**
         * Deserializes the database from file, or returns null if no
         * database directory exists
         */
        fun deserializeFromDisk(dbDirectory: String) : PureMemoryDatabase? {
            val topDirectory = File(dbDirectory)
            val innerFiles = topDirectory.listFiles()
            if ((! topDirectory.exists()) || innerFiles.isNullOrEmpty()) {
                logImperative("directory $dbDirectory did not exist.  Returning null for the PureMemoryDatabase")
                return null
            }

            val projects = readAndDeserializeProjects(dbDirectory)
            val users = readAndDeserializeUsers(dbDirectory)
            val sessions = readAndDeserializeSessions(dbDirectory, users.toSet())
            val employees = readAndDeserializeEmployees(dbDirectory)
            val fullTimeEntries = readAndDeserializeTimeEntries(dbDirectory, employees, projects)

            return PureMemoryDatabase(employees, users, projects, fullTimeEntries, sessions, dbDirectory)
        }

        private fun readAndDeserializeTimeEntries(
            dbDirectory: String,
            employees: ConcurrentSet<Employee>,
            projects: ConcurrentSet<Project>) : ConcurrentHashMap<Employee, ConcurrentSet<TimeEntry>> {
            val timeEntriesDirectory = "timeentries/"
            return try {
                val fullTimeEntries: ConcurrentHashMap<Employee, ConcurrentSet<TimeEntry>> = ConcurrentHashMap()

                for (employeeDirectory: File in File(dbDirectory + timeEntriesDirectory).listFiles()?.filter { it.isDirectory } ?: throw NoTimeEntriesOnDiskException()) {
                    val employee : Employee = try {
                        employees.single { it.id == EmployeeId.make(employeeDirectory.name) }
                    } catch (ex : NoSuchElementException) {
                        throw DatabaseCorruptedException("Unable to find an employee with the id of ${employeeDirectory.name} based on entry in $timeEntriesDirectory")
                    }
                    val simpleTimeEntries = ConcurrentSet<TimeEntry>()

                    // loop through all the files of time entries for this employee, collecting them
                    val timeEntryFiles = employeeDirectory.listFiles()
                    if (timeEntryFiles.isNullOrEmpty()) {
                        throw DatabaseCorruptedException("no time entry files found in employees directory at ${employeeDirectory.path}")
                    }
                    for (monthlyTimeEntries: File in timeEntryFiles.filter { it.isFile }) {
                        try {
                            simpleTimeEntries.addAll(deserializeTimeEntries(monthlyTimeEntries.readText(), employee, projects))
                        } catch (ex : DatabaseCorruptedException) {
                            throw DatabaseCorruptedException("Could not deserialize time entry file ${monthlyTimeEntries.name}.  ${ex.message}", ex.ex)
                        }
                    }

                    fullTimeEntries[employee] = simpleTimeEntries
                }

                fullTimeEntries
            } catch (ex : NoTimeEntriesOnDiskException) {
                logWarn("No time entries were found on disk, initializing new empty data")
                ConcurrentHashMap()
            }
        }

        private fun readAndDeserializeUsers(dbDirectory: String): ConcurrentSet<User> {
            return try {
                val usersFile = readFile(dbDirectory, "users")
                deserializeUsers(usersFile)
            } catch (ex: FileNotFoundException) {
                logWarn("users file missing, creating empty")
                ConcurrentSet()
            }
        }

        private fun readAndDeserializeSessions(dbDirectory: String, users: Set<User>): ConcurrentSet<Session> {
            return try {
                val sessionsFile = readFile(dbDirectory, "sessions")
                deserializeSessions(sessionsFile, users)
            } catch (ex: FileNotFoundException) {
                logWarn("sessions file missing, creating empty")
                ConcurrentSet()
            }
        }

        private fun readAndDeserializeEmployees(dbDirectory: String): ConcurrentSet<Employee> {
            return try {
                val employeesFile = readFile(dbDirectory, "employees")
                deserializeEmployees(employeesFile)
            } catch (ex: FileNotFoundException) {
                logWarn("employees file missing, creating empty")
                ConcurrentSet()
            }
        }

        private fun readAndDeserializeProjects(dbDirectory: String): ConcurrentSet<Project> {
            return try {
                val projectsFile = readFile(dbDirectory, "projects")
                deserializeProjects(projectsFile)
            } catch (ex: FileNotFoundException) {
                logWarn("projects file missing, creating empty")
                ConcurrentSet()
            }
        }

        private fun deserializeProjects(projectsFile: String): ConcurrentSet<Project> {
            return projectsFile.split("\n").map{ProjectSurrogate.deserializeToProject(it)}.toConcurrentSet()
        }

        private fun deserializeEmployees(employeesFile: String): ConcurrentSet<Employee> {
            return employeesFile.split("\n").map{EmployeeSurrogate.deserializeToEmployee(it)}.toConcurrentSet()
        }

        private fun deserializeSessions(sessionsFile: String, users: Set<User>): ConcurrentSet<Session> {
            return sessionsFile.split("\n").map{SessionSurrogate.deserializeToSession(it, users)}.toConcurrentSet()
        }

        private fun deserializeUsers(usersFile: String): ConcurrentSet<User> {
            return usersFile.split("\n").map{UserSurrogate.deserializeToUser(it)}.toConcurrentSet()
        }

        fun deserializeTimeEntries(timeEntries: String, employees: Employee, projects: ConcurrentSet<Project>): Set<TimeEntry> {
            return timeEntries.split("\n").map { TimeEntrySurrogate.deserializeToTimeEntry(it, employees, projects) }.toSet()
        }

        private fun readFile(dbDirectory: String, name : String): String {
            return File(dbDirectory + name + databaseFileSuffix).readText()
        }

    }


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
        actionQueue.enqueue{ File(subDirectory).mkdirs() }
        writeDbFile(timeentriesSerialized, filename, subDirectory)
    }

    private fun writeDbFile(value: String, name : String, dbDirectory: String) {
        val pathname = dbDirectory + name + databaseFileSuffix
        val dbFileUsers = File(pathname)
        logTrace("about to write to $pathname")
        actionQueue.enqueue{ dbFileUsers.writeText(value) }
    }

    fun serializeTimeEntries(employeeDateTimeEntries: Set<TimeEntry>): String {
        return employeeDateTimeEntries.joinToString ("\n") { TimeEntrySurrogate.toSurrogate(it).serialize() }
    }

    private fun serializeUsers(pmd: PureMemoryDatabase): String {
        return pmd.users.joinToString("\n") { UserSurrogate.toSurrogate(it).serialize() }
    }

    private fun serializeSessions(pmd: PureMemoryDatabase): String {
        return pmd.sessions.joinToString("\n") { SessionSurrogate.toSurrogate(it).serialize() }
    }

    private fun serializeEmployees(pmd: PureMemoryDatabase): String {
        return pmd.employees.joinToString("\n") { EmployeeSurrogate.toSurrogate(it).serialize() }
    }

    private fun serializeProjects(pmd: PureMemoryDatabase): String {
        return pmd.projects.joinToString("\n") { ProjectSurrogate.toSurrogate(it).serialize() }
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
    data class TimeEntrySurrogate(val i: Int, val e: Int, val p: Int, val t : Int, val d : Int, val dtl: String) {

        fun serialize(): String {
            return """{ i: $i , e: $e , p: $p , t: $t , d: $d , dtl: ${encode(dtl)} }"""
        }

        companion object {

            fun deserialize(str: String): TimeEntrySurrogate {
                try {
                    val groups = checkNotNull(serializedStringRegex.findAll(str)).flatMap{it.groupValues}.toList()
                    val id = checkParseToInt(groups[1])
                    val empId = checkParseToInt(groups[3])
                    val projId = checkParseToInt(groups[5])
                    val time = checkParseToInt(groups[7])
                    val date = checkParseToInt(groups[9])
                    val details = decode(groups[11])
                    return TimeEntrySurrogate(id, empId, projId, time, date, details)
                } catch (ex : Throwable) {
                    throw DatabaseCorruptedException("Unable to deserialize this text as time entry data: $str", ex)
                }
            }

            fun deserializeToTimeEntry(str: String, employee: Employee, projects: ConcurrentSet<Project>) : TimeEntry {
                return fromSurrogate(deserialize(str), employee, projects)
            }

            fun toSurrogate(te : TimeEntry) : TimeEntrySurrogate {
                return TimeEntrySurrogate(te.id, te.employee.id.value, te.project.id.value, te.time.numberOfMinutes, te.date.epochDay, te.details.value)
            }

            private fun fromSurrogate(te: TimeEntrySurrogate, employee: Employee, projects: ConcurrentSet<Project>) : TimeEntry {
                val project = try {
                    projects.single { it.id == ProjectId(te.p) }
                } catch (ex : NoSuchElementException) {
                    throw DatabaseCorruptedException("Unable to find a project with the id of ${te.p}.  Project set size: ${projects.size()}")
                }
                return TimeEntry(
                        te.i,
                        employee,
                        project,
                        Time(te.t),
                        Date(te.d),
                        Details(te.dtl))

            }

        }
    }

    /**
     * A surrogate. See longer description for another surrogate at [TimeEntrySurrogate]
     */
    data class UserSurrogate(val id: Int, val name: String, val hash: String, val salt: String, val empId: Int?) {

        fun serialize(): String {
            return """{ id: $id , name: ${encode(name)} , hash: ${encode(hash)} , salt: ${encode(salt)} , empId: ${empId ?: "null"} }"""
        }

        companion object {

            fun deserialize(str : String) : UserSurrogate {
                try {
                    val groups = checkNotNull(serializedStringRegex.findAll(str)).flatMap{it.groupValues}.toList()
                    val id = checkParseToInt(groups[1])
                    val empId: Int? = if (groups[9] == "null") null else checkParseToInt(groups[9])
                    return UserSurrogate(id, decode(groups[3]), decode(groups[5]), decode(groups[7]), empId)
                } catch (ex : Throwable) {
                    throw DatabaseCorruptedException("Unable to deserialize this text as user data: $str", ex)
                }
            }

            fun deserializeToUser(str : String) : User {
                return fromSurrogate(deserialize(str))
            }

            fun toSurrogate(u : User) : UserSurrogate {
                return UserSurrogate(u.id.value, u.name.value, u.hash.value, u.salt.value, u.employeeId?.value)
            }

            private fun fromSurrogate(us: UserSurrogate) : User {
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
     * A surrogate. See longer description for another surrogate at [TimeEntrySurrogate]
     */
    data class EmployeeSurrogate(val id: Int, val name: String) {

        fun serialize(): String {
            return """{ id: $id , name: ${encode(name)} }"""
        }

        companion object {

            fun deserialize(str: String): EmployeeSurrogate {
                try {
                    val groups = checkNotNull(serializedStringRegex.findAll(str)).flatMap{it.groupValues}.toList()
                    val id = checkParseToInt(groups[1])
                    return EmployeeSurrogate(id, decode(groups[3]))
                } catch (ex : Throwable) {
                    throw DatabaseCorruptedException("Unable to deserialize this text as employee data: $str", ex)
                }
            }

            fun deserializeToEmployee(str: String) : Employee {
                return fromSurrogate(deserialize(str))
            }

            fun toSurrogate(e : Employee) : EmployeeSurrogate {
                return EmployeeSurrogate(e.id.value, e.name.value)
            }

            private fun fromSurrogate(es: EmployeeSurrogate) : Employee {
                return Employee(EmployeeId(es.id), EmployeeName(es.name))
            }

        }
    }

    /**
     * A surrogate. See longer description for another surrogate at [TimeEntrySurrogate]
     */
    data class ProjectSurrogate(val id: Int, val name: String) {

        fun serialize(): String {
            return """{ id: $id , name: ${encode(name)} }"""
        }

        companion object {

            fun deserialize(str: String): ProjectSurrogate {
                try {
                    val groups = checkNotNull(serializedStringRegex.findAll(str)).flatMap{it.groupValues}.toList()
                    val id = checkParseToInt(groups[1])
                    return ProjectSurrogate(id, decode(groups[3]))
                } catch (ex : Throwable) {
                    throw DatabaseCorruptedException("Unable to deserialize this text as project data: $str", ex)
                }
            }

            fun deserializeToProject(str: String) : Project {
                return fromSurrogate(deserialize(str))
            }

            fun toSurrogate(p : Project) : ProjectSurrogate {
                return ProjectSurrogate(p.id.value, p.name.value)
            }

            private fun fromSurrogate(ps: ProjectSurrogate) : Project {
                return Project(ProjectId(ps.id), ProjectName(ps.name))
            }
        }
    }

    /**
     * A surrogate. See longer description for another surrogate at [TimeEntrySurrogate]
     */
    data class SessionSurrogate(val sessionStr : String, val id: Int, val epochSecond: Long) {

        fun serialize(): String {
            return """{ s: ${encode(sessionStr)} , id: $id , e: $epochSecond }"""
        }

        companion object {

            fun deserialize(str: String): SessionSurrogate {
                try {
                    val groups = checkNotNull(serializedStringRegex.findAll(str)).flatMap{it.groupValues}.toList()
                    val sessionString = decode(groups[1])
                    val id = checkParseToInt(groups[3])
                    val epochSecond = checkParseToLong(groups[5])
                    return SessionSurrogate(sessionString, id, epochSecond)
                } catch (ex : Throwable) {
                    throw DatabaseCorruptedException("Unable to deserialize this text as session data: $str", ex)
                }
            }

            fun deserializeToSession(str: String, users: Set<User>) : Session {
                return fromSurrogate(deserialize(str), users)
            }

            fun toSurrogate(s : Session) : SessionSurrogate {
                return SessionSurrogate(s.sessionId, s.user.id.value, s.dt.epochSecond)
            }

            private fun fromSurrogate(ss: SessionSurrogate, users: Set<User>) : Session {
                val user = try {
                    users.single { it.id.value == ss.id }
                } catch (ex : NoSuchElementException) {
                    throw DatabaseCorruptedException("Unable to find a user with the id of ${ss.id}.  User set size: ${users.size}")
                }
                return Session(ss.sessionStr, user, DateTime(ss.epochSecond))
            }
        }
    }

}