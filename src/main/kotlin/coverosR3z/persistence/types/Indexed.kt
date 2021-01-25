package coverosR3z.persistence.types

/**
 * This interface is for those data types that have an index.
 * See [getIndex]
 */
interface Indexed {

    /**
     * Gets the current index of this object.  A common pattern in the
     * system is to use the [ConcurrentSet.nextIndex], which is an [java.util.concurrent.atomic.AtomicInteger].
     * If you are creating a new index for an item, use [java.util.concurrent.atomic.AtomicInteger.getAndIncrement]
     * to get the current value and increment it in one motion, so we can avoid worrying
     * about thread safety when creating new unique id's per element
     */
    fun getIndex() : Int
}