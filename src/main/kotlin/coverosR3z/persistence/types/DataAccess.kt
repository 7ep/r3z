package coverosR3z.persistence.types

import coverosR3z.persistence.types.ChangeTrackingSet.DataAction.*
import coverosR3z.persistence.utility.DatabaseDiskPersistence
import java.util.*

/**
 * Represents the common methods available on data.
 *
 * For read-only situations (no changes being made to the data),
 * you can use [read]
 *
 * For any situation where the data changes, see [actOn]
 *
 * @param T is the domain-oriented type, such as Project or Employee.
 *        The database expects all data to be a set of [ChangeTrackingSet]
 */
class DataAccess<T: IndexableSerializable> (
    private val data : ChangeTrackingSet<T>,
    private val dbp : DatabaseDiskPersistence? = null,
    private val name: String)
   {

    /**
     * carry out some write action on the data.
     *
     * This has to be synchronized because there's no other atomic way to
     * make changes to both the database *and* the disk.
     *
     * @param action a lambda to receive the set of data and do whatever you want with it
     */
    @Synchronized
    fun <R> actOn(action: (ChangeTrackingSet<T>) -> R): R {
        val result = action.invoke(data)

        // if the data set is now empty, reset the nextIndex to 1
        if (data.isEmpty()) {
            data.nextIndex.set(1)
        }

        if (dbp != null) {
            do {
                val nextItem = data.modified.poll()
                if (nextItem != null) {
                    when (nextItem.second) {
                        CREATE -> dbp.persistToDisk(nextItem.first, name)
                        DELETE -> dbp.deleteOnDisk(nextItem.first, name)
                        UPDATE -> dbp.updateOnDisk(nextItem.first, name)
                    }
                }
            } while (nextItem != null)
        }
        return result
    }

    /**
     * carry out some readonly action on the data.
     * @param action a lambda to receive the set of data and do whatever you want with it
     */
    fun <R> read(action: (Set<T>) -> R): R {
        return action.invoke(Collections.unmodifiableSet(data))
    }

}