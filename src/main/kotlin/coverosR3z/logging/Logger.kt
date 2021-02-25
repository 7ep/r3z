package coverosR3z.logging

import coverosR3z.authentication.types.CurrentUser
import coverosR3z.config.utility.SystemOptions
import coverosR3z.logging.ILogger.Companion.getTimestamp
import coverosR3z.misc.utility.ActionQueue

class Logger : ILogger {
    private val loggerPrinter = ActionQueue("loggerPrinter")

    /**
     * Set the system to standard configuration for which
     * log entries will print
     */
    override fun resetLogSettingsToDefault() {
        logSettings[LogTypes.AUDIT] = true
        logSettings[LogTypes.DEBUG] = true
        logSettings[LogTypes.WARN] = true
        logSettings[LogTypes.TRACE] = false
    }

    override fun turnOnAllLogging() {
        logSettings[LogTypes.AUDIT] = true
        logSettings[LogTypes.DEBUG] = true
        logSettings[LogTypes.WARN] = true
        logSettings[LogTypes.TRACE] = true
    }

    override fun turnOffAllLogging() {
        logSettings[LogTypes.AUDIT] = false
        logSettings[LogTypes.DEBUG] = false
        logSettings[LogTypes.WARN] = false
        logSettings[LogTypes.TRACE] = false
    }

    override val logSettings = mutableMapOf(
            LogTypes.AUDIT to true,
            LogTypes.WARN to true,
            LogTypes.DEBUG to true,
            LogTypes.TRACE to false)

    override fun logAudit(cu : CurrentUser, msg : () -> String) {
        if (logSettings[LogTypes.AUDIT] == true) {
            loggerPrinter.enqueue { println("${getTimestamp()} AUDIT: ${cu.name.value}: ${msg()}") }
        }
    }

    /**
     * Used to log finicky details of technical solutions
     */
    override fun logDebug(cu : CurrentUser, msg: () -> String) {
        if (logSettings[LogTypes.DEBUG] == true) {
            loggerPrinter.enqueue { println("${getTimestamp()} DEBUG: ${cu.name.value}: ${msg()}") }
        }
    }

    /**
     * Logs nearly extraneous levels of detail.
     */
    override fun logTrace(cu : CurrentUser, msg: () -> String) {
        if (logSettings[LogTypes.TRACE] == true) {
            loggerPrinter.enqueue { println("${getTimestamp()} TRACE: ${cu.name.value}: ${msg()}") }
        }
    }

    /**
     * Logs items that could be concerning to the operations team.  Like
     * a missing database file.
     */
    override fun logWarn(cu : CurrentUser, msg: () -> String) {
        if (logSettings[LogTypes.WARN] == true) {
            loggerPrinter.enqueue { println("${getTimestamp()} ${cu.name.value}: WARN: ${msg()}") }
        }
    }

    /**
     * Sets logging per the [SystemOptions], if the user requested
     * something
     */
    override fun configureLogging(serverOptions: SystemOptions) {
        if (serverOptions.allLoggingOff) {
            turnOffAllLogging()
        }
        if (serverOptions.allLoggingOn) {
            turnOnAllLogging()
        }
    }

    override fun stop() {
        loggerPrinter.stop()
    }

}
