package coverosR3z.persistence.utility

import coverosR3z.persistence.types.*


/**
 * An object-oriented strongly-typed database
 */
class PureMemoryDatabase(
    private val diskPersistence: DatabaseDiskPersistence? = null,
    private val data: Map<String, ChangeTrackingSet<*>> = mapOf()
) {

    fun stop() {
        diskPersistence?.stop()
    }

    fun copy(): PureMemoryDatabase {
        return PureMemoryDatabase(
            data = this.data.entries.map {it.key to it.value.toList().toChangeTrackingSet()}.toMap()
        )
    }

    /**
     * This method is central to accessing the database.
     */
    @Suppress("UNCHECKED_CAST")
    fun <T: IndexableSerializable> dataAccess(directoryName: String) : DataAccess<T> {
        return DataAccess(checkNotNull(data[directoryName]), diskPersistence, directoryName) as DataAccess<T>
    }

    /**
     * returns true if all the sets of data are empty
     */
    fun isEmpty(): Boolean {
        return data.entries.all { it.value.isEmpty() }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PureMemoryDatabase

        if (data != other.data) return false

        return true
    }

    override fun hashCode(): Int {
        return data.hashCode()
    }

}