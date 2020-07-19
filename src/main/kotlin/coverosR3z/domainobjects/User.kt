package coverosR3z.domainobjects

import coverosR3z.exceptions.MalformedDataDuringSerializationException
import kotlinx.serialization.Serializable
import java.lang.Integer.parseInt

private const val maxEmployeeCount = 100_000_000
private const val maxEmployeeMsg = "No way this company has more than 100 million employees"
private const val nameCannotBeEmptyMsg = "All users must have a non-empty name"

/**
 * Holds a user's name before we have a whole object, like [User]
 */
@Serializable
data class UserName(val value: String) {
    init {
        assert(value.isNotEmpty()) {nameCannotBeEmptyMsg}
    }
}

@Serializable
data class User(val id: Int, val name: String) {

    init {
        assert(name.isNotEmpty()) {nameCannotBeEmptyMsg}
        assert(id < maxEmployeeCount) { maxEmployeeMsg }
    }

    fun serialize(): String {
        return "{id=$id,name=$name}"
    }

    companion object {
        private val deserializationRegex = "\\{id=(.*),name=(.*)}".toRegex()

        fun deserialize(value : String) : User? {
            try {
                val matches = deserializationRegex.matchEntire(value) ?: throw Exception()
                val (idString, name) = matches.destructured
                val id = parseInt(idString)
                return User(id, name)
            } catch (ex : Exception) {
                throw MalformedDataDuringSerializationException("was unable to deserialize this: ( $value )")
            }

        }
    }

}

@Serializable
data class UserId(val id: Int) {
    init {
        assert(id < maxEmployeeCount) {maxEmployeeMsg }
    }
}



