package coverosR3z.persistence.types

import coverosR3z.persistence.types.ChangeTrackingSet.DataAction.CREATE
import coverosR3z.persistence.types.ChangeTrackingSet.DataAction.DELETE
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Similar to [ConcurrentSet] except that it tracks any changes
 * made to the data.  Anything that uses the [add] command will
 * have an item added to [modified] with a tag of [CREATE], and
 * anything that uses [remove] will do similarly with a tag of [DELETE]
 */
class ChangeTrackingSet<T> : ConcurrentSet<T>() {

    /**
     * This is used to tag what gets changed, so we
     * know what to do during serialization later.
     * For example, if something was deleted, we
     * would delete the file.
     */
    enum class DataAction {
        /**
         * New data is being added
         */
        CREATE,

        /**
         * Data is being deleted from the set
         */
        DELETE
    }

    val modified = R3zConcurrentQueue<Pair<T, DataAction>>()


    /**
     * clears the set of tracked changed data
     */
    fun clearModifications() {
        modified.clear()
    }

    override fun add(element : T) : Boolean {
        modified.add(Pair(element, CREATE))
        return super.add(element)
    }

    /**
     * Unlike [add], this will not put anything into the
     * list of modified data.  This is necessary in
     * some situations, like when deserializing data from disk
     * during system startup.
     */
    fun addWithoutTracking(item : T) : Boolean {
        return super.add(item)
    }

    override fun addAll(elements: Collection<T>) : Boolean {
        modified.addAll(elements.map { Pair(it, CREATE) }.toSet() as Collection<Pair<T, DataAction>>)
        return super.addAll(elements)
    }

    override fun remove(element: T) : Boolean {
        modified.add(Pair(element, DELETE))
        return super.remove(element)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        if (!super.equals(other)) return false

        other as ChangeTrackingSet<*>

        if (modified != other.modified) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + modified.hashCode()
        return result
    }


}

fun <T> List<T>.toChangeTrackingSet() : ChangeTrackingSet<T> {
    val newSet = ChangeTrackingSet<T>()
    this.forEach{newSet.add(it)}
    return newSet
}
