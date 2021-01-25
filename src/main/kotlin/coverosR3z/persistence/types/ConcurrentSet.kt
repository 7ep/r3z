package coverosR3z.persistence.types

import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * This uses a [ConcurrentHashMap] as its base.  We store
 * the data in the keys only.  We provide some syntactic sugar
 * so this seems similar to using a Set.
 */
open class ConcurrentSet<T> : Iterable<T>, Set<T> {

    private val map : ConcurrentHashMap<T, NullEnum> = ConcurrentHashMap()

    val nextIndex = AtomicInteger(1)

    private enum class NullEnum {
        /**
         * This is just a token for the value in the ConcurrentHashMap, since
         * we are only using the keys, never the values.
         */
        NULL
    }

    /**
     * Adds an item to this collection
     */
    open fun add(item : T) : T{
        map[item] = NullEnum.NULL
        return item
    }

    /**
     * Adds all of the elements of the specified collection to this collection.
     *
     * @return `true` if any of the specified elements was added to the collection, `false` if the collection was not modified.
     */
    open fun addAll(elements: Collection<T>): Boolean {
        var didAdd = false
        for (element in elements) {
            map[element] = NullEnum.NULL
            didAdd = true
        }
        return didAdd
    }

    /**
     * Removes an item from this collection
     */
    open fun remove(item: T) {
        map.remove(checkNotNull(item))
    }

    /**
     * We will provide an iterator, but only for
     * read-only actions - no writing allowed
     *
     * This constricts the interface, to simplify
     * things so it is easier to comprehend where
     * data is allowed to be changed.
     */
    override fun iterator(): Iterator<T> {
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

}

