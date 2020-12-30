package coverosR3z.persistence

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * This uses a [ConcurrentHashMap] as its base.  We store
 * the data in the keys only.  We provide some syntactic sugar
 * so this seems similar to using a Set.
 */
class ConcurrentSet<T> : Iterable<T>{

    private val map : ConcurrentHashMap<T, NullEnum> = ConcurrentHashMap()

    fun size() = map.size

    val nextIndex = AtomicInteger(size()+1)

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
    fun add(item : T) {
        map[item] = NullEnum.NULL
    }

    /**
     * Adds all of the elements of the specified collection to this collection.
     *
     * @return `true` if any of the specified elements was added to the collection, `false` if the collection was not modified.
     */
    fun addAll(elements: Collection<T>): Boolean {
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
    fun remove(item: T) {
        map.remove(checkNotNull(item))
    }

    override fun iterator(): Iterator<T> {
        return map.keySet(NullEnum.NULL).iterator()
    }

    companion object {

        fun <T> concurrentSetOf(vararg elements: T): ConcurrentSet<T> = elements.toList().toConcurrentSet()

    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ConcurrentSet<*>

        if (map != other.map) return false

        return true
    }

    override fun hashCode(): Int {
        return map.hashCode()
    }

}

fun <T> List<T>.toConcurrentSet() : ConcurrentSet<T> {
    val newSet = ConcurrentSet<T>()
    this.forEach{newSet.add(it)}
    return newSet
}