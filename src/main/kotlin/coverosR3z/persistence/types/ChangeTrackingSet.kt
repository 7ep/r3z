package coverosR3z.persistence.types

class ChangeTrackingSet<T> : ConcurrentSet<T>() {

    private val modified = mutableSetOf<T>()

    /**
     * Gets the current changes to the data, clearing
     * it in the process
     */
    fun getChangedData(): Set<T> {
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
        modified.add(item)
        return super.add(item)
    }

    override fun addAll(elements: Collection<T>) : Boolean {
        modified.addAll(elements)
        return super.addAll(elements)
    }

    override fun remove(item: T) {
        modified.add(item)
        return super.remove(item)
    }


}

fun <T> List<T>.toChangeTrackingSet() : ChangeTrackingSet<T> {
    val newSet = ChangeTrackingSet<T>()
    this.forEach{newSet.add(it)}
    return newSet
}
