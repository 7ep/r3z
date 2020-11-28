package coverosR3z.logging

import coverosR3z.authentication.CurrentUser
import coverosR3z.domainobjects.SYSTEM_USER

enum class LogTypes {
    INFO,
    WARN,
    DEBUG,
    TRACE
}

val logSettings = mutableMapOf(LogTypes.INFO to true, LogTypes.WARN to true, LogTypes.DEBUG to false, LogTypes.TRACE to false)

fun resetLogSettingsToDefault() {
    logSettings[LogTypes.INFO] = true
    logSettings[LogTypes.DEBUG] = true
    logSettings[LogTypes.WARN] = false
    logSettings[LogTypes.TRACE] = false
}

/**
 * The class version of logging
 */
class Logger (private val cu : CurrentUser = CurrentUser(SYSTEM_USER)) {
    fun info(msg : String) {
        logInfo(msg, cu)
    }
}

fun logInfo(msg : String, cu : CurrentUser = CurrentUser(SYSTEM_USER)) {
    if (logSettings[LogTypes.INFO] == true) {
        println("INFO: ${cu.user.name.value}: $msg")
    }
}

/**
 * Used to log finicky details of technical solutions
 */
fun logDebug(msg : String) {
    if (logSettings[LogTypes.INFO] == true) {
        println("DEBUG: $msg")
    }
}

/**
 * Logs nearly extraneous levels of detail.
 */
fun logTrace(msg: String) {
    if (logSettings[LogTypes.INFO] == true) {
        println("DEBUG: $msg")
    }
}

/**
 * Logs nearly extraneous levels of detail.
 */
fun logWarn(msg: String) {
    if (logSettings[LogTypes.INFO] == true) {
        println("WARN: $msg")
    }
}
