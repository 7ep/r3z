package coverosR3z.persistence

import coverosR3z.domainobjects.*
import coverosR3z.exceptions.DatabaseCorruptedException
import coverosR3z.exceptions.NoTimeEntriesOnDiskException
import coverosR3z.logging.logImperative
import coverosR3z.logging.logTrace
import coverosR3z.logging.logWarn
import coverosR3z.misc.*
import coverosR3z.timerecording.TimeEntryPersistence
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
                         private val timeEntries: ConcurrentSet<TimeEntry> = ConcurrentSet(),
                         private val sessions: ConcurrentSet<Session> = ConcurrentSet(),
                         private val dbDirectory : String? = null
) {

    private val actionQueue = ActionQueue("DatabaseWriter for $this")

    fun copy(): PureMemoryDatabase {
        return PureMemoryDatabase(
            employees = this.employees.toList().toConcurrentSet(),
            users = this.users.toList().toConcurrentSet(),
            projects = this.projects.toList().toConcurrentSet(),
            timeEntries = this.timeEntries.toList().toConcurrentSet(),
            sessions = this.sessions.toList().toConcurrentSet(),
        )
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
            serializeUsersToDisk()

        return result
    }

    /**
     * carry out some action on the [User] set of data.
     * @param shouldSerialize if true, carry out serialization and persistence to disk
     * @param action a lambda to receive the set of users and do whatever you want with it
     */
    fun <R> actOnEmployees(shouldSerialize : Boolean = false, action: (ConcurrentSet<Employee>) -> R) : R
    {
        val result = action.invoke(employees)

        if (shouldSerialize)
            serializeEmployeesToDisk()

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
            serializeSessionsToDisk()

        return result
    }

    /**
     * carry out some action on the [TimeEntry] set of data.
     * @param shouldSerialize if true, carry out serialization and persistence to disk
     * @param action a lambda to receive the set of time entries and do whatever you want with it
     */
    fun <R> actOnTimeEntries(shouldSerialize : Boolean = false,
                             action: (timeEntries: ConcurrentSet<TimeEntry>) -> R) : R
    {
        val result = action.invoke(timeEntries)

        if (shouldSerialize && result is TimeEntry)
            serializeTimeEntriesToDisk(result)

        return result
    }

    /**
     * carry out some action on the [Project] set of data.
     * @param shouldSerialize if true, carry out serialization and persistence to disk
     * @param action a lambda to receive the set of projects and do whatever you want with it
     */
    fun <R> actOnProjects(shouldSerialize : Boolean = false, action: (ConcurrentSet<Project>) -> R) : R {
        val result = action.invoke(projects)

        if (shouldSerialize)
            serializeProjectsToDisk()

        return result
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


    /**
     * Write the entire set of data to a single file, overwriting if necessary
     */
    private fun serializeUsersToDisk() {
        if (dbDirectory == null) {
            logTrace("database directory was null, skipping serialization for Users")
            return
        }
        val users = users.joinToString("\n") { UserSurrogate.toSurrogate(it).serialize() }
        writeDbFile(users, "users", dbDirectory)
    }

    /**
     * Write the entire set of data to a single file, overwriting if necessary
     */
    private fun serializeSessionsToDisk() {
        if (dbDirectory == null) {
            logTrace("database directory was null, skipping serialization for Sessions")
            return
        }
        val sessions = sessions.joinToString("\n") { SessionSurrogate.toSurrogate(it).serialize() }
        writeDbFile(sessions, "sessions", dbDirectory)
    }

    /**
     * Write the entire set of data to a single file, overwriting if necessary
     */
    private fun serializeEmployeesToDisk() {
        if (dbDirectory == null) {
            logTrace("database directory was null, skipping serialization for Employees")
            return
        }
        val employees = employees.joinToString("\n") { EmployeeSurrogate.toSurrogate(it).serialize() }
        writeDbFile(employees, "employees", dbDirectory)
    }

    /**
     * Write the entire set of data to a single file, overwriting if necessary
     */
    private fun serializeProjectsToDisk() {
        if (dbDirectory == null) {
            logTrace("database directory was null, skipping serialization for Projects")
            return
        }
        val projects = projects.joinToString("\n") { ProjectSurrogate.toSurrogate(it).serialize() }
        writeDbFile(projects, "projects", dbDirectory)
    }

    /**
     * Because the time entry data set is large, we don't want to rewrite the entire thing each time a
     * user changes anything.  Instead, we want to examine what has changed and only write that.
     */
    private fun serializeTimeEntriesToDisk(timeEntry: TimeEntry) {
        if (dbDirectory == null) {
            logTrace("database directory was null, skipping serialization for time entries")
            return
        }

        val employeeDateTimeEntries = timeEntries.filter { it.employee == timeEntry.employee && it.date.month() == timeEntry.date.month() }
        val timeentriesSerialized = employeeDateTimeEntries.joinToString ("\n") { TimeEntrySurrogate.toSurrogate(it).serialize() }
        val subDirectory = dbDirectory + "timeentries/" + "${timeEntry.employee.id.value}/"
        val filename = "${timeEntry.date.year()}_${timeEntry.date.month()}"
        actionQueue.enqueue{ File(subDirectory).mkdirs() }
        writeDbFile(timeentriesSerialized, filename, subDirectory)
    }

    private fun writeDbFile(value: String, name : String, directory: String) {
        val pathname = directory + name + databaseFileSuffix
        val dbFileUsers = File(pathname)
        logTrace("about to write to $pathname")
        actionQueue.enqueue{ dbFileUsers.writeText(value) }
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

    companion object {

        private val serializedStringRegex = """ .*?: (.*?) """.toRegex()
        const val databaseFileSuffix = ".db"

        /**
         * This extends [ConcurrentHashMap] with the ability to provide
         * atomic access to the index counter, for building id's
         * for each new entry
         */
        val <K,V> ConcurrentHashMap<K,V>.nextIndex: AtomicInteger
            get() = AtomicInteger(this.size+1)

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
                val tep = TimeEntryPersistence(pmd)
                tep.persistNewEmployee(EmployeeName("Administrator"))

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
            val tep = TimeEntryPersistence(pmd)
            tep.persistNewEmployee(EmployeeName("Administrator"))

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
            projects: ConcurrentSet<Project>) : ConcurrentSet<TimeEntry> {
            val timeEntriesDirectory = "timeentries/"
            return try {
                val fullTimeEntries: ConcurrentSet<TimeEntry> = ConcurrentSet()

                for (employeeDirectory: File in File(dbDirectory + timeEntriesDirectory).listFiles()?.filter { it.isDirectory } ?: throw NoTimeEntriesOnDiskException()) {
                    val employee : Employee = try {
                        employees.single { it.id == EmployeeId.make(employeeDirectory.name) }
                    } catch (ex : NoSuchElementException) {
                        throw DatabaseCorruptedException("Unable to find an employee with the id of ${employeeDirectory.name} based on entry in $timeEntriesDirectory")
                    }
                    val simpleTimeEntries = mutableSetOf<TimeEntry>()

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

                    fullTimeEntries.addAll(simpleTimeEntries)
                }

                fullTimeEntries
            } catch (ex : NoTimeEntriesOnDiskException) {
                logWarn("No time entries were found on disk, initializing new empty data")
                ConcurrentSet()
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
}