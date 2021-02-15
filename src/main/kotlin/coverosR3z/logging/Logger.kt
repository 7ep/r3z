package coverosR3z.logging

import coverosR3z.authentication.types.CurrentUser
import coverosR3z.authentication.types.SYSTEM_USER
import coverosR3z.config.utility.SystemOptions
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

fun logAudit(cu : CurrentUser = CurrentUser(SYSTEM_USER), msg : () -> String) {
    if (LogConfig.logSettings[LogTypes.AUDIT] == true) {
        loggerPrinter.enqueue { println("${getCurrentMillis()} AUDIT: ${cu.user.name.value}: ${msg()}") }
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

/**
 * Logs nearly extraneous levels of detail.
 */
fun logTrace(cu : CurrentUser = CurrentUser(SYSTEM_USER), msg: () -> String) {
    if (LogConfig.logSettings[LogTypes.TRACE] == true) {
        loggerPrinter.enqueue { println("${getCurrentMillis()} TRACE: ${cu.user.name.value}: ${msg()}") }
    }
}

/**
 * Logs items that could be concerning to the operations team.  Like
 * a missing database file.
 */
fun logWarn(cu : CurrentUser = CurrentUser(SYSTEM_USER), msg: () -> String) {
    if (LogConfig.logSettings[LogTypes.WARN] == true) {
        loggerPrinter.enqueue { println("${getCurrentMillis()} ${cu.user.name.value}: WARN: ${msg()}") }
    }
}

/**
 * Logging that must be shown, which you cannot turn off
 */
fun logImperative(msg: String) {
    println("${getCurrentMillis()} IMPERATIVE: $msg")
}


/**
 * Sets logging per the [SystemOptions], if the user requested
 * something
 */
fun configureLogging(serverOptions: SystemOptions) {
    if (serverOptions.allLoggingOff) {
        turnOffAllLogging()
    }
    if (serverOptions.allLoggingOn) {
        turnOnAllLogging()
    }
}
