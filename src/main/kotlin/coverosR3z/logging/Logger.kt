package coverosR3z.logging

import coverosR3z.authentication.CurrentUser
import coverosR3z.domainobjects.SYSTEM_USER

enum class LogTypes {
    /**
     * Generally useful information, particularly for recording business actions by users.  That is, reading these
     * logs should read like a description of the user carrying out business chores
     */
    INFO,
    WARN,
    DEBUG,
    TRACE
}

val systemStartMillis = System.currentTimeMillis()

fun getCurrentMillis() : Long {
    return System.currentTimeMillis() - systemStartMillis
}

/**
 * Set the system to standard configuration for which
 * log entries will print
 */
fun resetLogSettingsToDefault() {
    logSettings[LogTypes.INFO] = true
    logSettings[LogTypes.DEBUG] = false
    logSettings[LogTypes.WARN] = true
    logSettings[LogTypes.TRACE] = false
}

val logSettings = mutableMapOf(LogTypes.INFO to true, LogTypes.WARN to true, LogTypes.DEBUG to false, LogTypes.TRACE to false)

/**
 * The class version of logging
 */
class Logger (private val cu : CurrentUser = CurrentUser(SYSTEM_USER)) {
    fun info(msg : String) {
        logInfo(msg, cu)
    }

    fun debug(msg: String) {
        logDebug(msg, cu)
    }
}

fun logInfo(msg : String, cu : CurrentUser = CurrentUser(SYSTEM_USER)) {
    if (logSettings[LogTypes.INFO] == true) {
        println("${getCurrentMillis()} INFO: ${cu.user.name.value}: $msg")
    }
}

/**
 * Used to log finicky details of technical solutions
 */
fun logDebug(msg : String, cu : CurrentUser = CurrentUser(SYSTEM_USER)) {
    if (logSettings[LogTypes.DEBUG] == true) {
        println("${getCurrentMillis()} DEBUG: ${cu.user.name.value}: $msg")
    }
}

/**
 * Logs nearly extraneous levels of detail.
 */
fun logTrace(msg: String) {
    if (logSettings[LogTypes.TRACE] == true) {
        println("${getCurrentMillis()} TRACE: $msg")
    }
}

/**
 * Logs nearly extraneous levels of detail.
 */
fun logWarn(msg: String) {
    if (logSettings[LogTypes.WARN] == true) {
        println("${getCurrentMillis()} WARN: $msg")
    }
}

/**
 * Logging for actions that always take place at server startup
 */
fun logStart(msg: String) {
    println("${getCurrentMillis()} START: $msg")
}
