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

/**
 * Returns the value parsed as an int.  If this fails, returns
 * an [IllegalStateException] with the message
 */
fun checkParseToInt(value: String?,
                    nullMsg: () -> String = {"Must not be a null value"},
                    blankMsg: () -> String = {"Must not be blank"},
                    parseMsg: () -> String = {"Must be able to parse ${value?.replace(" ", "(SPACE)")} as integer"}): Int {
    val notNullValue = requireNotNull(value){ nullMsg }
    require(notNullValue.isNotBlank()) { blankMsg }
    return try {
        notNullValue.toInt()
    } catch (ex: java.lang.NumberFormatException) {
        throw IllegalStateException(parseMsg())
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

/**
 * Returns text that has three symbols replaced -
 * the less-than, greater-than, and ampersand.
 * See https://www.w3.org/International/questions/qa-escapes#use
 *
 * This will protect against something like <div>$USERNAME</div> allowing
 * a username of
 *      <script>alert(1)</script>
 * becoming
 *      <div><script>alert(1)</script</div>
 * and instead becomes
 *      <div>&lt;script&gt;alert(1)&lt;/script&gt;</div>
 *
 * If the text is going inside an attribute (e.g. <div class="TEXT_GOES_HERE"> )
 * Then you need to escape slightly differently. In that case see [safeAttr]
 */
fun safeHtml(input : String) : String {
    return input.replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
}

/**
 * Replace dangerous text that would go inside an HTML attribute.
 * See [safeHtml]
 */
fun safeAttr(input : String) : String {
    return input.replace("\"", "&quot;")
        .replace("'", "&apos;")
}