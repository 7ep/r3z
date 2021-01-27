package coverosR3z.persistence.types

/**
 * Used for making deserialization generic across
 * types.  See [deserialize]
 */
interface Deserializable<T> {

    /**
     * Takes a string form of a type and
     * converts it to its type.
     * See [coverosR3z.persistence.utility.DatabaseDiskPersistence.deserialize]
     */
    fun deserialize(str: String) : T

}