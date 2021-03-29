package coverosR3z.persistence.types

import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * This uses a [ConcurrentHashMap] as its base.  We store
 * the data in the keys only.  We provide some syntactic sugar
 * so this seems similar to using a Set.
 */
open class MutableConcurrentSet<T: IndexableSerializable> : Iterable<T>, MutableSet<T>{
    private val map : ConcurrentHashMap<Int, T> = ConcurrentHashMap()

    val nextIndex = AtomicInteger(1)

    override fun add(element : T) : Boolean {
        map.computeIfAbsent(element.getIndex()) { element }
        return true
    }

    /**
     * Adds all of the elements of the specified collection to this collection.
     *
     * @return `true` if any of the specified elements was added to the collection, `false` if the collection was not modified.
     */
    override fun addAll(elements: Collection<T>): Boolean {
        throw NotImplementedError("we have no current use for this, so leaving unimplemented")
    }

    override fun remove(element: T) : Boolean {
        return map.remove(element.getIndex()) != null
    }

    override fun iterator(): MutableIterator<T> {
        return Collections.unmodifiableSet(map.values.toSet()).iterator()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MutableConcurrentSet<*>

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
        return map.contains(element)
    }

    override fun containsAll(elements: Collection<T>): Boolean {
        throw NotImplementedError(NOT_CURRENTLY_USED)
    }

    override fun isEmpty(): Boolean {
        return map.isEmpty()
    }

    override fun clear() {
        this.map.clear()
    }

    override fun removeAll(elements: Collection<T>): Boolean {
        throw NotImplementedError(NOT_CURRENTLY_USED)
    }

    override fun retainAll(elements: Collection<T>): Boolean {
        throw NotImplementedError(NOT_CURRENTLY_USED)
    }

    open fun update(element: T) : Boolean {
        val result = map.computeIfPresent(element.getIndex()) { _, _ ->  element }
        return result != null
    }

    companion object {
        private const val NOT_CURRENTLY_USED = "we have no current use for this, so leaving unimplemented"
    }

}

