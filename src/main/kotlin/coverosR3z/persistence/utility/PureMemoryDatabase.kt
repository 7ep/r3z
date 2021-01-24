package coverosR3z.persistence.utility

import coverosR3z.authentication.types.Session
import coverosR3z.authentication.types.User
import coverosR3z.config.CURRENT_DATABASE_VERSION
import coverosR3z.logging.logImperative
import coverosR3z.logging.logTrace
import coverosR3z.logging.logWarn
import coverosR3z.misc.utility.ActionQueue
import coverosR3z.persistence.exceptions.DatabaseCorruptedException
import coverosR3z.persistence.exceptions.NoTimeEntriesOnDiskException
import coverosR3z.persistence.types.*
import coverosR3z.timerecording.persistence.TimeEntryPersistence
import coverosR3z.timerecording.types.*
import java.io.File
import java.io.FileNotFoundException
import java.util.*
import javax.xml.crypto.Data
import kotlin.NoSuchElementException


/**
 * Why use those heavy-handed database applications when you
 * can simply store your data in simple collections?
 *
 * Here, things are simple.  Anything you need, you make.
 *
 * @param dbDirectory if this is null, the database won't use the disk at all.  If you set it to a directory, like
 *                      File("db/") the database will use that directory for all persistence.
 */
open class PureMemoryDatabase(protected val employees: ChangeTrackingSet<Employee> = ChangeTrackingSet(),
                         protected val users: ChangeTrackingSet<User> = ChangeTrackingSet(),
                         protected val projects: ChangeTrackingSet<Project> = ChangeTrackingSet(),
                         protected val timeEntries: ChangeTrackingSet<TimeEntry> = ChangeTrackingSet(),
                         protected val sessions: ChangeTrackingSet<Session> = ChangeTrackingSet(),
                         protected val dbDirectory : String? = null
) {

    private val actionQueue = ActionQueue("DatabaseWriter")

    fun copy(): PureMemoryDatabase {
        return PureMemoryDatabase(
            employees = this.employees.toList().toChangeTrackingSet(),
            users = this.users.toList().toChangeTrackingSet(),
            projects = this.projects.toList().toChangeTrackingSet(),
            timeEntries = this.timeEntries.toList().toChangeTrackingSet(),
            sessions = this.sessions.toList().toChangeTrackingSet(),
        )
    }

    ////////////////////////////////////
    //   DATA ACCESS
    ////////////////////////////////////

    inner class EmployeeDataAccess : DataAccess<Employee> {

        override fun <R> actOn(action: (ChangeTrackingSet<Employee>) -> R): R {
            val result = action.invoke(employees)

            serializeToDisk(employees, EMPLOYEES_FILENAME)

            return result
        }

        override fun <R> read(action: (Set<Employee>) -> R): R {
            return action.invoke(Collections.unmodifiableSet(employees))
        }

    }

    inner class UserDataAccess : DataAccess<User> {

        override fun <R> actOn(action: (ChangeTrackingSet<User>) -> R): R {
            val result = action.invoke(users)

            serializeToDisk(users, USERS_FILENAME)

            return result
        }

        override fun <R> read(action: (Set<User>) -> R): R {
            return action.invoke(Collections.unmodifiableSet(users))
        }

    }

    inner class SessionDataAccess : DataAccess<Session> {

        override fun <R> actOn(action: (ChangeTrackingSet<Session>) -> R) : R {
            val result = action.invoke(sessions)

            serializeToDisk(sessions, SESSIONS_FILENAME)

            return result
        }

        override fun <R> read(action: (Set<Session>) -> R) : R {
            return action.invoke(Collections.unmodifiableSet(sessions))
        }

    }

    inner class ProjectDataAccess : DataAccess<Project> {

        override fun <R> actOn(action: (ChangeTrackingSet<Project>) -> R): R {
            val result = action.invoke(projects)

            serializeToDisk(projects, PROJECTS_FILENAME)

            return result
        }

        override fun <R> read(action: (Set<Project>) -> R): R {
            return action.invoke(Collections.unmodifiableSet(projects))
        }

    }

    inner class TimeEntryDataAccess : DataAccess<TimeEntry> {
        override fun <R> actOn(action: (ChangeTrackingSet<TimeEntry>) -> R): R {
            val result = action.invoke(timeEntries)

            val changedTimeEntries = timeEntries.getChangedData()
            serializeTimeEntriesToDisk(changedTimeEntries)

            return result
        }

        override fun <R> read(action: (Set<TimeEntry>) -> R): R {
            return action(Collections.unmodifiableSet(timeEntries))
        }

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

    protected fun <T: Serializable> serializeToDisk(set : Set<T>, filename: String) {
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
    private fun serializeTimeEntriesToDisk(modifiedEntries: Set<TimeEntry>) {
        if (dbDirectory == null) {
            logTrace { "database directory was null, skipping serialization for time entries" }
            return
        }

        for (entry in modifiedEntries) {
            // get all the time-entries for the employee and the month(s) of the modified time entry
            val employeeDateTimeEntries =
                timeEntries.filter { it.employee == entry.employee && it.date.month() == entry.date.month() }
            val timeentriesSerialized = employeeDateTimeEntries.joinToString("\n") { it.serialize() }
            val subDirectory = dbDirectory + "$TIMEENTRIES_DIRECTORY/" + "${entry.employee.id.value}/"
            val filename = "${entry.date.year()}_${entry.date.month()}"
            actionQueue.enqueue { File(subDirectory).mkdirs() }
            writeDbFile(timeentriesSerialized, filename, subDirectory)
        }
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
        private val serializedStringRegex = """ .*?: (.*?) """.toRegex()
        const val EMPLOYEES_FILENAME = "employees"
        const val SESSIONS_FILENAME = "sessions"
        const val PROJECTS_FILENAME = "projects"
        const val USERS_FILENAME = "users"
        const val TIMEENTRIES_DIRECTORY = "timeentries"


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

            val fullDbDirectory = "$dbDirectory$CURRENT_DATABASE_VERSION/"

            // first we assume the database has been previously persisted
            val restoredPMD = deserializeFromDisk(fullDbDirectory)

            return if (restoredPMD != null) {
                // return the restored database
                restoredPMD
            } else {
                logImperative("No existing database found, building new database")
                // if nothing is there, we build a new database
                // and add a clean set of directories
                val pmd = PureMemoryDatabase(dbDirectory = fullDbDirectory)
                logImperative("Created new PureMemoryDatabase")

                File(fullDbDirectory).mkdirs()
                logImperative("Created the database directory at \"$fullDbDirectory\"")

                val versionFilename = "currentVersion.txt"
                File(dbDirectory + versionFilename).writeText(CURRENT_DATABASE_VERSION.toString())
                logImperative("Wrote the version of the database ($CURRENT_DATABASE_VERSION) to $versionFilename")


                val tep = TimeEntryPersistence(pmd)
                tep.persistNewEmployee(EmployeeName("Administrator"))
                logImperative("Created an initial employee")

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
        private fun deserializeFromDisk(dbDirectory: String): PureMemoryDatabase? {
            val topDirectory = File(dbDirectory)
            val innerFiles = topDirectory.listFiles()
            if ((!topDirectory.exists()) || innerFiles.isNullOrEmpty()) {
                logImperative("directory $dbDirectory did not exist.  Returning null for the PureMemoryDatabase")
                return null
            }

            val projects = readAndDeserializeNew(dbDirectory, PROJECTS_FILENAME) { Project.deserialize(it) }
            projects.nextIndex.set(projects.maxOfOrNull { it.id.value }?.inc() ?: 1)

            val users = readAndDeserializeNew(dbDirectory, USERS_FILENAME) { User.deserialize(it) }
            users.nextIndex.set(users.maxOfOrNull { it.id.value }?.inc() ?: 1)

            val sessions = readAndDeserializeNew(dbDirectory, SESSIONS_FILENAME) { Session.deserialize(it, users.toSet()) }
            val employees = readAndDeserializeNew(dbDirectory, EMPLOYEES_FILENAME) { Employee.deserialize(it) }
            employees.nextIndex.set(employees.maxOfOrNull { it.id.value }?.inc() ?: 1)

            val fullTimeEntries = readAndDeserializeTimeEntries(dbDirectory, employees.toSet(), projects.toSet())
            fullTimeEntries.nextIndex.set(fullTimeEntries.maxOfOrNull { it.id.value }?.inc() ?: 1)

            return PureMemoryDatabase(employees, users, projects, fullTimeEntries, sessions, dbDirectory)
        }

        private fun readAndDeserializeTimeEntries(
            dbDirectory: String,
            employees: Set<Employee>,
            projects: Set<Project>) : ChangeTrackingSet<TimeEntry> {
            val timeEntriesDirectory = "$TIMEENTRIES_DIRECTORY/"
            return try {
                val fullTimeEntries: ChangeTrackingSet<TimeEntry> = ChangeTrackingSet()

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
                ChangeTrackingSet()
            }
        }

        private fun readFile(dbDirectory: String, name : String): String {
            return File(dbDirectory + name + databaseFileSuffix).readText()
        }

        private fun <T> readAndDeserialize(dbDirectory: String, filename: String, deserializer: (String) -> T): ConcurrentSet<T> {
            return try {
                val file = readFile(dbDirectory, filename)
                if (file.isBlank()) {
                    logWarn { "$filename file exists but empty, creating empty data set" }
                    return ConcurrentSet()
                }
                file.split("\n").map { deserializer(it) }.toConcurrentSet()
            } catch (ex: FileNotFoundException) {
                logWarn { "$filename file missing, creating empty" }
                ConcurrentSet()
            }
        }

        private fun <T> readAndDeserializeNew(dbDirectory: String, filename: String, deserializer: (String) -> T): ChangeTrackingSet<T> {
            return try {
                val file = readFile(dbDirectory, filename)
                if (file.isBlank()) {
                    logWarn { "$filename file exists but empty, creating empty data set" }
                    return ChangeTrackingSet()
                }
                file.split("\n").map { deserializer(it) }.toChangeTrackingSet()
            } catch (ex: FileNotFoundException) {
                logWarn { "$filename file missing, creating empty" }
                ChangeTrackingSet()
            }
        }


    }
}