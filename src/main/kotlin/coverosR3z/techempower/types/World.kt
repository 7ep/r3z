package coverosR3z.techempower.types

import coverosR3z.persistence.types.IndexableSerializable
import coverosR3z.persistence.types.SerializationKeys

data class World(val id: Int, val randomNumber: Int) : IndexableSerializable() {

    override fun getIndex(): Int {
        return id
    }

    override val dataMappings: Map<SerializationKeys, String>
        get() = mapOf(
            Keys.ID to "$id",
            Keys.RANDOM_NUMBER to "$randomNumber"
        )

    enum class Keys(override val keyString: String) : SerializationKeys {
        ID("id"),
        RANDOM_NUMBER("rn");
    }
}


