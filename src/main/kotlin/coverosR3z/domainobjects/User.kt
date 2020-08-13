package coverosR3z.domainobjects

import kotlinx.serialization.Serializable

private const val maxUserCount = 100_000_000
private const val maxUserMsg = "No way this company has more than 100 million users"
private const val minIdMsg = "Valid identifier values are 1 or above"
private const val nameCannotBeEmptyMsg = "All users must have a non-empty name"
private const val hashCannotBeEmptyMsg = "All users must have a hash"

/**
 * Holds a username before we have a whole object, like [User]
 */
@Serializable
data class UserName(val value: String){
    init {
        assert(value.isNotBlank()) {nameCannotBeEmptyMsg}
    }
}

@Serializable
data class User(val id: Int, val name: String, val hash: String) {

    init {
        assert(name.isNotBlank()) {nameCannotBeEmptyMsg}
        assert(id < maxUserCount) { maxUserMsg }
        assert(id > 0) { minIdMsg }
    }
}

data class Hash(val value: String) {
    init {
        assert(value.isNotBlank()) {hashCannotBeEmptyMsg}
    }
}