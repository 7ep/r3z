package coverosR3z.logging

import coverosR3z.authentication.types.CurrentUser
import coverosR3z.authentication.types.SYSTEM_USER
import coverosR3z.misc.utility.ActionQueue

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

val loggerPrinter = ActionQueue("loggerPrinter")

/**
 * Set the system to standard configuration for which
 * log entries will print
 */
fun resetLogSettingsToDefault() {
    LogConfig.logSettings[LogTypes.AUDIT] = true
    LogConfig.logSettings[LogTypes.DEBUG] = true
    LogConfig.logSettings[LogTypes.WARN] = true
    LogConfig.logSettings[LogTypes.TRACE] = false
}

fun turnOnAllLogging() {
    LogConfig.logSettings[LogTypes.AUDIT] = true
    LogConfig.logSettings[LogTypes.DEBUG] = true
    LogConfig.logSettings[LogTypes.WARN] = true
    LogConfig.logSettings[LogTypes.TRACE] = true
}

fun turnOffAllLogging() {
    LogConfig.logSettings[LogTypes.AUDIT] = false
    LogConfig.logSettings[LogTypes.DEBUG] = false
    LogConfig.logSettings[LogTypes.WARN] = false
    LogConfig.logSettings[LogTypes.TRACE] = false
}

object LogConfig{
    val logSettings = mutableMapOf(
            LogTypes.AUDIT to true,
            LogTypes.WARN to true,
            LogTypes.DEBUG to true,
            LogTypes.TRACE to false)
}

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
        logAudit(cu) { msg }
    }

    fun debug(msg: String) {
        logDebug(cu) {msg}
    }

}

@Deprecated("Use the alternate form instead, has better performance", ReplaceWith("logAudit(cu) {msg}"))
fun logAudit(msg : String, cu : CurrentUser = CurrentUser(SYSTEM_USER)) {
    if (LogConfig.logSettings[LogTypes.AUDIT] == true) {
        loggerPrinter.enqueue { println("${getCurrentMillis()} AUDIT: ${cu.user.name.value}: $msg") }
    }
}

fun logAudit(cu : CurrentUser = CurrentUser(SYSTEM_USER), msg : () -> String) {
    if (LogConfig.logSettings[LogTypes.AUDIT] == true) {
        loggerPrinter.enqueue { println("${getCurrentMillis()} AUDIT: ${cu.user.name.value}: ${msg()}") }
    }
}

@Deprecated("Use the alternate form instead, has better performance", ReplaceWith("logDebug(cu) {msg}"))
fun logDebug(msg : String, cu : CurrentUser = CurrentUser(SYSTEM_USER)) {
    if (LogConfig.logSettings[LogTypes.DEBUG] == true) {
        loggerPrinter.enqueue { println("${getCurrentMillis()} DEBUG: ${cu.user.name.value}: $msg") }
    }
}

/**
 * Used to log finicky details of technical solutions
 */
fun logDebug(cu : CurrentUser = CurrentUser(SYSTEM_USER), msg: () -> String) {
    if (LogConfig.logSettings[LogTypes.DEBUG] == true) {
        loggerPrinter.enqueue { println("${getCurrentMillis()} DEBUG: ${cu.user.name.value}: ${msg()}") }
    }
}

@Deprecated("Use the alternate form instead, has better performance", ReplaceWith("logTrace {msg}"))
fun logTrace(msg: String) {
    if (LogConfig.logSettings[LogTypes.TRACE] == true) {
        loggerPrinter.enqueue { println("${getCurrentMillis()} TRACE: $msg") }
    }
}

/**
 * Logs nearly extraneous levels of detail.
 */
fun logTrace(msg: () -> String) {
    if (LogConfig.logSettings[LogTypes.TRACE] == true) {
        loggerPrinter.enqueue { println("${getCurrentMillis()} TRACE: ${msg()}") }
    }
}

@Deprecated("Use the alternate form instead, has better performance", ReplaceWith("logWarn {msg}"))
fun logWarn(msg: String) {
    if (LogConfig.logSettings[LogTypes.WARN] == true) {
        loggerPrinter.enqueue { println("${getCurrentMillis()} WARN: $msg") }
    }
}

/**
 * Logs items that could be concerning to the operations team.  Like
 * a missing database file.
 */
fun logWarn(msg: () -> String) {
    if (LogConfig.logSettings[LogTypes.WARN] == true) {
        loggerPrinter.enqueue { println("${getCurrentMillis()} WARN: ${msg()}") }
    }
}

/**
 * Logging that must be shown, which you cannot turn off
 */
fun logImperative(msg: String) {
    println("${getCurrentMillis()} IMPERATIVE: $msg")
}
