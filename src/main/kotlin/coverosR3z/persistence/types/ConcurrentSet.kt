package coverosR3z.persistence.types

import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * This uses a [ConcurrentHashMap] as its base.  We store
 * the data in the keys only.  We provide some syntactic sugar
 * so this seems similar to using a Set.
 */
open class ConcurrentSet<T> : Iterable<T>, Set<T>, MutableCollection<T> {

    private val map : ConcurrentHashMap<T, NullEnum> = ConcurrentHashMap()

    val nextIndex = AtomicInteger(1)

    private enum class NullEnum {
        /**
         * This is just a token for the value in the ConcurrentHashMap, since
         * we are only using the keys, never the values.
         */
        NULL
    }

    override fun add(element : T) : Boolean {
        map[element] = NullEnum.NULL
        return true
    }

    /**
     * Adds all of the elements of the specified collection to this collection.
     *
     * @return `true` if any of the specified elements was added to the collection, `false` if the collection was not modified.
     */
    override fun addAll(elements: Collection<T>): Boolean {
        var didAdd = false
        for (element in elements) {
            map[element] = NullEnum.NULL
            didAdd = true
        }
        return didAdd
    }

    override fun remove(element: T) : Boolean {
        val itemToRemove = checkNotNull(element)
        if (map[itemToRemove] == null) return false

        map.remove(itemToRemove)
        return true
    }

    /**
     * We will provide an iterator, but only for
     * read-only actions - no writing allowed
     *
     * This constricts the interface, to simplify
     * things so it is easier to comprehend where
     * data is allowed to be changed.
     */
    override fun iterator(): MutableIterator<T> {
        return Collections.unmodifiableSet(map.keySet(NullEnum.NULL)).iterator()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ConcurrentSet<*>

        if (map != other.map) return false
        if (nextIndex.get() != other.nextIndex.get()) return false

        return true
    }

    override fun hashCode(): Int {
        var result = map.hashCode()
        result = 31 * result + nextIndex.get().hashCode()
        return result
    }

    override val size: Int
        get() = map.size

    override fun contains(element: T): Boolean {
        return map.keys.contains(element)
    }

    override fun containsAll(elements: Collection<T>): Boolean {
        return map.keys.containsAll(elements)
    }

    override fun isEmpty(): Boolean {
        return map.isEmpty()
    }

    override fun clear() {
        this.map.clear()
    }

    override fun removeAll(elements: Collection<T>): Boolean {
        // whether the set was modified
        var wasModified = false
        for (e in elements) {
            checkNotNull(e)
            if (map[e] != null) {
                map.remove(e)
                wasModified = true
            }
        }
        return wasModified
    }

    override fun retainAll(elements: Collection<T>): Boolean {
        throw NotImplementedError("we have no current use for this, so leaving unimplemented")
    }

}

