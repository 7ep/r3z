package coverosR3z.persistence.types

import coverosR3z.persistence.types.ChangeTrackingSet.DataAction.CREATE
import coverosR3z.persistence.types.ChangeTrackingSet.DataAction.DELETE
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
abstract class AbstractDataAccess<T> (
    private val data : ChangeTrackingSet<T>,
    private val dbp : DatabaseDiskPersistence? = null,
    private val name: String)
    : DataAccess<T>
        where T : IndexableSerializable {

    override fun <R> actOn(action: (ChangeTrackingSet<T>) -> R): R {
        val result = action.invoke(data)

        do {
            val nextItem = data.modified.poll()
            if (nextItem != null) {
                when (nextItem.second) {
                    CREATE -> dbp?.persistToDisk(nextItem.first, name)
                    DELETE -> dbp?.deleteOnDisk(nextItem.first, name)
                }
            }
        }
        while (nextItem != null)

        return result
    }

    override fun <R> read(action: (Set<T>) -> R): R {
        return action.invoke(Collections.unmodifiableSet(data))
    }

}