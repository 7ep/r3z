package coverosR3z.persistence.types

import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * This uses a [ConcurrentHashMap] as its base.  We store
 * the data in the keys only.  We provide some syntactic sugar
 * so this seems similar to using a Set.
 *
 * This set is simpler than [MutableConcurrentSet] - it stores the data in the keys.
 */
open class SimpleConcurrentSet<T : Any> : Iterable<T>{

    private val map : ConcurrentHashMap<T, NullEnum> = ConcurrentHashMap()

    private enum class NullEnum {
        /**
         * This is just a token for the value in the ConcurrentHashMap, since
         * we are only using the keys, never the values.
         */
        NULL
    }

    fun add(element : T) : Boolean {
        map.computeIfAbsent(element) { NullEnum.NULL }
        return true
    }

    fun remove(element: T) : Boolean {
        return map.remove(element) != null
    }

    val size: Int
        get() = map.size

    fun contains(element: T): Boolean {
        return map.keys.contains(element)
    }

    override fun iterator(): MutableIterator<T> {
        return Collections.unmodifiableSet(map.keySet(NullEnum.NULL)).iterator()
    }


}

