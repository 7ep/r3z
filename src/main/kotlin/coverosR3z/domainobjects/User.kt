package coverosR3z.domainobjects

import coverosR3z.domainobjects.Hash.Companion.createHash
import coverosR3z.misc.generateRandomString
import java.security.MessageDigest
import java.security.spec.KeySpec
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec


private const val maxUserCount = 100_000_000
const val maxUserNameSize = 50
const val minUserNameSize = 3
const val tooLargeUsernameMsg = "Username is too large. Max is $maxUserNameSize"
const val tooSmallUsernameMsg = "Username is too small. Min is $minUserNameSize"
private const val maxUserMsg = "No way this company has more than 100 million users"
private const val minIdMsg = "Valid identifier values are 1 or above"
const val usernameCannotBeEmptyMsg = "All users must have a non-empty name"
const val usernameNotNullMsg = "Username must not be null"

/**
 * This is used to represent no user - just to avoid using null for a user
 * It's a typed null, essentially
 */
val NO_USER = User(UserId(maxUserCount - 1), UserName("NO_USER"), createHash(Password("NO_USER_PASSWORD"), Salt("THIS REPRESENTS NO USER")), Salt("THIS REPRESENTS NO USER"), NO_EMPLOYEE.id)

/**
 * This is the user who does things if no one is logged in actively doing it.
 * Like for example, when someone is authenticating, it is the system user
 * that is taking care of them.  Where on the other hand, if someone is recording
 * time, we would want to see that user indicated as the executor.
 */
val SYSTEM_USER = User(UserId(maxUserCount - 2), UserName("SYSTEM"), createHash(Password("SYSTEM_USER_PASSWORD"), Salt("THIS REPRESENTS ACTIONS BY THE SYSTEM")), Salt("THIS REPRESENTS ACTIONS BY THE SYSTEM"), null)

/**
 * Holds a username before we have a whole object, like [User]
 */
data class UserName(val value: String){
    init {
        require(value.isNotBlank()) {usernameCannotBeEmptyMsg}
        require(value.length <= maxUserNameSize) {tooLargeUsernameMsg}
        require(value.length >= minUserNameSize) { tooSmallUsernameMsg}
    }

    companion object {
        fun make(value: String?) : UserName {
            val valueNotNull = checkNotNull(value) {usernameNotNullMsg}
            return UserName(valueNotNull)
        }
    }
}

data class UserId(val value: Int) {
    init {
        require(value < maxUserCount) { maxUserMsg }
        require(value > 0) { minIdMsg }
    }
}

data class Salt(val value: String)

data class User(val id: UserId, val name: UserName, val hash: Hash, val salt: Salt, val employeeId: EmployeeId?)

/**
 * Don't use the constructor to make a hash typically.  Use
 * the [createHash] function
 */
data class Hash constructor(val value: String) {


    companion object{
        /**
         * Hash the input string with the provided PBKDF2 algorithm, and return a string representation
         *
         * See https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html
         */
        fun createHash(password: Password, salt : Salt): Hash {
            val spec: KeySpec = PBEKeySpec(password.value.toCharArray(), salt.value.toByteArray(), 65536, 128)
            val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1")
            val hashed: ByteArray = factory.generateSecret(spec).encoded
            return Hash(hashed.joinToString("") { "%02x".format(it) })
        }

        /**
         * Creates a random string to salt our hashes so they're yummy.
         *
         * By adding a large value to the password before hashing it,
         * we put a huge impediment in place to using dictionary
         * attacks on our user's password hashes.
         *
         * You may pass in a different generator than the default secure
         * random string generator, perhaps for testing.
         *
         * See https://en.wikipedia.org/wiki/Salt_(cryptography)
         */
        fun getSalt(generator: () -> String = { generateRandomString(16) }): Salt {
            return Salt(generator())
        }

    }
}
