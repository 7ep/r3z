package coverosR3z.logging

import coverosR3z.authentication.CurrentUser
import coverosR3z.domainobjects.SYSTEM_USER

/**
 * The class version of logging
 */
class Logger (private val cu : CurrentUser = CurrentUser(SYSTEM_USER)) {
    fun info(msg : String) {
        println("INFO: ${cu.user.name.value}: $msg")
    }
}

fun logInfo(msg : String, cu : CurrentUser = CurrentUser(SYSTEM_USER)) {
    println("INFO: ${cu.user.name.value}: $msg")
}

/**
 * Used to log finicky details of technical solutions
 */
fun logDebug(msg : String) {
    println("DEBUG: $msg")
}

/**
 * Logs nearly extraneous levels of detail.
 */
fun logTrace(msg: String) {
//    println("DEBUG: $msg")
}

/**
 * Logs nearly extraneous levels of detail.
 */
fun logWarn(msg: String) {
    println("WARN: $msg")
}
