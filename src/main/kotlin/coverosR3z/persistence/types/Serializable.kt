package coverosR3z.persistence.types

import coverosR3z.misc.utility.encode

/**
 * Serializable classes are able to [serialize] their content
 */
abstract class Serializable {

    /**
     * this represents the connection between a name of a property
     * and the associated value ot it in this class,
     * used during the serialization process.  For example,
     * if we think about the [coverosR3z.timerecording.types.Project] type, it has an id
     * and a name.  Therefore, an appropriate map might
     * be "name" to encode(ProjectName.value) and "id" to id.
     *
     * Note that encoding values before serialization is key -
     * see the [coverosR3z.misc.utility.encode] method for details.
     */
    abstract val dataMappings: Map<SerializationKeys, String>

    companion object {
        /**
         * This is used as a boundary of what is acceptable for a key string
         * used in our serialization process.  There's no need to complicate
         * things.  Keys should be short and sweet.  No need for symbols or
         * numbers or whitespace - it just would complicate deserialization later.
         */
        val validKeyRegex = """[a-zA-Z]{1,10}""".toRegex()
    }


    /**
     * converts the data in this object to a form easily written to disk.
     * See [dataMappings] to see how we map a name to a value
     */
    fun serialize(): String {
        val allKeys = dataMappings.keys.map { it.keyString }
        check(allKeys.size == allKeys.toSet().size) {"Serialization keys must be unique.  Here are your keys: $allKeys"}
        dataMappings.keys.forEach {
            check(it.keyString.isNotBlank()) {"Serialization keys must match this regex: ${validKeyRegex.pattern}.  Your key was: (BLANK)"}
            check(validKeyRegex.matches(it.keyString)) {"Serialization keys must match this regex: ${validKeyRegex.pattern}.  Your key was: ${it.keyString}"}
        }
        return "{ "+ dataMappings.entries.joinToString (" , ") { "${it.key.keyString}: ${encode(it.value)}" }  +" }"
    }
}