package coverosR3z.persistence.types

/**
 * A combination of [Indexed] and [Serializable], so we can use
 * it as a single constraint in generics, especially used in
 * [AbstractDataAccess]
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
    enum class Keys(private val keyString: String) : SerializationKeys {
        EXAMPLE_ONE("eo"),
        EXAMPLE_TWO("et"),

        ;
        /**
         * This needs to be a method and not just a value of the class
         * so that we can have it meet an interface specification, so
         * that we can use it in generic code
         */
        override fun getKey() : String {
            throw NotImplementedError("define the keys for each piece of data we will serialize and persist to disk")
        }
    }
}