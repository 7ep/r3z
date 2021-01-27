package coverosR3z.persistence.types

/**
 * This interface is typically used on the
 * enums in serializable data types, like Project
 * or Employee, to enumerate the possible keys
 * used during the serialization / deserialization process
 */
interface SerializationKeys {

    fun getKey() : String
}
