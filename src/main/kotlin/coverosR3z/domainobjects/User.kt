package coverosR3z.domainobjects

import kotlinx.serialization.Serializable
import java.security.MessageDigest
import java.security.SecureRandom
import kotlin.random.Random

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
data class User(val id: Int, val name: String, val hash: Hash, val salt: String) {

    init {
        assert(name.isNotBlank()) {nameCannotBeEmptyMsg}
        assert(id < maxUserCount) { maxUserMsg }
        assert(id > 0) { minIdMsg }
    }
}

@Serializable
data class Hash private constructor(val value: String) {

    companion object{
        /**
         * Hash the input string with the provided SHA-256 algorithm, and return a string representation
         */
        fun createHash(password: String): Hash {
            val md = MessageDigest.getInstance("SHA-256")
            md.update(password.toByteArray())
            val hashed : ByteArray = md.digest()
            return Hash(hashed.joinToString("") {"%02x".format(it)})
        }

        /**
         * Creates a random string to salt our hashes so they're yummy.
         * See https://en.wikipedia.org/wiki/Salt_(cryptography)
         */
        fun getSalt(): String {
            val randomBytes : ByteArray = Random.nextBytes(16)
            return randomBytes.joinToString("") {"%02x".format(it)}
        }

    }
}
