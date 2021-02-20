package coverosR3z.persistence.utility

import coverosR3z.authentication.types.Session
import coverosR3z.authentication.types.User
import coverosR3z.config.CURRENT_DATABASE_VERSION
import coverosR3z.logging.ILogger
import coverosR3z.logging.ILogger.Companion.logImperative
import coverosR3z.misc.utility.ActionQueue
import coverosR3z.misc.utility.checkParseToInt
import coverosR3z.misc.utility.decode
import coverosR3z.persistence.exceptions.DatabaseCorruptedException
import coverosR3z.persistence.types.*
import coverosR3z.timerecording.persistence.TimeEntryPersistence
import coverosR3z.timerecording.types.*
import java.io.File

/**
 * Encapsulates the logic necessary for writing any data to disk.
 *
 * The pattern is to group it by index
 */
class DatabaseDiskPersistence(private val dbDirectory : String? = null, val logger: ILogger) {

    private val actionQueue = ActionQueue("DatabaseWriter")

    /**
     * Includes the version
     */
    private var dbDirectoryWithVersion : String? = null

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
     * @param item the data we are serializing and writing
     * @param name the name of the data
     */
    fun <T: IndexableSerializable> persistToDisk(item : T, name: String) {
        if (dbDirectoryWithVersion == null) {
            logger.logTrace { "database directory was null, skipping serialization for $name" }
            return
        }

        val parentDirectory = "$dbDirectoryWithVersion$name"
        actionQueue.enqueue { File(parentDirectory).mkdirs() }

        val fullPath = "$parentDirectory/${item.getIndex()}$databaseFileSuffix"
        // we save the next index each time.  This way we can be more consistent
        // since we allow deleting data, even to the point that all data can be deleted.
        val nextIndexPath = "$parentDirectory/$nextIndexFileName$databaseFileSuffix"
        actionQueue.enqueue {
            File(fullPath).writeText(item.serialize())
            File(nextIndexPath).writeText((item.getIndex() + 1).toString())
        }
    }

    /**
     * Deletes a piece of data from the disk
     *
     * Our data consists of directories as containers and each
     * individual piece of data (e.g. [TimeEntry], [Project], etc.) as
     * a file in that directory.  This method simply finds the proper
     * file and deletes it.
     *
     * @param item the data we are serializing and writing
     * @param subDirectory the name of the data, for finding the directory
     */
    fun <T: IndexableSerializable> deleteOnDisk(item: T, subDirectory: String) {
        if (dbDirectoryWithVersion == null) {
            logger.logTrace { "database directory was null, skipping delete for $subDirectory" }
            return
        }

        val parentDirectory = "$dbDirectoryWithVersion$subDirectory"

        val fullPath = "$parentDirectory/${item.getIndex()}$databaseFileSuffix"
        actionQueue.enqueue { File(fullPath).delete() }
    }

    /**
     * This factory method handles the nitty-gritty about starting
     * the database with respect to the files on disk.  If you plan
     * to use the database with the disk, here's a great place to
     * start.  If you are just going to use the database in memory-only,
     * check out [PureMemoryDatabase.startMemoryOnly]
     */
    fun startWithDiskPersistence() : PureMemoryDatabase {

        dbDirectoryWithVersion = "$dbDirectory$CURRENT_DATABASE_VERSION/"

        // first we assume the database has been previously persisted
        val restoredPMD = deserializeFromDisk()

        return if (restoredPMD != null) {
            restoredPMD
        } else {
            logImperative("No existing database found, building new database")
            // if nothing is there, we build a new database
            // and add a clean set of directories
            val pmd = PureMemoryDatabase(dbDirectory = dbDirectoryWithVersion, diskPersistence = this)
            logImperative("Created new PureMemoryDatabase")

            File(checkNotNull(dbDirectoryWithVersion)).mkdirs()
            logImperative("Created the database directory at \"$dbDirectoryWithVersion\"")

            val versionFilename = "currentVersion.txt"
            File(dbDirectory + versionFilename).writeText(CURRENT_DATABASE_VERSION.toString())
            logImperative("Wrote the version of the database ($CURRENT_DATABASE_VERSION) to $versionFilename")


            val tep = TimeEntryPersistence(pmd, logger = logger)
            tep.persistNewEmployee(EmployeeName("Administrator"))
            logImperative("Created an initial employee")

            pmd
        }
    }


    /**
     * Deserializes the database from files, or returns null if no
     * database directory exists
     */
    private fun deserializeFromDisk(): PureMemoryDatabase? {
        val topDirectory = File(checkNotNull(dbDirectoryWithVersion))
        val innerFiles = topDirectory.listFiles()
        if ((!topDirectory.exists()) || innerFiles.isNullOrEmpty()) {
            logImperative("directory $dbDirectoryWithVersion did not exist.  Returning null for the PureMemoryDatabase")
            return null
        }

        val projects = readAndDeserialize(Project.directoryName) { Project.Deserializer().deserialize(it) }
        val users = readAndDeserialize(User.directoryName) { User.Deserializer().deserialize(it) }
        val sessions = readAndDeserialize(Session.directoryName) { Session.Deserializer(users).deserialize(it) }
        val employees = readAndDeserialize(Employee.directoryName) { Employee.Deserializer().deserialize(it) }
        val timeEntries = readAndDeserialize(TimeEntry.directoryName) { TimeEntry.Deserializer(employees, projects).deserialize(it) }
        val submittedPeriods = readAndDeserialize(SubmittedPeriod.directoryName) { SubmittedPeriod.Deserializer(employees).deserialize(it) }

        return PureMemoryDatabase(employees, users, projects, timeEntries, sessions, submittedPeriods, dbDirectoryWithVersion, this)
    }

    private fun <T : Indexed> readAndDeserialize(dataName: String, deserializer: (String) -> T): ChangeTrackingSet<T> {
        val dataDirectory = File("$dbDirectoryWithVersion$dataName")

        if (! dataDirectory.exists()) {
            logger.logWarn { "$dataName directory missing, creating empty set of data" }
            return ChangeTrackingSet()
        }

        val data = ChangeTrackingSet<T>()
        dataDirectory
            .walkTopDown()
            .filter {it.isFile && it.nameWithoutExtension != nextIndexFileName}
            .forEach {
                val fileContents = it.readText()
                if (fileContents.isBlank()) {
                    logger.logWarn { "${it.name} file exists but empty, skipping" }
                } else {
                    data.addWithoutTracking(deserializer(fileContents))
                }
            }


        val nextIndexFile = File("$dbDirectoryWithVersion$dataName/$nextIndexFileName$databaseFileSuffix")
        val nextIndex = if (! nextIndexFile.exists()) 1 else checkParseToInt(nextIndexFile.readText())
        data.nextIndex.set(nextIndex)
        return data
    }

    companion object {
        const val databaseFileSuffix = ".db"
        const val nextIndexFileName = "nextindex"

        private val serializedStringRegex = """ (.*?): (.*?) """.toRegex()

        /**
         * Used by the classes needing serialization to avoid a bit of boilerplate
         * @param T the type of thing we want to return, like a project, employee, favorite color, whatevs
         * @param E the type of an enum - such a pain
         * @param K a particular enum's whole type, we use this to restrict our possible keys for serialization
         *        to being from a set of enums.  See [Employee.keys] for example.
         * @param C a [SerializableCompanion], which is an abstract class that requires
         *        instantiating with a set of enums of type [K]
         */
        fun <T: Any, E : Any, K : Enum<E>,  C: SerializableCompanion<K>> deserialize(
            str: String,
            instance: C,
            convert: (Map<SerializationKeys, String>) -> T
        ) : T {
            try {
                val groups = checkNotNull(serializedStringRegex.findAll(str)).flatMap { it.groupValues }.toList()
                var currentIndex = 0
                check(groups.size % 3 == 0) {"Our regular expression returns three values each time.  The whole match, then the key, then the value.  Thus a multiple of 3"}
                val map = mutableMapOf<SerializationKeys, String>()
                while(true) {
                    if (groups.size - currentIndex >= 3) {
                        val keyString = instance.convertToKey(groups[currentIndex + 1])
                        map[keyString] = decode(groups[currentIndex + 2])
                        currentIndex += 3
                    } else {
                        break
                    }
                }
                return convert(map)
            } catch (ex : DatabaseCorruptedException) {
                throw ex
            }catch (ex : Throwable) {
                throw DatabaseCorruptedException("Unable to deserialize this text from the ${instance.directoryName} directory: $str", ex)
            }
        }


    }
}