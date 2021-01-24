package coverosR3z.persistence.types

class ChangeTrackingSet<T> : ConcurrentSet<T>() {

    private val modified = mutableSetOf<T>()

    fun getChangedData(): Set<T> {
        return modified
    }

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
