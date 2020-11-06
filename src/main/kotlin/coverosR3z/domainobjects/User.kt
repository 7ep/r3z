package coverosR3z.domainobjects

import coverosR3z.authentication.CurrentUser
import kotlinx.serialization.Serializable
import java.security.MessageDigest
import kotlin.random.Random

private const val maxUserCount = 100_000_000
private const val maxUserMsg = "No way this company has more than 100 million users"
private const val minIdMsg = "Valid identifier values are 1 or above"
private const val nameCannotBeEmptyMsg = "All users must have a non-empty name"
private val md = MessageDigest.getInstance("SHA-256")

/**
 * This is used to represent no user - just to avoid using null for a user
 */
val NO_USER = User(maxUserCount-1, "NO_USER", Hash.createHash(""), "THIS REPRESENTS NO USER", -1)

/**
 * This is the user who does things if no one is logged in actively doing it.
 * Like for example, when someone is authenticating, it is the system user
 * that is taking care of them.  Where on the other hand, if someone is recording
 * time, we would want to see that user indicated as the executor.
 */
val SYSTEM_USER = User(maxUserCount-2, "SYSTEM", Hash.createHash(""), "THIS REPRESENTS ACTIONS BY THE SYSTEM", null)

/**
 * Holds a username before we have a whole object, like [User]
 */
@Serializable
data class UserName(val value: String){
    init {
        require(value.isNotBlank()) {nameCannotBeEmptyMsg}
    }
}

@Serializable
data class User(val id: Int, val name: String, val hash: Hash, val salt: String, val employeeId: Int?) {

    init {
        require(name.isNotBlank()) {nameCannotBeEmptyMsg}
        require(id < maxUserCount) { maxUserMsg }
        require(id > 0) { minIdMsg }
    }
}

@Serializable
data class Hash private constructor(val value: String) {


    companion object{
        /**
         * Hash the input string with the provided SHA-256 algorithm, and return a string representation
         */
        fun createHash(password: String): Hash {
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
