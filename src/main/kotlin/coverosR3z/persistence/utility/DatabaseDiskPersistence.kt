package coverosR3z.persistence.utility

import coverosR3z.authentication.types.Invitation
import coverosR3z.authentication.types.Session
import coverosR3z.authentication.types.User
import coverosR3z.system.config.CURRENT_DATABASE_VERSION
import coverosR3z.system.logging.ILogger
import coverosR3z.system.logging.ILogger.Companion.logImperative
import coverosR3z.system.misc.utility.ActionQueue
import coverosR3z.system.misc.utility.decode
import coverosR3z.persistence.exceptions.DatabaseCorruptedException
import coverosR3z.persistence.types.ChangeTrackingSet
import coverosR3z.persistence.types.IndexableSerializable
import coverosR3z.persistence.types.SerializableCompanion
import coverosR3z.persistence.types.SerializationKeys
import coverosR3z.system.config.types.SystemConfiguration
import coverosR3z.timerecording.types.Employee
import coverosR3z.timerecording.types.Project
import coverosR3z.timerecording.types.SubmittedPeriod
import coverosR3z.timerecording.types.TimeEntry
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Encapsulates the logic necessary for writing any data to disk.
 *
 * The pattern is to group it by index
 */
class DatabaseDiskPersistence(
    private val dbDirectory: String? = null,
    val logger: ILogger,
    executorService: ExecutorService = Executors.newCachedThreadPool(Executors.defaultThreadFactory())
) {

    private val actionQueue = ActionQueue("DatabaseWriter", executorService)

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
        val parentDirectory = "$dbDirectoryWithVersion$name"
        actionQueue.enqueue { File(parentDirectory).mkdirs() }

        val fullPath = "$parentDirectory/${item.getIndex()}$databaseFileSuffix"

        actionQueue.enqueue {
            File(fullPath).writeText(item.serialize())
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
        val fullPath = "$dbDirectoryWithVersion$subDirectory/${item.getIndex()}$databaseFileSuffix"
        actionQueue.enqueue { File(fullPath).delete() }
    }

    fun <T: IndexableSerializable> updateOnDisk(item: T, subDirectory: String) {
        val fullPath = "$dbDirectoryWithVersion$subDirectory/${item.getIndex()}$databaseFileSuffix"
        val file = File(fullPath)

        actionQueue.enqueue {
            // if the file isn't already there, throw an exception
            check(file.exists()) { "we were asked to update $file but it doesn't exist" }
            File(fullPath).writeText(item.serialize())
        }
    }

    /**
     * This factory method handles the nitty-gritty about starting
     * the database with respect to the files on disk.  If you plan
     * to use the database with the disk, here's a great place to
     * start.
     */
    fun startWithDiskPersistence() : PureMemoryDatabase {

        dbDirectoryWithVersion = "$dbDirectory$CURRENT_DATABASE_VERSION/"

        // first we assume the database has been previously persisted
        val restoredPMD = deserializeFromDisk()

        return if (restoredPMD != null) {
            restoredPMD
        } else {
            logImperative("Building new database at $dbDirectory")
            // if nothing is there, we build a new database
            // and add a clean set of directories
            val pmd = PureMemoryDatabase.createEmptyDatabase(diskPersistence = this)
            logImperative("Created new PureMemoryDatabase")

            File(checkNotNull(dbDirectoryWithVersion)).mkdirs()
            logImperative("Created the database directory at \"$dbDirectoryWithVersion\"")

            val versionFilename = "currentVersion.txt"
            File(dbDirectory + versionFilename).writeText(CURRENT_DATABASE_VERSION.toString())
            logImperative("Wrote the version of the database ($CURRENT_DATABASE_VERSION) to $versionFilename")

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
            logImperative("directory $dbDirectoryWithVersion was not found")
            return null
        }

        val projects = readAndDeserialize(Project.directoryName) { Project.Deserializer().deserialize(it) }
        val employees = readAndDeserialize(Employee.directoryName) { Employee.Deserializer().deserialize(it) }
        val users = readAndDeserialize(User.directoryName) { User.Deserializer(employees).deserialize(it) }
        val sessions = readAndDeserialize(Session.directoryName) { Session.Deserializer(users).deserialize(it) }
        val timeEntries = readAndDeserialize(TimeEntry.directoryName) { TimeEntry.Deserializer(employees, projects).deserialize(it) }
        val submittedPeriods = readAndDeserialize(SubmittedPeriod.directoryName) { SubmittedPeriod.Deserializer(employees).deserialize(it) }
        val invitations = readAndDeserialize(Invitation.directoryName) { Invitation.Deserializer(employees).deserialize(it) }
        val systemconfig = readAndDeserialize(SystemConfiguration.directoryName) { SystemConfiguration.Deserializer().deserialize(it) }

        return PureMemoryDatabase(
            this,
            data = mapOf(
                SubmittedPeriod.directoryName to submittedPeriods,
                TimeEntry.directoryName to timeEntries,
                Employee.directoryName to employees,
                Session.directoryName to sessions,
                Project.directoryName to projects,
                User.directoryName to users,
                Invitation.directoryName to invitations,
                SystemConfiguration.directoryName to systemconfig,
            ),
        )
    }

    private fun <T : IndexableSerializable> readAndDeserialize(dataName: String, deserializer: (String) -> T): ChangeTrackingSet<T> {
        val dataDirectory = File("$dbDirectoryWithVersion$dataName")

        if (! dataDirectory.exists()) {
            logger.logWarn { "$dataName directory missing, creating empty set of data" }
            return ChangeTrackingSet()
        }

        val data = ChangeTrackingSet<T>()
        dataDirectory
            .walkTopDown()
            .filter {it.isFile}
            .forEach {
                val fileContents = it.readText()
                if (fileContents.isBlank()) {
                    logger.logWarn { "${it.name} file exists but empty, skipping" }
                } else {
                    data.addWithoutTracking(deserializer(fileContents))
                }
            }

        if (data.isEmpty()) data.nextIndex.set(1) else data.nextIndex.set(data.maxOf { it.getIndex() } + 1)
        return data
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as DatabaseDiskPersistence

        if (dbDirectory != other.dbDirectory) return false
        if (dbDirectoryWithVersion != other.dbDirectoryWithVersion) return false

        return true
    }

    override fun hashCode(): Int {
        var result = dbDirectory?.hashCode() ?: 0
        result = 31 * result + (dbDirectoryWithVersion?.hashCode() ?: 0)
        return result
    }


    companion object {
        const val databaseFileSuffix = ".db"

        private val serializedStringRegex = """ (.*?): (.*?) """.toRegex()

        /**
         * Used by the classes needing serialization to avoid a bit of boilerplate
         * @param T the type of thing we want to return, like a project, employee, favorite color, whatevs
         */
        fun <T: Any> dbentryDeserialize(
            str: String,
            instance: SerializableCompanion<*>,
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