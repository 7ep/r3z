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

        if (dbp != null) {
            do {
                val nextItem = data.modified.poll()
                if (nextItem != null) {
                    when (nextItem.second) {
                        CREATE -> dbp.persistToDisk(nextItem.first, name)
                        DELETE -> handleDeletion(nextItem.first, name, dbp, data)
                        UPDATE -> dbp.updateOnDisk(nextItem.first, name)
                    }
                }
            } while (nextItem != null)
        }
        return result
    }

    /**
     * Deletion is a special case.  If items have been deleted so that the next
     * index changes, we account for that here.
     *
     * For example, let's say we have three items, and their indexes are: 1, 2, 3
     *
     * What if we wipe out item 3?  Then we need to adjust our nextIndex counter to
     * have 3 as the next index to assign.
     *
     * What if there is only one item in the set of data, with an index of 18? If
     * we delete that, there are no items in the set and so our next index should be 1.
     *
     * Note that deleting items not at the end shouldn't have much effect.  For example,
     * if we have items 1, 2, 3, and we delete the item with index 2, then nothing changes,
     * we still have a nextIndex of 4.
     */
    private fun handleDeletion(item: T, name: String, dbp: DatabaseDiskPersistence, data: ChangeTrackingSet<T>)  {
        dbp.deleteOnDisk(item, name)
        val nextIndexValue = data.maxOfOrNull { it.getIndex() } ?: 0
        data.nextIndex.set(nextIndexValue + 1)
    }

    /**
     * carry out some readonly action on the data.
     * @param action a lambda to receive the set of data and do whatever you want with it
     */
    fun <R> read(action: (Set<T>) -> R): R {
        return action.invoke(Collections.unmodifiableSet(data))
    }

}