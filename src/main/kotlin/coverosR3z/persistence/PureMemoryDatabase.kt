package coverosR3z.persistence

import coverosR3z.domainobjects.*
import coverosR3z.exceptions.DatabaseCorruptedException
import coverosR3z.exceptions.NoTimeEntriesOnDiskException
import coverosR3z.logging.logImperative
import coverosR3z.logging.logTrace
import coverosR3z.logging.logWarn
import coverosR3z.misc.ActionQueue
import coverosR3z.persistence.surrogates.*
import coverosR3z.timerecording.TimeEntryPersistence
import java.io.File
import java.io.FileNotFoundException


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
            logTrace { "database directory was null, skipping serialization for Users" }
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
            logTrace { "database directory was null, skipping serialization for Sessions" }
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
            logTrace { "database directory was null, skipping serialization for Employees" }
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
            logTrace { "database directory was null, skipping serialization for Projects" }
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
            logTrace { "database directory was null, skipping serialization for time entries" }
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
        logTrace { "about to write to $pathname" }
        actionQueue.enqueue{ dbFileUsers.writeText(value) }
    }

    companion object {

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
                logWarn { "No time entries were found on disk, initializing new empty data" }
                ConcurrentSet()
            }
        }

        private fun readAndDeserializeUsers(dbDirectory: String): ConcurrentSet<User> {
            return try {
                val usersFile = readFile(dbDirectory, "users")
                deserializeUsers(usersFile)
            } catch (ex: FileNotFoundException) {
                logWarn { "users file missing, creating empty" }
                ConcurrentSet()
            }
        }

        private fun readAndDeserializeSessions(dbDirectory: String, users: Set<User>): ConcurrentSet<Session> {
            return try {
                val sessionsFile = readFile(dbDirectory, "sessions")
                deserializeSessions(sessionsFile, users)
            } catch (ex: FileNotFoundException) {
                logWarn { "sessions file missing, creating empty" }
                ConcurrentSet()
            }
        }

        private fun readAndDeserializeEmployees(dbDirectory: String): ConcurrentSet<Employee> {
            return try {
                val employeesFile = readFile(dbDirectory, "employees")
                deserializeEmployees(employeesFile)
            } catch (ex: FileNotFoundException) {
                logWarn { "employees file missing, creating empty" }
                ConcurrentSet()
            }
        }

        private fun readAndDeserializeProjects(dbDirectory: String): ConcurrentSet<Project> {
            return try {
                val projectsFile = readFile(dbDirectory, "projects")
                deserializeProjects(projectsFile)
            } catch (ex: FileNotFoundException) {
                logWarn { "projects file missing, creating empty" }
                ConcurrentSet()
            }
        }

        private fun deserializeProjects(projectsFile: String): ConcurrentSet<Project> {
            return projectsFile.split("\n").map{ ProjectSurrogate.deserializeToProject(it)}.toConcurrentSet()
        }

        private fun deserializeEmployees(employeesFile: String): ConcurrentSet<Employee> {
            return employeesFile.split("\n").map{ EmployeeSurrogate.deserializeToEmployee(it)}.toConcurrentSet()
        }

        private fun deserializeSessions(sessionsFile: String, users: Set<User>): ConcurrentSet<Session> {
            return sessionsFile.split("\n").map{ SessionSurrogate.deserializeToSession(it, users)}.toConcurrentSet()
        }

        private fun deserializeUsers(usersFile: String): ConcurrentSet<User> {
            return usersFile.split("\n").map{ UserSurrogate.deserializeToUser(it)}.toConcurrentSet()
        }

        private fun deserializeTimeEntries(timeEntries: String, employees: Employee, projects: ConcurrentSet<Project>): Set<TimeEntry> {
            return timeEntries.split("\n").map { TimeEntrySurrogate.deserializeToTimeEntry(it, employees, projects) }.toSet()
        }

        private fun readFile(dbDirectory: String, name : String): String {
            return File(dbDirectory + name + databaseFileSuffix).readText()
        }

    }
}