package coverosR3z.persistence.types

/**
 * Represents the kind of data that a particular [Serializable] type
 * would need within an companion object
 */
interface SerializableCompanion {

    /**
     * The directory where this data will be stored
     */
    val directoryName: String

    /**
     * Converts a string to a [SerializationKeys]
     */
    fun convertToKey(s: String): SerializationKeys

}