package coverosR3z.persistence.types

import java.util.concurrent.ConcurrentLinkedQueue

class R3zConcurrentQueue<T> : ConcurrentLinkedQueue<T>() {

    /**
     * This method is the whole reason behind creating
     * our own queue, it is so we can override the equals
     * to only care about the list, in order, so we
     * can more easily compare equality on the [ConcurrentLinkedQueue]
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as R3zConcurrentQueue<*>

        if (this.toList() != other.toList()) return false

        return true
    }

    override fun hashCode(): Int {
        return javaClass.hashCode()
    }
}