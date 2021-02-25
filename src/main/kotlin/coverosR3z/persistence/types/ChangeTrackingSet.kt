package coverosR3z.persistence.types

import coverosR3z.persistence.types.ChangeTrackingSet.DataAction.*

/**
 * Similar to [MutableConcurrentSet] except that it tracks any changes
 * made to the data.  Anything that uses the [add] command will
 * have an item added to [modified] with a tag of [CREATE], and
 * anything that uses [remove] will do similarly with a tag of [DELETE]
 */
class ChangeTrackingSet<T: IndexableSerializable> : MutableConcurrentSet<T>() {

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
        DELETE,

        /**
         * Update the data in place
         */
        UPDATE,
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

    override fun remove(element: T) : Boolean {
        modified.add(Pair(element, DELETE))
        return super.remove(element)
    }

    /**
     * Updates a value
     *
     * We will find the old element by its id, since this must
     * be of type [IndexableSerializable], it means we have
     * access to the getIndex() command.  Then we will
     * overwrite the value stored.
     *
     * There is some nuance to this method.
     *
     * In particular, this will put a DELETE followed
     * by an ADD into the modified queue, for the files
     * to be changed on disk.
     *
     */
    override fun update(element: T) : Boolean {
        modified.add(Pair(element, UPDATE))
        return super.update(element)
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

fun <T: IndexableSerializable> List<T>.toChangeTrackingSet() : ChangeTrackingSet<T> {
    val newSet = ChangeTrackingSet<T>()
    this.forEach{newSet.add(it)}
    return newSet
}
