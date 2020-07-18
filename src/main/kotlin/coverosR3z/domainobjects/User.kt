package coverosR3z.domainobjects

/**
 * Holds a user's name before we have a whole object, like [User]
 */
data class UserName(val value: String) {
    init {
        assert(value.isNotEmpty()) {"All users must have a non-empty name"}
    }
}

data class User(val id: Int, val name: String) {

    companion object {
        private val deserializationRegex = "\\{id=(.*),name=(.*)}".toRegex()

        fun deserialize(value : String) : User? {
            val matches = deserializationRegex.matchEntire(value)
            if (matches != null) {
                val (idString, name) = matches.destructured
                val id = Integer.parseInt(idString)
                return User(id, name)
            } else {
                return null
            }
        }
    }

    fun serialize(): String {
        return "{id=$id,name=$name}"
    }

    init {
        assert(name.isNotEmpty()) {"All users must have a non-empty name"}
        assert(id < 100_000_000) { "There is no way on earth this company has ever or will " +
                "ever have more than even a million employees" }
    }
}

data class UserId(val id: Int) {
    init {
        assert(id < 100_000_000) { "There is no way on earth this company has ever or will " +
                "ever have more than even a million employees" }
    }
}



