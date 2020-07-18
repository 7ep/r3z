package coverosR3z.domainobjects

import java.lang.Integer.parseInt

private const val maxEmployeeCount = 100_000_000
private const val maxEmployeeMsg = "No way this company has more than 100 million employees"

/**
 * Holds a user's name before we have a whole object, like [User]
 */
data class UserName(val value: String) {
    init {
        assert(value.isNotEmpty()) {"All users must have a non-empty name"}
    }
}

data class User(val id: Int, val name: String) {

    init {
        assert(name.isNotEmpty()) {"All users must have a non-empty name"}
        assert(id < maxEmployeeCount) { maxEmployeeMsg }
    }

    fun serialize(): String {
        return "{id=$id,name=$name}"
    }

    companion object {
        private val deserializationRegex = "\\{id=(.*),name=(.*)}".toRegex()

        fun deserialize(value : String) : User? {
            val matches = deserializationRegex.matchEntire(value)
            if (matches != null) {
                val (idString, name) = matches.destructured
                val id = parseInt(idString)
                return User(id, name)
            } else {
                return null
            }
        }
    }

}

data class UserId(val id: Int) {
    init {
        assert(id < maxEmployeeCount) {maxEmployeeMsg }
    }
}



