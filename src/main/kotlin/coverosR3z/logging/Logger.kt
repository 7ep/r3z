package coverosR3z.logging

import coverosR3z.authentication.CurrentUser
import coverosR3z.domainobjects.SYSTEM_USER

enum class LogTypes {
    /**
     * Generally useful information, particularly for recording business actions by users.  That is, reading these
     * logs should read like a description of the user carrying out business chores
     */
    AUDIT,
    WARN,
    DEBUG,
    TRACE
}

fun getCurrentMillis() : Long {
    return System.currentTimeMillis()
}

/**
 * Set the system to standard configuration for which
 * log entries will print
 */
fun resetLogSettingsToDefault() {
    logSettings[LogTypes.AUDIT] = true
    logSettings[LogTypes.DEBUG] = true
    logSettings[LogTypes.WARN] = true
    logSettings[LogTypes.TRACE] = false
}

val logSettings = mutableMapOf(
        LogTypes.AUDIT to true,
        LogTypes.WARN to true,
        LogTypes.DEBUG to true,
        LogTypes.TRACE to false)

/**
 * The class version of logging
 */
class Logger (private val cu : CurrentUser = CurrentUser(SYSTEM_USER)) {
    /**
     * Logs here are intended to record business actions
     * taken by the system, for auditing purposes.  Keep
     * this logging narrowly focused on business, and
     * try not to be repetitive.
     */
    fun audit(msg : String) {
        logAudit(msg, cu)
    }

    fun debug(msg: String) {
        logDebug(msg, cu)
    }
}

/**
 * See [Logger.audit]
 */
fun logAudit(msg : String, cu : CurrentUser = CurrentUser(SYSTEM_USER)) {
    if (logSettings[LogTypes.AUDIT] == true) {
        println("${getCurrentMillis()} AUDIT: ${cu.user.name.value}: $msg")
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
fun logTrace(msg: () -> String) {
    if (logSettings[LogTypes.TRACE] == true) {
        println("${getCurrentMillis()} TRACE: ${msg()}")
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
