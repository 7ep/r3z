package coverosR3z.persistence.types

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
    abstract val dataMappings: Map<String,String>

    /**
     * converts the data in this object to a form easily written to disk.
     * See [dataMappings] to see how we map a name to a value
     */
    fun serialize(): String {
        return "{ "+ dataMappings.entries.joinToString (" , ") { "${it.key}: ${it.value}" }  +" }"
    }
}