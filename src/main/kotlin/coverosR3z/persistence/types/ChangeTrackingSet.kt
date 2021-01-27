package coverosR3z.persistence.types

import coverosR3z.persistence.types.ChangeTrackingSet.DataAction.CREATE
import coverosR3z.persistence.types.ChangeTrackingSet.DataAction.DELETE

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

    private val modified = mutableSetOf<Pair<T, DataAction>>()

    /**
     * Gets the current changes to the data, clearing
     * it in the process
     */
    fun getChangedData(): Set<Pair<T, DataAction>> {
        val mySet = modified.toSet()
        modified.clear()
        return mySet
    }

    /**
     * clears the set of tracked changed data
     */
    fun clearModifications() {
        modified.clear()
    }

    override fun add(item : T) : T {
        modified.add(Pair(item, CREATE))
        return super.add(item)
    }

    override fun addAll(elements: Collection<T>) : Boolean {
        modified.addAll(elements.map { Pair(it, CREATE) }.toSet() as Collection<Pair<T, DataAction>>)
        return super.addAll(elements)
    }

    override fun remove(item: T) {
        modified.add(Pair(item, DELETE))
        return super.remove(item)
    }


}

fun <T> List<T>.toChangeTrackingSet() : ChangeTrackingSet<T> {
    val newSet = ChangeTrackingSet<T>()
    this.forEach{newSet.add(it)}
    return newSet
}
