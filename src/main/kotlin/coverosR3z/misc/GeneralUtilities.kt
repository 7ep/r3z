package coverosR3z.misc

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