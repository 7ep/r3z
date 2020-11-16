package coverosR3z.misc

import java.lang.IllegalStateException
import kotlin.random.Random

/**
 * Generate a secure random string
 *
 * @param size - number of random bytes to generate (*not* the size of the string - the string is double this size)
 */
fun generateRandomString(size : Int): String {
    val randomBytes: ByteArray = Random.nextBytes(size)
    return randomBytes.joinToString("") { "%02x".format(it) }
}

/**
 * Returns the value parsed as an int.  If this fails, returns
 * an [IllegalStateException] with the message
 */
fun checkParseToInt(value: String, msg: () -> String): Int {
    return try {
        value.toInt()
    } catch (ex: java.lang.NumberFormatException) {
        throw IllegalStateException(msg())
    }
}