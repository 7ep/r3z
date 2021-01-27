package coverosR3z.persistence.utility

import coverosR3z.authentication.types.Session
import coverosR3z.authentication.types.User
import coverosR3z.config.CURRENT_DATABASE_VERSION
import coverosR3z.logging.logImperative
import coverosR3z.logging.logTrace
import coverosR3z.logging.logWarn
import coverosR3z.misc.utility.ActionQueue
import coverosR3z.persistence.exceptions.DatabaseCorruptedException
import coverosR3z.persistence.types.ChangeTrackingSet
import coverosR3z.persistence.types.IndexableSerializable
import coverosR3z.persistence.utility.PureMemoryDatabase.Companion.EMPLOYEES
import coverosR3z.persistence.utility.PureMemoryDatabase.Companion.PROJECTS
import coverosR3z.persistence.utility.PureMemoryDatabase.Companion.SESSIONS
import coverosR3z.persistence.utility.PureMemoryDatabase.Companion.TIME_ENTRIES
import coverosR3z.persistence.utility.PureMemoryDatabase.Companion.USERS
import coverosR3z.timerecording.persistence.TimeEntryPersistence
import coverosR3z.timerecording.types.Employee
import coverosR3z.timerecording.types.EmployeeName
import coverosR3z.timerecording.types.Project
import coverosR3z.timerecording.types.TimeEntry
import java.io.File

/**
 * Encapsulates the logic necessary for writing any data to disk.
 *
 * The pattern is to group it by index
 */
class DatabaseDiskPersistence(private val dbDirectory : String? = null) {

    private val actionQueue = ActionQueue("DatabaseWriter")

    /**
     * This function will stop the database persistence cleanly.
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
     * takes any serializable data and writes it to disk
     *
     * @param set the data we are serializing and writing
     * @param name the name of the data
     */
    fun <T: IndexableSerializable> persistToDisk(set : Set<T>, name: String) {
        if (dbDirectory == null) {
            logTrace { "database directory was null, skipping serialization for $name" }
            return
        }

        for (item in set) {
            val parentDirectory = "$dbDirectory$name"
            actionQueue.enqueue { File(parentDirectory).mkdirs() }

            val fullPath = "$parentDirectory/${item.getIndex()}$databaseFileSuffix"
            actionQueue.enqueue { File(fullPath).writeText(item.serialize()) }
        }
    }

    fun <T: IndexableSerializable> deleteOnDisk(set: Set<T>, subDirectory: String) {
        if (dbDirectory == null) {
            logTrace { "database directory was null, skipping delete for $subDirectory" }
            return
        }

        for (item in set) {
            val parentDirectory = "$dbDirectory$subDirectory"

            val fullPath = "$parentDirectory/${item.getIndex()}$databaseFileSuffix"
            actionQueue.enqueue { File(fullPath).delete() }
        }
    }


    companion object {
        const val databaseFileSuffix = ".db"

        private val serializedStringRegex = """ (.*?): (.*?) """.toRegex()

        /**
         * This factory method handles the nitty-gritty about starting
         * the database with respect to the files on disk.  If you plan
         * to use the database with the disk, here's a great place to
         * start.  If you are just going to use the database in memory-only,
         * check out [PureMemoryDatabase.startMemoryOnly]
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

            val projects = readAndDeserialize(dbDirectory, PROJECTS) { Project.Deserializer().deserialize(it) }
            projects.nextIndex.set(projects.maxOfOrNull { it.id.value }?.inc() ?: 1)

            val users = readAndDeserialize(dbDirectory, USERS) { User.Deserializer().deserialize(it) }
            users.nextIndex.set(users.maxOfOrNull { it.id.value }?.inc() ?: 1)

            val sessions = readAndDeserialize(dbDirectory, SESSIONS) { Session.Deserializer(users.toSet()).deserialize(it) }
            sessions.nextIndex.set(sessions.maxOfOrNull { it.simpleId }?.inc() ?: 1)

            val employees = readAndDeserialize(dbDirectory, EMPLOYEES) { Employee.Deserializer().deserialize(it) }
            employees.nextIndex.set(employees.maxOfOrNull { it.id.value }?.inc() ?: 1)

            val timeEntries = readAndDeserialize(dbDirectory, TIME_ENTRIES) { TimeEntry.Deserializer(employees, projects).deserialize(it) }
            timeEntries.nextIndex.set(timeEntries.maxOfOrNull { it.id.value }?.inc() ?: 1)

            return PureMemoryDatabase(employees, users, projects, timeEntries, sessions, dbDirectory)
        }

        private fun <T> readAndDeserialize(dbDirectory: String, filename: String, deserializer: (String) -> T): ChangeTrackingSet<T> {
            val dataDirectory = File("$dbDirectory$filename")
            val changeTrackingSet = ChangeTrackingSet<T>()

            if (! dataDirectory.exists()) {
                logWarn { "$filename directory missing, creating empty set of data" }
                return changeTrackingSet
            }

            dataDirectory
                .walkTopDown()
                .filter {it.isFile }
                .forEach {
                    logTrace { "about to read ${it.name}" }
                    val fileContents = it.readText()
                    if (fileContents.isBlank()) {
                        logWarn { "${it.name} file exists but empty, skipping" }
                    } else {
                        changeTrackingSet.add(deserializer(fileContents))
                    }
                }

            return changeTrackingSet
        }

        /**
         * Used by the classes needing serialization to avoid a bit of boilerplate
         */
        fun <T: Any> deserialize(str : String, clazz: Class<T>, convert: (Map<String, String>) -> T) : T {
            try {
                val groups = checkNotNull(serializedStringRegex.findAll(str)).flatMap { it.groupValues }.toList()
                var currentIndex = 0
                check(groups.size % 3 == 0) {"Our regular expression returns three values each time.  The whole match, then the key, then the value.  Thus a multiple of 3"}
                val map = mutableMapOf<String, String>()
                while(true) {
                    if (groups.size - currentIndex >= 3) {
                        map[groups[currentIndex+1]] = groups[currentIndex + 2]
                        currentIndex += 3
                    } else {
                        break
                    }
                }
                return convert(map)
            } catch (ex : DatabaseCorruptedException) {
                throw ex
            }catch (ex : Throwable) {
                throw DatabaseCorruptedException("Unable to deserialize this text as ${clazz.simpleName} data: $str", ex)
            }
        }
    }
}