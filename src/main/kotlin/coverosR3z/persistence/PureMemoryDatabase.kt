package coverosR3z.persistence

import coverosR3z.domainobjects.*
import coverosR3z.exceptions.DatabaseCorruptedException
import coverosR3z.exceptions.NoTimeEntriesOnDiskException
import coverosR3z.logging.logImperative
import coverosR3z.logging.logTrace
import coverosR3z.logging.logWarn
import coverosR3z.misc.ActionQueue
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

    private val actionQueue = ActionQueue("DatabaseWriter")

    fun copy(): PureMemoryDatabase {
        return PureMemoryDatabase(
            employees = this.employees.toList().toConcurrentSet(),
            users = this.users.toList().toConcurrentSet(),
            projects = this.projects.toList().toConcurrentSet(),
            timeEntries = this.timeEntries.toList().toConcurrentSet(),
            sessions = this.sessions.toList().toConcurrentSet(),
        )
    }

    ////////////////////////////////////
    //   DATA ACCESS
    ////////////////////////////////////

    /**
     * carry out some action on the [User] set of data.
     * @param shouldSerialize if true, carry out serialization and persistence to disk
     * @param action a lambda to receive the set of users and do whatever you want with it
     */
    fun <R> actOnUsers(shouldSerialize : Boolean = false, action: (ConcurrentSet<User>) -> R) : R {
        val result = action.invoke(users)

        if (shouldSerialize)
            serializeToDisk(users, "users")

        return result
    }

    /**
     * carry out some action on the [User] set of data.
     * @param shouldSerialize if true, carry out serialization and persistence to disk
     * @param action a lambda to receive the set of users and do whatever you want with it
     */
    fun <R> actOnEmployees(shouldSerialize : Boolean = false, action: (ConcurrentSet<Employee>) -> R) : R {
        val result = action.invoke(employees)

        if (shouldSerialize)
            serializeToDisk(employees, "employees")

        return result
    }

    /**
     * carry out some action on the [Session] set of data.
     * @param shouldSerialize if true, carry out serialization and persistence to disk
     * @param action a lambda to receive the set of sessions and do whatever you want with it
     */
    fun <R> actOnSessions(shouldSerialize : Boolean = false, action: (ConcurrentSet<Session>) -> R) : R {
        val result = action.invoke(sessions)

        if (shouldSerialize)
            serializeToDisk(sessions, "sessions")

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
            serializeToDisk(projects, "projects")

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


    ////////////////////////////////////
    //   BOILERPLATE
    ////////////////////////////////////



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


    ////////////////////////////////////
    //   DATABASE CONTROL
    ////////////////////////////////////


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


    ////////////////////////////////////
    //   SERIALIZATION
    ////////////////////////////////////

    private fun <T: Serializable> serializeToDisk(set : ConcurrentSet<T>, filename: String) {
        if (dbDirectory == null) {
            logTrace { "database directory was null, skipping serialization for $filename" }
            return
        }
        val joined = set.joinToString("\n") { it.serialize() }
        writeDbFile(joined, filename, dbDirectory)
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
        val timeentriesSerialized = employeeDateTimeEntries.joinToString ("\n") { it.serialize() }
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

        /**
         * The suffix for the database files we will write to disk
         */
        const val databaseFileSuffix = ".db"
        val serializedStringRegex = """ .*?: (.*?) """.toRegex()


        /**
         * Used by the classes needing serialization to avoid a bit of boilerplate
         */
        fun <T: Any> deserializer(str : String, clazz: Class<T>, convert: (List<String>) -> T) : T {
            try {
                val groups = checkNotNull(serializedStringRegex.findAll(str)).flatMap { it.groupValues }.toList()
                return convert(groups)
            } catch (ex : DatabaseCorruptedException) {
                throw ex
            }catch (ex : Throwable) {
                throw DatabaseCorruptedException("Unable to deserialize this text as ${clazz.simpleName} data: $str", ex)
            }
        }

        /**
         * This factory method handles the nitty-gritty about starting
         * the database with respect to the files on disk.  If you plan
         * to use the database with the disk, here's a great place to
         * start.  If you are just going to use the database in memory-only,
         * check out [startMemoryOnly]
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
         * used for testing purposes.  For production use,
         * check out [startWithDiskPersistence]
         */
        fun startMemoryOnly() : PureMemoryDatabase {
            val pmd = PureMemoryDatabase()

            logImperative("creating an initial employee")
            val tep = TimeEntryPersistence(pmd)
            tep.persistNewEmployee(EmployeeName("Administrator"))

            return pmd
        }

        /**
         * Deserializes the database from files, or returns null if no
         * database directory exists
         */
        fun deserializeFromDisk(dbDirectory: String): PureMemoryDatabase? {
            val topDirectory = File(dbDirectory)
            val innerFiles = topDirectory.listFiles()
            if ((!topDirectory.exists()) || innerFiles.isNullOrEmpty()) {
                logImperative("directory $dbDirectory did not exist.  Returning null for the PureMemoryDatabase")
                return null
            }

            val projects = readAndDeserialize(dbDirectory, "projects") { Project.deserialize(it) }
            val users = readAndDeserialize(dbDirectory, "users") { User.deserialize(it) }
            val sessions = readAndDeserialize(dbDirectory, "sessions") { Session.deserialize(it, users.toSet()) }
            val employees = readAndDeserialize(dbDirectory, "employees") { Employee.deserialize(it) }
            val fullTimeEntries = readAndDeserializeTimeEntries(dbDirectory, employees.toSet(), projects.toSet())

            return PureMemoryDatabase(employees, users, projects, fullTimeEntries, sessions, dbDirectory)
        }

        private fun readAndDeserializeTimeEntries(
            dbDirectory: String,
            employees: Set<Employee>,
            projects: Set<Project>) : ConcurrentSet<TimeEntry> {
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
                            simpleTimeEntries.addAll(
                                monthlyTimeEntries.readText().split("\n")
                                    .map { TimeEntry.deserialize(it, employee, projects) }.toSet()
                            )
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

        private fun readFile(dbDirectory: String, name : String): String {
            return File(dbDirectory + name + databaseFileSuffix).readText()
        }

        private fun <T> readAndDeserialize(dbDirectory: String, filename: String, deserializer: (String) -> T): ConcurrentSet<T> {
            return try {
                val file = readFile(dbDirectory, filename)
                file.split("\n").map { deserializer(it) }.toConcurrentSet()
            } catch (ex: FileNotFoundException) {
                logWarn { "$filename file missing, creating empty" }
                ConcurrentSet()
            }
        }


    }
}