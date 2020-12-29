package coverosR3z.persistence

import java.util.concurrent.ConcurrentHashMap

/**
 * This uses a [ConcurrentHashMap] as its base.  We store
 * the data in the keys only.  We provide some syntactic sugar
 * so this seems similar to using a Set.
 */
class ConcurrentSet<T> : Iterable<T>{

    private val map : ConcurrentHashMap<T, NullEnum> = ConcurrentHashMap()
    val size = map.size

    private enum class NullEnum {
        /**
         * This is just a token for the value in the ConcurrentHashMap, since
         * we are only using the keys, never the values.
         */
        NULL
    }

    fun add(t : T) {
        map[t] = NullEnum.NULL
    }

    override fun iterator(): Iterator<T> {
        return map.keySet(NullEnum.NULL).iterator()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ConcurrentSet<*>

        if (map != other.map) return false
        if (size != other.size) return false

        return true
    }

    override fun hashCode(): Int {
        var result = map.hashCode()
        result = 31 * result + size
        return result
    }

}

fun <T> List<T>.toConcurrentSet() : ConcurrentSet<T> {
    val newSet = ConcurrentSet<T>()
    this.forEach{newSet.add(it)}
    return newSet
}