package coverosR3z.persistence.types

/**
 * A combination of [Indexed] and [Serializable], so we can use
 * it as a single constraint in generics
 */
abstract class IndexableSerializable : Indexed, Serializable() {

    companion object : SerializableCompanion<Keys>(Keys.values()) {

        /**
         * If this doesn't get overridden, we'll yell at the developer
         */
        override val directoryName: String
            get() = throw NotImplementedError("This will set the directory where the data gets stored")

    }

    /**
     * This is an example only.  If you create a new type to be
     * stored in the database, you will require a set of keys
     * for the values you will store. 
     */
    enum class Keys(override val keyString: String) : SerializationKeys {
        EXAMPLE_ONE("eo"),
        EXAMPLE_TWO("et"),
    }
}