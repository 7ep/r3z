package coverosR3z.logging

import coverosR3z.authentication.types.CurrentUser
import coverosR3z.config.utility.SystemOptions
import coverosR3z.logging.ILogger.Companion.getTimestamp
import coverosR3z.misc.utility.ActionQueue
import coverosR3z.system.types.SystemConfiguration

class Logger : ILogger {
    private val loggerPrinter = ActionQueue("loggerPrinter")

    /**
     * Set the system to standard configuration for which
     * log entries will print
     */
    override fun resetLogSettingsToDefault() {
        logSettings = SystemConfiguration.LogSettings(
            audit = true,
            warn = true,
            debug = true,
            trace = false)
    }

    override fun turnOnAllLogging() {
        logSettings = SystemConfiguration.LogSettings(
            audit = true,
            warn = true,
            debug = true,
            trace = true)
    }

    override fun turnOffAllLogging() {
        logSettings = SystemConfiguration.LogSettings(
            audit = false,
            warn = false,
            debug = false,
            trace = false)
    }

    override var logSettings = SystemConfiguration.LogSettings(
            audit = true,
            warn = true,
            debug = true,
            trace = false)

    override fun logAudit(cu : CurrentUser, msg : () -> String) {
        if (logSettings.audit) {
            loggerPrinter.enqueue { println("${getTimestamp()} AUDIT: ${cu.name.value}: ${msg()}") }
        }
    }

    /**
     * Used to log finicky details of technical solutions, for basic debugging
     */
    override fun logDebug(cu : CurrentUser, msg: () -> String) {
        if (logSettings.debug) {
            loggerPrinter.enqueue { println("${getTimestamp()} DEBUG: ${cu.name.value}: ${msg()}") }
        }
    }

    /**
     * Logs nearly extraneous levels of detail, needed for deep debugging
     */
    override fun logTrace(cu : CurrentUser, msg: () -> String) {
        if (logSettings.trace) {
            loggerPrinter.enqueue { println("${getTimestamp()} TRACE: ${cu.name.value}: ${msg()}") }
        }
    }

    /**
     * Logs items that could be concerning to the operations team.  Like
     * a missing database file.
     */
    override fun logWarn(cu : CurrentUser, msg: () -> String) {
        if (logSettings.warn) {
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
