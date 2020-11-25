package coverosR3z.misc

import coverosR3z.domainobjects.Date
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
fun checkParseToInt(value: String, msg: () -> String = {"Must be able to parse $value as integer"}): Int {
    return try {
        value.toInt()
    } catch (ex: java.lang.NumberFormatException) {
        throw IllegalStateException(msg())
    }
}

/**
 * returns the time spent on the items inside.
 * To use: simply wrap the code with getTime, like this:
 *
 *      val timeTaken = getTime {
 *           foo()
 *           bar()
 *      }
 */
fun <T>getTime(function: () -> T): Pair<Long, T> {
    val start = System.currentTimeMillis()
    val result : T = function()
    val finish = System.currentTimeMillis()
    return Pair(finish - start, result)
}
