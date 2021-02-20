package coverosR3z.persistence.types

/**
 * Represents the kind of data that a particular [Serializable] type
 * would need within an companion object
 */
abstract class SerializableCompanion<T : SerializationKeys>(val values : Array<T>) {

    /**
     * The directory where this data will be stored
     */
    abstract val directoryName: String

    /**
     * Converts a string to a [SerializationKeys]
     */
    fun convertToKey(s: String): SerializationKeys {
        return values.single { it.getKey() == s }
    }

}