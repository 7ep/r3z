package coverosR3z.persistence.utility

import coverosR3z.logging.logTrace
import coverosR3z.misc.utility.ActionQueue
import coverosR3z.persistence.types.IndexableSerializable
import coverosR3z.timerecording.types.Project
import java.io.File

/**
 * Encapsulates the logic necessary for writing any data to disk.
 *
 * The pattern is to group it by index
 */
class DatabaseSerialization(private val dbDirectory : String? = null) {

    private val actionQueue = ActionQueue("DatabaseWriter")



    /**
     * takes any serializable data and writes it to disk
     *
     * @param set the data we are serializing and writing
     * @param name the name of the data
     */
    fun <T: IndexableSerializable> serializeToDisk(set : Set<T>, name: String) {
        if (dbDirectory == null) {
            logTrace { "database directory was null, skipping serialization for $name" }
            return
        }

        for (item in set) {
            val parentDirectory = "$dbDirectory$name"
            File(parentDirectory).mkdirs()

            val fullPath = "$parentDirectory/${item.getIndex()}$databaseFileSuffix"
            File(fullPath)
                .writeText(item.serialize())
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
            File(fullPath).delete()
        }
    }

    companion object {
        const val databaseFileSuffix = ".db"
    }
}