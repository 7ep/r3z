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
    init {
        assert(name.isNotEmpty()) {"All users must have a non-empty name"}
        assert(id < 100_000_000) { "There is no way on earth this company has ever or will " +
                "ever have more than even a million employees" }
    }
}


