package coverosR3z.persistence.types

/**
 * A combination of [Indexed] and [Serializable], so we can use
 * it as a single constraint in generics, especially used in
 * [AbstractDataAccess]
 */
abstract class IndexableSerializable : Indexed, Serializable()