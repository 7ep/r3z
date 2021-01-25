package coverosR3z.persistence.types

import coverosR3z.persistence.types.ChangeTrackingSet.DataAction.CREATE
import coverosR3z.persistence.types.ChangeTrackingSet.DataAction.DELETE
import coverosR3z.persistence.utility.DatabaseDiskPersistence
import java.util.*

abstract class AbstractDataAccess<T> (
    private val data : ChangeTrackingSet<T>,
    private val dbp : DatabaseDiskPersistence,
    private val name: String)
    : DataAccess<T>
        where T : IndexableSerializable {

    override fun <R> actOn(action: (ChangeTrackingSet<T>) -> R): R {
        val result = action.invoke(data)

        data.getChangedData().forEach {
            when (it.second) {
                CREATE -> dbp.persistToDisk(setOf(it.first), name)
                DELETE -> dbp.deleteOnDisk(setOf(it.first), name)
            }
        }

        return result
    }

    override fun <R> read(action: (Set<T>) -> R): R {
        return action.invoke(Collections.unmodifiableSet(data))
    }

}