package coverosR3z.persistence.types

interface Deserializable<T> {

    fun deserialize(str: String) : T

}